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
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Object> items;
    private Context context;
    private OnItemClickListener listener;

    private static final int TYPE_STICKER = 1;
    private static final int TYPE_WALLPAPER = 2;
    private boolean isAnimacionEnCurso = false;

    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;
    private SharedPreferences prefs;
    private Set<String> favorites;

    public interface OnItemClickListener { void onItemClick(Object item); }

    public SearchAdapter(Context context, List<Object> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        favorites = prefs.getStringSet("fav_wallpapers_ids", new HashSet<>());

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build();
        soundId = soundPool.load(context, R.raw.open, 1);
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> { if (status == 0) soundLoaded = true; });
    }

    public void updateList(List<Object> newItems) {
        this.items = newItems;
        favorites = prefs.getStringSet("fav_wallpapers_ids", new HashSet<>());
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof StickerPack) ? TYPE_STICKER : TYPE_WALLPAPER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_STICKER) {
            View view = inflater.inflate(R.layout.item_sticker_grid, parent, false);
            return new StickerViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_wallpaper, parent, false);
            return new WallpaperViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);

        if (getItemViewType(position) == TYPE_STICKER) {
            StickerViewHolder sHolder = (StickerViewHolder) holder;
            StickerPack pack = (StickerPack) item;
            String imgUrl = baseUrl + pack.identifier + "/" + pack.trayImageFile;
            Glide.with(context).load(imgUrl).transform(new CenterCrop()).into(sHolder.imgIcon);

            try {
                // CORRECCIÓN: OCULTAR CANDADO PREMIUM SIEMPRE
                View badgePrem = sHolder.itemView.findViewById(R.id.badgePremium);
                if (badgePrem != null) badgePrem.setVisibility(View.GONE);

                View badgeNew = sHolder.itemView.findViewById(R.id.badgeNew);
                View badgeUpd = sHolder.itemView.findViewById(R.id.badgeUpdated);
                if (badgeNew != null) badgeNew.setVisibility("new".equalsIgnoreCase(pack.status) ? View.VISIBLE : View.GONE);
                if (badgeUpd != null) badgeUpd.setVisibility("updated".equalsIgnoreCase(pack.status) ? View.VISIBLE : View.GONE);
            } catch (Exception e) {}

            // CORRECCIÓN: OCULTAR EFECTO HOLO (SI EXISTE EN XML)
            View holo = sHolder.itemView.findViewById(R.id.holoEffectView);
            if (holo != null) holo.setVisibility(View.GONE);

            // ... (Resto de lógica de Sticker añadida/regalo igual) ...
            boolean isAdded = context.getSharedPreferences("StickerMonitor", Context.MODE_PRIVATE).getBoolean(pack.identifier, false);
            if (sHolder.iconAdded != null) sHolder.iconAdded.setVisibility(isAdded ? View.VISIBLE : View.GONE);

            if (sHolder.imgGiftOverlay != null) {
                boolean isNew = "new".equalsIgnoreCase(pack.status);
                SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
                boolean isOpened = prefsGift.getBoolean("opened_" + pack.identifier, false);
                if (isNew && !isOpened) {
                    sHolder.imgGiftOverlay.setVisibility(View.VISIBLE);
                    sHolder.imgGiftOverlay.setAlpha(1.0f);
                    sHolder.imgGiftOverlay.setImageResource(R.drawable.regalo1);
                    if (sHolder.txtName != null) { sHolder.txtName.setVisibility(View.INVISIBLE); sHolder.txtName.setText(pack.name); }
                    sHolder.imgGiftOverlay.setOnClickListener(v -> {
                        if (isAnimacionEnCurso) return;
                        isAnimacionEnCurso = true;
                        reproducirSonidoPop();
                        sHolder.imgGiftOverlay.setImageResource(R.drawable.regalo2);
                        prefsGift.edit().putBoolean("opened_" + pack.identifier, true).apply();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (sHolder.txtName != null) sHolder.txtName.setVisibility(View.VISIBLE);
                            sHolder.imgGiftOverlay.animate().translationY(2000f).alpha(40f).setDuration(600).withEndAction(() -> {
                                sHolder.imgGiftOverlay.setVisibility(View.GONE);
                                isAnimacionEnCurso = false;
                            }).start();
                        }, 1000);
                    });
                } else {
                    sHolder.imgGiftOverlay.setVisibility(View.GONE);
                    if (sHolder.txtName != null) { sHolder.txtName.setVisibility(View.VISIBLE); sHolder.txtName.setText(pack.name); }
                }
            } else {
                if (sHolder.txtName != null) { sHolder.txtName.setVisibility(View.VISIBLE); sHolder.txtName.setText(pack.name); }
            }

            sHolder.itemView.setOnClickListener(v -> {
                if (sHolder.imgGiftOverlay != null && sHolder.imgGiftOverlay.getVisibility() == View.VISIBLE) return;
                listener.onItemClick(pack);
            });

        } else {
            // --- WALLPAPER ---
            WallpaperViewHolder wHolder = (WallpaperViewHolder) holder;
            Config.Wallpaper wall = (Config.Wallpaper) item;
            try {
                String hexColor = wall.colorBg;
                if (hexColor == null || hexColor.isEmpty()) hexColor = "#E0E0E0";
                if (!hexColor.startsWith("#")) hexColor = "#" + hexColor;
                wHolder.imgIcon.setBackgroundColor(Color.parseColor(hexColor));
            } catch (Exception e) { wHolder.imgIcon.setBackgroundColor(Color.LTGRAY); }
            String imgUrl = baseUrl + "wallpappers/" + wall.imageFile;
            Glide.with(context).load(imgUrl).transform(new CenterCrop(), new RoundedCorners(16)).into(wHolder.imgIcon);

            try {
                // CORRECCIÓN: OCULTAR CANDADO PREMIUM SIEMPRE
                View badgePrem = wHolder.itemView.findViewById(R.id.badgePremiumWall);
                if (badgePrem != null) badgePrem.setVisibility(View.GONE);

                View badgeNew = wHolder.itemView.findViewById(R.id.badgeNew);
                if (badgeNew != null) badgeNew.setVisibility(wall.isNew ? View.VISIBLE : View.GONE);
            } catch (Exception e) {}

            // CORRECCIÓN: OCULTAR EFECTO HOLO (SI EXISTE)
            View holo = wHolder.itemView.findViewById(R.id.holoEffectView);
            if (holo != null) holo.setVisibility(View.GONE);

            // ... (Resto de lógica Wallpaper igual) ...
            if (wHolder.btnFavorite != null) {
                wHolder.btnFavorite.setVisibility(View.VISIBLE);
                String wallId = wall.imageFile;
                if (favorites.contains(wallId)) {
                    wHolder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                    wHolder.btnFavorite.setColorFilter(Color.WHITE);
                } else {
                    wHolder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                    wHolder.btnFavorite.setColorFilter(Color.WHITE);
                }
                wHolder.btnFavorite.setOnClickListener(v -> {
                    Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));
                    boolean isAdding = false;
                    if (currentFavs.contains(wallId)) {
                        currentFavs.remove(wallId);
                        wHolder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                        isAdding = false;
                    } else {
                        currentFavs.add(wallId);
                        wHolder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                        isAdding = true;
                    }
                    wHolder.btnFavorite.setColorFilter(Color.WHITE);
                    prefs.edit().putStringSet("fav_wallpapers_ids", currentFavs).apply();
                    favorites = currentFavs;
                    actualizarNube(wallId, isAdding);
                });
            }

            if (wHolder.imgGiftOverlay != null) {
                boolean isNew = wall.isNew;
                SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
                boolean isOpened = prefsGift.getBoolean("opened_" + wall.imageFile, false);
                if (isNew && !isOpened) {
                    wHolder.imgGiftOverlay.setVisibility(View.VISIBLE);
                    wHolder.imgGiftOverlay.setImageResource(R.drawable.regalow1);
                    wHolder.imgGiftOverlay.setOnClickListener(v -> {
                        if (isAnimacionEnCurso) return;
                        isAnimacionEnCurso = true;
                        reproducirSonidoPop();
                        wHolder.imgGiftOverlay.setImageResource(R.drawable.regalow2);
                        prefsGift.edit().putBoolean("opened_" + wall.imageFile, true).apply();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            wHolder.imgGiftOverlay.animate().translationY(2000f).alpha(40f).setDuration(600).withEndAction(() -> {
                                wHolder.imgGiftOverlay.setVisibility(View.GONE);
                                isAnimacionEnCurso = false;
                            }).start();
                        }, 1000);
                    });
                } else {
                    wHolder.imgGiftOverlay.setVisibility(View.GONE);
                }
            }
            wHolder.itemView.setOnClickListener(v -> {
                if (wHolder.imgGiftOverlay != null && wHolder.imgGiftOverlay.getVisibility() == View.VISIBLE) return;
                listener.onItemClick(wall);
            });
        }
    }

    private void actualizarNube(String itemId, boolean isAdding) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (isAdding) db.collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayUnion(itemId));
            else db.collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayRemove(itemId));
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class StickerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtName;
        ImageView iconAdded;
        ImageView imgGiftOverlay;
        public StickerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_pack_preview);
            txtName = itemView.findViewById(R.id.txt_pack_name);
            iconAdded = itemView.findViewById(R.id.iconAdded);
            imgGiftOverlay = itemView.findViewById(R.id.imgGiftOverlay);
        }
    }

    static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        ImageView imgGiftOverlay;
        ImageView btnFavorite;
        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgWallpaperPreview);
            imgGiftOverlay = itemView.findViewById(R.id.imgGiftOverlay);
            btnFavorite = itemView.findViewById(R.id.btnFavoriteWall);
        }
    }

    private void reproducirSonidoPop() {
        if (soundPool != null && soundLoaded) soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (soundPool != null) { soundPool.release(); soundPool = null; }
    }
}