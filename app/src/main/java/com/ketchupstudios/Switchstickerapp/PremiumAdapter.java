package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

// IMPORTS FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class PremiumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<PremiumItem> items;
    private final Context context;
    private int lastPosition = -1;
    private boolean isAnimacionEnCurso = false;

    // Variables para favoritos
    private SharedPreferences appPrefs;
    private Set<String> favWallIds;

    // Variables de sonido SoundPool
    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;

    public PremiumAdapter(List<PremiumItem> items, Context context) {
        this.items = items;
        this.context = context;

        // Cargar favoritos al inicio
        appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        favWallIds = appPrefs.getStringSet("fav_wallpapers_ids", new HashSet<>());

        // INICIALIZAR SOUNDPOOL
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        soundId = soundPool.load(context, R.raw.open, 1);
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) soundLoaded = true;
        });
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == PremiumItem.TYPE_STICKER) {
            if (context instanceof MainActivity) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_pack, parent, false);
                return new StickerViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_grid, parent, false);
                return new StickerViewHolder(v);
            }
        } else if (viewType == PremiumItem.TYPE_WALLPAPER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_premium, parent, false);
            return new WallpaperViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_pack, parent, false);
            return new EmptyViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PremiumItem item = items.get(position);
        int type = getItemViewType(position);

        if (type == PremiumItem.TYPE_STICKER) {
            // --- LÓGICA STICKERS ---
            StickerViewHolder sh = (StickerViewHolder) holder;
            StickerPack pack = item.stickerPack;
            sh.itemView.setVisibility(View.VISIBLE);

            ViewGroup.LayoutParams layoutParams = sh.itemView.getLayoutParams();
            if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(false);
            }

            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            Glide.with(context).load(baseUrl + pack.identifier + "/" + pack.trayImageFile)
                    .transform(new CenterCrop(), new RoundedCorners(16)).into(sh.img);

            if (sh.badgePremium != null) sh.badgePremium.setVisibility(View.VISIBLE);
            if (sh.badgeUpdated != null) sh.badgeUpdated.setVisibility("updated".equalsIgnoreCase(pack.status) ? View.VISIBLE : View.GONE);
            if (sh.badgeNew != null) sh.badgeNew.setVisibility("new".equalsIgnoreCase(pack.status) ? View.VISIBLE : View.GONE);

            boolean isAdded = context.getSharedPreferences("StickerMonitor", Context.MODE_PRIVATE).getBoolean(pack.identifier, false);
            if (sh.iconAdded != null) sh.iconAdded.setVisibility(isAdded ? View.VISIBLE : View.GONE);

            // Regalo Sticker (Igual que antes)
            if (sh.imgGiftOverlay != null) {
                boolean isNew = "new".equalsIgnoreCase(pack.status);
                SharedPreferences prefs = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
                boolean isOpened = prefs.getBoolean("opened_" + pack.identifier, false);

                if (isNew && !isOpened) {
                    sh.imgGiftOverlay.setVisibility(View.VISIBLE);
                    sh.imgGiftOverlay.setImageResource(R.drawable.regalo1);
                    sh.txtName.setVisibility(View.INVISIBLE);
                    sh.txtName.setText(pack.name);
                    sh.imgGiftOverlay.setOnClickListener(v -> {
                        if (isAnimacionEnCurso) return;
                        isAnimacionEnCurso = true;
                        reproducirSonidoPop();
                        sh.imgGiftOverlay.setImageResource(R.drawable.regalo2);
                        prefs.edit().putBoolean("opened_" + pack.identifier, true).apply();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            sh.txtName.setVisibility(View.VISIBLE);
                            sh.imgGiftOverlay.animate().translationY(2000f).alpha(40f).setDuration(600).withEndAction(() -> {
                                sh.imgGiftOverlay.setVisibility(View.GONE);
                                isAnimacionEnCurso = false;
                            }).start();
                        }, 1000);
                    });
                } else {
                    sh.imgGiftOverlay.setVisibility(View.GONE);
                    sh.txtName.setVisibility(View.VISIBLE);
                    sh.txtName.setText(pack.name);
                }
            } else {
                sh.txtName.setVisibility(View.VISIBLE);
                sh.txtName.setText(pack.name);
            }

            sh.itemView.setOnClickListener(v -> {
                if (sh.imgGiftOverlay != null && sh.imgGiftOverlay.getVisibility() == View.VISIBLE) return;

                // --- RASTREO ANALYTICS (NUEVO) ---
                FirebaseAnalytics fa = FirebaseAnalytics.getInstance(context);
                Bundle b = new Bundle();
                b.putString(FirebaseAnalytics.Param.ITEM_ID, pack.identifier);
                b.putString("premium_click", "true");
                fa.logEvent("premium_sticker_click", b);
                // ---------------------------------

                if (context instanceof MainActivity) ((MainActivity) context).intentarAbrirPackPremium(pack);
                else if (context instanceof FullListActivity) ((FullListActivity) context).intentarAbrirPackPremium(pack);
            });

        } else if (type == PremiumItem.TYPE_WALLPAPER) {
            // --- LÓGICA WALLPAPERS ---
            WallpaperViewHolder wh = (WallpaperViewHolder) holder;
            Config.Wallpaper wall = item.wallpaper;
            wh.itemView.setVisibility(View.VISIBLE);

            if (wh.btnFavorite != null) wh.btnFavorite.setVisibility(View.VISIBLE);

            ViewGroup.LayoutParams layoutParams = wh.itemView.getLayoutParams();
            int width140 = (int) (140 * context.getResources().getDisplayMetrics().density);

            if (context instanceof MainActivity) {
                layoutParams.width = width140;
                if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                    ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
                }
            } else {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                    ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(false);
                }
            }
            wh.itemView.setLayoutParams(layoutParams);

            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);

            try {
                int colorFondo = Color.parseColor(wall.colorBg != null && !wall.colorBg.isEmpty() ? wall.colorBg : "#E0E0E0");
                wh.img.setBackgroundColor(colorFondo);
            } catch (Exception e) { wh.img.setBackgroundColor(Color.LTGRAY); }

            Glide.with(context).load(baseUrl + "wallpappers/" + wall.imageFile)
                    .transform(new CenterCrop(), new RoundedCorners(16)).into(wh.img);

            if (wh.badgePremium != null) wh.badgePremium.setVisibility(View.VISIBLE);
            if (wh.badgeNew != null) wh.badgeNew.setVisibility(wall.isNew ? View.VISIBLE : View.GONE);

            // --- FAVORITOS (LOCAL Y NUBE) ---
            if (wh.btnFavorite != null) {
                if (favWallIds.contains(wall.imageFile)) {
                    wh.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                } else {
                    wh.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                }

                wh.btnFavorite.setOnClickListener(v -> {
                    Set<String> currentFavs = new HashSet<>(appPrefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));
                    boolean isAdding = false;

                    if (currentFavs.contains(wall.imageFile)) {
                        currentFavs.remove(wall.imageFile);
                        wh.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                        isAdding = false;
                    } else {
                        currentFavs.add(wall.imageFile);
                        wh.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                        isAdding = true;

                        // --- RASTREO ANALYTICS SOLO AL DAR LIKE ---
                        FirebaseAnalytics fa = FirebaseAnalytics.getInstance(context);
                        Bundle b = new Bundle();
                        b.putString(FirebaseAnalytics.Param.ITEM_ID, wall.imageFile);
                        b.putString("premium_like", "true");
                        fa.logEvent("premium_wallpaper_like", b);
                        // ------------------------------------------
                    }
                    appPrefs.edit().putStringSet("fav_wallpapers_ids", currentFavs).apply();
                    favWallIds = currentFavs;

                    // GUARDAR EN NUBE
                    actualizarNube(wall.imageFile, isAdding);
                });
            }

            // Regalo (Igual que antes)
            if (wh.imgGiftOverlay != null) {
                boolean isNew = wall.isNew;
                SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
                boolean isOpened = prefsGift.getBoolean("opened_" + wall.imageFile, false);

                if (isNew && !isOpened) {
                    wh.imgGiftOverlay.setVisibility(View.VISIBLE);
                    wh.imgGiftOverlay.setImageResource(R.drawable.regalow1);
                    wh.imgGiftOverlay.setOnClickListener(v -> {
                        if (isAnimacionEnCurso) return;
                        isAnimacionEnCurso = true;
                        reproducirSonidoPop();
                        wh.imgGiftOverlay.setImageResource(R.drawable.regalow2);
                        prefsGift.edit().putBoolean("opened_" + wall.imageFile, true).apply();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            wh.imgGiftOverlay.animate().translationY(2000f).alpha(40f).setDuration(600).withEndAction(() -> {
                                wh.imgGiftOverlay.setVisibility(View.GONE);
                                isAnimacionEnCurso = false;
                            }).start();
                        }, 1000);
                    });
                } else {
                    wh.imgGiftOverlay.setVisibility(View.GONE);
                }
            }

            wh.itemView.setOnClickListener(v -> {
                if (wh.imgGiftOverlay != null && wh.imgGiftOverlay.getVisibility() == View.VISIBLE) return;
                if (context instanceof MainActivity) ((MainActivity) context).analizarClickWallpaper(wall);
                else if (context instanceof FullListActivity) ((FullListActivity) context).intentarAbrirWallpaperPremium(wall);
            });

        } else {
            holder.itemView.setVisibility(View.INVISIBLE);
        }

        if (type != PremiumItem.TYPE_EMPTY) {
            setAnimation(holder.itemView, position);
        }
    }

    // --- NUEVO MÉTODO AUXILIAR PARA FIREBASE ---
    private void actualizarNube(String itemId, boolean isAdding) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (isAdding) {
                db.collection("users").document(user.getUid())
                        .update("fav_wallpapers", FieldValue.arrayUnion(itemId));
            } else {
                db.collection("users").document(user.getUid())
                        .update("fav_wallpapers", FieldValue.arrayRemove(itemId));
            }
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 100.0f, Animation.ABSOLUTE, 0.0f);
            translate.setDuration(400);
            AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
            alpha.setDuration(400);
            animSet.addAnimation(translate);
            animSet.addAnimation(alpha);
            long delay = (position % 8) * 80;
            animSet.setStartOffset(delay);
            animSet.setInterpolator(new DecelerateInterpolator());
            viewToAnimate.startAnimation(animSet);
            lastPosition = position;
        }
    }

    private void reproducirSonidoPop() {
        if (soundPool != null && soundLoaded) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    static class StickerViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName;
        View badgePremium, badgeUpdated, badgeNew;
        ImageView iconAdded, imgGiftOverlay;
        StickerViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.img_pack_preview);
            txtName = v.findViewById(R.id.txt_pack_name);
            badgePremium = v.findViewById(R.id.badgePremium);
            badgeUpdated = v.findViewById(R.id.badgeUpdated);
            badgeNew = v.findViewById(R.id.badgeNew);
            iconAdded = v.findViewById(R.id.iconAdded);
            imgGiftOverlay = v.findViewById(R.id.imgGiftOverlay);
        }
    }

    static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        View badgePremium, badgeNew;
        ImageView btnFavorite;
        ImageView imgGiftOverlay;
        WallpaperViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.imgWallpaperPreview);
            badgePremium = v.findViewById(R.id.badgePremiumWall);
            badgeNew = v.findViewById(R.id.badgeNew);
            btnFavorite = v.findViewById(R.id.btnFavoriteWall);
            imgGiftOverlay = v.findViewById(R.id.imgGiftOverlay);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        EmptyViewHolder(View v) { super(v); }
    }

    public void updateData() {
        SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        this.favWallIds = appPrefs.getStringSet("fav_wallpapers_ids", new HashSet<>());
        notifyDataSetChanged();
    }
}