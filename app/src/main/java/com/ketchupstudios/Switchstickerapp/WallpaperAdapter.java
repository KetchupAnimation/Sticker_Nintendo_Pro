package com.ketchupstudios.Switchstickerapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

    private final List<Config.Wallpaper> wallpapers;
    private final int layoutId;
    private Context context;
    private boolean isAnimacionEnCurso = false;
    private boolean isFavoritesView = false;

    private SharedPreferences prefs;
    private Set<String> favorites;
    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;
    private int lastPosition = -1;

    public WallpaperAdapter(List<Config.Wallpaper> wallpapers, int layoutId) {
        this(wallpapers, layoutId, false);
    }

    public WallpaperAdapter(List<Config.Wallpaper> wallpapers, int layoutId, boolean isFavoritesView) {
        this.wallpapers = wallpapers;
        this.layoutId = layoutId;
        this.isFavoritesView = isFavoritesView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        favorites = prefs.getStringSet("fav_wallpapers_ids", new HashSet<>());

        if (soundPool == null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build();
            try {
                soundId = soundPool.load(context, R.raw.open, 1);
                soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> { if (status == 0) soundLoaded = true; });
            } catch (Exception e) {}
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config.Wallpaper wall = wallpapers.get(position);

        try {
            int colorFondo = Color.parseColor(wall.colorBg != null && !wall.colorBg.isEmpty() ? wall.colorBg : "#E0E0E0");
            holder.img.setBackgroundColor(colorFondo);
        } catch (Exception e) { holder.img.setBackgroundColor(Color.LTGRAY); }

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        Glide.with(context)
                .load(baseUrl + "wallpappers/" + wall.imageFile)
                .transform(new CenterCrop(), new RoundedCorners(16))
                .into(holder.img);

        // --- ETIQUETAS ---
        if (holder.badgeNew != null) {
            TextView txt = holder.itemView.findViewById(R.id.txtBadgeNew);
            if (wall.rewardDay != null && !wall.rewardDay.isEmpty()) {
                holder.badgeNew.setVisibility(View.VISIBLE);
                if (holder.badgeNew instanceof CardView) ((CardView) holder.badgeNew).setCardBackgroundColor(Color.parseColor("#FFD700"));
                if (txt != null) { txt.setText("EVENT"); txt.setTextColor(Color.BLACK); }
            }
            else if (wall.isLimitedTime) {
                holder.badgeNew.setVisibility(View.VISIBLE);
                if (holder.badgeNew instanceof CardView) ((CardView) holder.badgeNew).setCardBackgroundColor(Color.parseColor("#4CAF50"));
                if (txt != null) { txt.setText("24h"); txt.setTextColor(Color.WHITE); }
            }
            else if (wall.isNew) {
                holder.badgeNew.setVisibility(View.VISIBLE);
                if (holder.badgeNew instanceof CardView) ((CardView) holder.badgeNew).setCardBackgroundColor(Color.parseColor("#D50000"));
                if (txt != null) { txt.setText("NEW"); txt.setTextColor(Color.WHITE); }
            }
            else { holder.badgeNew.setVisibility(View.GONE); }
        }

        if (holder.badgePremium != null) holder.badgePremium.setVisibility(View.GONE);

        // --- HOLO ---
        if (holder.holoView != null) {
            boolean isEventReward = (wall.rewardDay != null && !wall.rewardDay.isEmpty());
            boolean isLimitedActive = (wall.isLimitedTime && !wall.isExpired);
            if (isLimitedActive || isEventReward) holder.holoView.setVisibility(View.VISIBLE);
            else holder.holoView.setVisibility(View.GONE);
        }

        // --- FAVORITOS (BTN LISTA) ---
        holder.btnFavorite.setVisibility(View.VISIBLE);
        String wallId = wall.imageFile;

        if (favorites.contains(wallId)) {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            holder.btnFavorite.setColorFilter(Color.WHITE);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            holder.btnFavorite.setColorFilter(Color.WHITE);
        }

        // ================================================================
        // AQUÍ ESTABA EL PROBLEMA: LÓGICA DEL CLIC EN EL CORAZÓN DE LA LISTA
        // ================================================================
        holder.btnFavorite.setOnClickListener(v -> {
            Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));

            if (currentFavs.contains(wallId)) {
                // --- INTENTANDO QUITAR LIKE ---

                // Verificamos si es LIMITADO (24h) ... O ... si es OCULTO (Evento Snorlax)
                if (wall.isLimitedTime || wall.isHidden) {

                    // Mostramos la alerta NUEVA (Método creado abajo)
                    mostrarAlertaPersonalizada(wallId, holder);

                } else {
                    // Si es normal, borramos directo
                    ejecutarAccionQuitarLike(wallId, holder);
                }
            } else {
                // --- DAR LIKE ---
                if (wall.isLimitedTime) {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).analizarClickWallpaper(wall);
                    } else {
                        procederADarLike(wallId, holder, true);
                    }
                } else {
                    procederADarLike(wallId, holder, false);
                }
            }
        });
        // ================================================================

        // --- REGALO ---
        if (holder.imgGiftOverlay != null) {
            boolean isNew = wall.isNew;
            SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
            boolean isOpened = prefsGift.getBoolean("opened_" + wall.imageFile, false);
            boolean isEventReward = (wall.rewardDay != null && !wall.rewardDay.isEmpty());

            if (isNew && !isOpened && !isEventReward) {
                holder.imgGiftOverlay.setVisibility(View.VISIBLE);
                holder.imgGiftOverlay.setAlpha(1.0f);
                holder.imgGiftOverlay.setImageResource(R.drawable.regalow1);
                holder.imgGiftOverlay.setOnClickListener(v -> {
                    if (isAnimacionEnCurso) return;
                    isAnimacionEnCurso = true;
                    reproducirSonidoPop();
                    holder.imgGiftOverlay.setImageResource(R.drawable.regalow2);
                    prefsGift.edit().putBoolean("opened_" + wall.imageFile, true).apply();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        holder.imgGiftOverlay.animate().translationY(2000f).alpha(0f).setDuration(600).withEndAction(() -> {
                            holder.imgGiftOverlay.setVisibility(View.GONE);
                            isAnimacionEnCurso = false;
                        }).start();
                    }, 1000);
                });
            } else { holder.imgGiftOverlay.setVisibility(View.GONE); }
        }

        // --- CLICK EN EL ITEM (PARA ABRIR DETALLES) ---
        holder.itemView.setOnClickListener(v -> {
            if (holder.imgGiftOverlay != null && holder.imgGiftOverlay.getVisibility() == View.VISIBLE) return;

            if (context instanceof MainActivity) {
                // CORREGIDO: Usamos 'wall', no 'wallpaper'
                ((MainActivity) context).analizarClickWallpaper(wall);
            }
            else if (context instanceof FullListActivity) {
                // Asegúrate de tener el método en FullListActivity también
                ((FullListActivity) context).analizarClickWallpaper(wall);
            }
            else {
                // FAVORITOS (Abre directo)
                Intent intent = new Intent(context, WallpaperDetailsActivity.class);
                intent.putExtra("wall_name", wall.name);
                intent.putExtra("wall_author", wall.publisher);
                intent.putExtra("wall_image", wall.imageFile);
                intent.putExtra("wall_color", wall.colorBg);
                intent.putExtra("wall_artist_link", wall.artistLink);
                intent.putExtra("is_limited", wall.isLimitedTime);
                intent.putExtra("is_hidden", wall.isHidden);
                context.startActivity(intent);
            }
        });

        setAnimation(holder.itemView, position);
    }

    // ==========================================================
    // MÉTODO NUEVO PARA MOSTRAR LA ALERTA PERSONALIZADA
    // ==========================================================
    private void mostrarAlertaPersonalizada(String wallId, ViewHolder holder) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);

        // Inflamos el XML de diseño que creaste (layout_dialog_warning)
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.layout_dialog_warning, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnKeep = view.findViewById(R.id.btnKeepIt);
        TextView btnRemove = view.findViewById(R.id.btnRemoveAnyway);

        // OPCIÓN 1: QUEDÁRSELO
        btnKeep.setOnClickListener(v -> dialog.dismiss());

        // OPCIÓN 2: BORRARLO
        btnRemove.setOnClickListener(v -> {
            ejecutarAccionQuitarLike(wallId, holder); // Llamamos a la función de borrado
            dialog.dismiss();
        });

        dialog.show();
    }
    // ==========================================================

    private void procederADarLike(String wallId, ViewHolder holder, boolean isLimited) {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));
        currentFavs.add(wallId);
        holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
        holder.btnFavorite.setColorFilter(Color.WHITE);

        prefs.edit().putStringSet("fav_wallpapers_ids", currentFavs).apply();
        favorites = currentFavs;
        actualizarNube(wallId, true);

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, wallId);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "wallpaper");
        bundle.putString("action_type", "favorite");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        if (isLimited) {
            Toast.makeText(context, "Saved forever in your account!", Toast.LENGTH_LONG).show();
        }
    }

    private void ejecutarAccionQuitarLike(String wallId, ViewHolder holder) {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));
        currentFavs.remove(wallId);
        holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
        holder.btnFavorite.setColorFilter(Color.WHITE);

        if (isFavoritesView) {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                try {
                    wallpapers.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, wallpapers.size());
                } catch (Exception e) {}
            }
        }

        prefs.edit().putStringSet("fav_wallpapers_ids", currentFavs).apply();
        favorites = currentFavs;
        actualizarNube(wallId, false);
    }

    private void actualizarNube(String itemId, boolean isAdding) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (isAdding) db.collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayUnion(itemId));
            else db.collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayRemove(itemId));
        }
    }

    @Override
    public int getItemCount() { return wallpapers.size(); }

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

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        holder.itemView.clearAnimation();
        super.onViewDetachedFromWindow(holder);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, btnFavorite, imgGiftOverlay;
        View badgeNew, badgePremium;
        HoloCardView holoView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgWallpaperPreview);
            btnFavorite = itemView.findViewById(R.id.btnFavoriteWall);
            badgeNew = itemView.findViewById(R.id.badgeNew);
            badgePremium = itemView.findViewById(R.id.badgePremiumWall);
            imgGiftOverlay = itemView.findViewById(R.id.imgGiftOverlay);
            holoView = itemView.findViewById(R.id.holoEffectView);
        }
    }

    public void updateData() {
        if (context == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
        this.favorites = prefs.getStringSet("fav_wallpapers_ids", new java.util.HashSet<>());

        // No reseteamos lastPosition aquí para que no se repita la animación de entrada
        notifyDataSetChanged();
    }

    private void reproducirSonidoPop() {
        if (soundPool != null && soundLoaded) {
            try {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        context = null;
    }
}