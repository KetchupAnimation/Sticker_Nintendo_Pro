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
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Importar Toast
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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

public class BatteryThemeAdapter extends RecyclerView.Adapter<BatteryThemeAdapter.ViewHolder> {

    private List<Config.BatteryTheme> themes;
    private Context context;
    private OnThemeClickListener listener;
    private int layoutId;
    private static final String BASE_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/Bateria/";
    private int lastPosition = -1;
    private SharedPreferences prefs;
    private Set<String> favorites;
    private boolean isAnimacionEnCurso = false;
    private boolean isFavoritesView = false;

    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;

    public interface OnThemeClickListener {
        void onThemeClick(Config.BatteryTheme theme);
    }

    public BatteryThemeAdapter(Context context, List<Config.BatteryTheme> themes, int layoutId, OnThemeClickListener listener) {
        this(context, themes, layoutId, listener, false);
    }

    public BatteryThemeAdapter(Context context, List<Config.BatteryTheme> themes, int layoutId, OnThemeClickListener listener, boolean isFavoritesView) {
        this.context = context;
        this.themes = themes;
        this.layoutId = layoutId;
        this.listener = listener;
        this.isFavoritesView = isFavoritesView;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build();
        soundId = soundPool.load(context, R.raw.open, 1);
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> { if (status == 0) soundLoaded = true; });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        favorites = prefs.getStringSet("fav_battery_ids", new HashSet<>());
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config.BatteryTheme theme = themes.get(position);

        try {
            int colorFondo = Color.parseColor(theme.colorBg != null && !theme.colorBg.isEmpty() ? theme.colorBg : "#E0E0E0");
            holder.imgIcon.setBackgroundColor(colorFondo);
        } catch (Exception e) { holder.imgIcon.setBackgroundColor(Color.LTGRAY); }

        String iconUrl = BASE_URL + theme.folder + "/icono.png";
        Glide.with(context).load(iconUrl).transform(new CenterCrop(), new RoundedCorners(16)).into(holder.imgIcon);

        // --- BADGES ---
        if (holder.badgeNew != null) {
            if (theme.isLimitedTime) {
                holder.badgeNew.setVisibility(View.VISIBLE);
                holder.badgeNew.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Verde
                TextView txt = holder.itemView.findViewById(R.id.txtBadgeNew);
                if (txt != null) txt.setText("24h");
            }
            else if (theme.isNew) {
                holder.badgeNew.setVisibility(View.VISIBLE);
                holder.badgeNew.setCardBackgroundColor(Color.parseColor("#D50000")); // Rojo
                TextView txt = holder.itemView.findViewById(R.id.txtBadgeNew);
                if (txt != null) txt.setText("NEW");
            }
            else {
                holder.badgeNew.setVisibility(View.GONE);
            }
        }

        String themeId = theme.id;
        if (favorites.contains(themeId)) {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            holder.btnFavorite.setColorFilter(Color.WHITE);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            holder.btnFavorite.setColorFilter(Color.WHITE);
        }

        // --- FAVORITOS (CON MONETIZACIÓN) ---
        holder.btnFavorite.setOnClickListener(v -> {
            Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_battery_ids", new HashSet<>()));

            if (currentFavs.contains(themeId)) {
                // --- QUITAR LIKE ---
                if (theme.isLimitedTime) {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Warning!")
                            .setMessage("This is a Limited Time item.\nIf you remove it, you might lose it forever!")
                            .setPositiveButton("Remove", (dialog, which) -> {
                                ejecutarAccionQuitarLike(themeId, holder);
                            })
                            .setNegativeButton("Keep it", null)
                            .show();
                } else {
                    ejecutarAccionQuitarLike(themeId, holder);
                }
            } else {
                // --- DAR LIKE ---
                if (theme.isLimitedTime) {
                    // MONETIZACIÓN: Ver anuncio primero
                    if (context instanceof MainActivity) {
                        CustomToast.makeText(context, "Watch Ad to save this exclusive item!", Toast.LENGTH_SHORT).show();
                        ((MainActivity) context).cargarAnuncioYEjecutar(() -> {
                            procederADarLike(themeId, holder, true);
                        });
                    } else {
                        procederADarLike(themeId, holder, true);
                    }
                } else {
                    // Normal
                    procederADarLike(themeId, holder, false);
                }
            }
        });

        // Regalo
        if (holder.imgGiftOverlay != null) {
            boolean isNew = theme.isNew;
            SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
            boolean isOpened = prefsGift.getBoolean("opened_bat_" + theme.id, false);
            if (isNew && !isOpened) {
                holder.imgGiftOverlay.setVisibility(View.VISIBLE);
                holder.imgGiftOverlay.setImageResource(R.drawable.regalow1);
                holder.imgGiftOverlay.setOnClickListener(v -> {
                    if (isAnimacionEnCurso) return;
                    isAnimacionEnCurso = true;
                    reproducirSonidoPop();
                    holder.imgGiftOverlay.setImageResource(R.drawable.regalow2);
                    prefsGift.edit().putBoolean("opened_bat_" + theme.id, true).apply();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        holder.imgGiftOverlay.animate().translationY(2000f).alpha(0f).setDuration(600).withEndAction(() -> {
                            holder.imgGiftOverlay.setVisibility(View.GONE);
                            isAnimacionEnCurso = false;
                        }).start();
                    }, 500);
                });
            } else {
                holder.imgGiftOverlay.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (holder.imgGiftOverlay != null && holder.imgGiftOverlay.getVisibility() == View.VISIBLE) return;
            listener.onThemeClick(theme);
        });

        setAnimation(holder.itemView, position);
    }

    // --- MÉTODOS AUXILIARES ---

    private void procederADarLike(String themeId, ViewHolder holder, boolean isLimited) {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_battery_ids", new HashSet<>()));
        currentFavs.add(themeId);

        holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
        holder.btnFavorite.setColorFilter(Color.WHITE);

        prefs.edit().putStringSet("fav_battery_ids", currentFavs).apply();
        favorites = currentFavs;
        actualizarNube(themeId, true);

        // --- RASTREO ANALYTICS (NUEVO) ---
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, themeId);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "battery_theme");
        mFirebaseAnalytics.logEvent("battery_liked", bundle);
        // ---------------------------------

        if (isLimited) {
            CustomToast.makeText(context, "Saved forever!", Toast.LENGTH_SHORT).show();
        }
    }

    private void ejecutarAccionQuitarLike(String themeId, ViewHolder holder) {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_battery_ids", new HashSet<>()));
        currentFavs.remove(themeId);

        holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
        holder.btnFavorite.setColorFilter(Color.WHITE);

        if (isFavoritesView) {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                themes.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, themes.size());
            }
        }

        prefs.edit().putStringSet("fav_battery_ids", currentFavs).apply();
        favorites = currentFavs;
        actualizarNube(themeId, false);
    }

    private void actualizarNube(String itemId, boolean isAdding) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (isAdding) db.collection("users").document(user.getUid()).update("fav_battery", FieldValue.arrayUnion(itemId));
            else db.collection("users").document(user.getUid()).update("fav_battery", FieldValue.arrayRemove(itemId));
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 100.0f, Animation.ABSOLUTE, 0.0f);
            translate.setDuration(400);
            Animation alpha = new android.view.animation.AlphaAnimation(0.0f, 1.0f);
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
        if (soundPool != null && soundLoaded) soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void updateData() {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        this.favorites = prefs.getStringSet("fav_battery_ids", new HashSet<>());
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return themes.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon, btnFavorite, imgGiftOverlay;
        CardView badgeNew;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgThemeIcon);
            badgeNew = itemView.findViewById(R.id.badgeNew);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            imgGiftOverlay = itemView.findViewById(R.id.imgGiftOverlay);
        }
    }
}