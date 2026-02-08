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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.List;

public class IdThemeAdapter extends RecyclerView.Adapter<IdThemeAdapter.ViewHolder> {

    private final List<IdWalletActivity.IdTheme> themes;
    private final OnThemeSelectedListener listener;
    private Context context;

    // Variables de Sonido
    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;

    // Variable estática para evitar conflictos de múltiples clics rápidos
    private static boolean isAnimacionEnCurso = false;

    // Variable para animación escalonada
    private int lastPosition = -1;

    public interface OnThemeSelectedListener {
        void onThemeSelected(IdWalletActivity.IdTheme theme);
    }

    public IdThemeAdapter(List<IdWalletActivity.IdTheme> themes, OnThemeSelectedListener listener) {
        this.themes = themes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_id_card, parent, false);

        if (soundPool == null) {
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

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IdWalletActivity.IdTheme theme = themes.get(position);
        String imageUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/ID/" + theme.imageFile;

        // 1. Configuración Visual Básica
        try {
            int color = Color.parseColor(theme.colorBg);
            holder.img.setBackgroundColor(color);
        } catch (Exception e) {
            holder.img.setBackgroundColor(Color.LTGRAY);
        }

        Glide.with(context).load(imageUrl).into(holder.img);

        if (theme.isNew) {
            holder.badgeNew.setVisibility(View.VISIBLE);
        } else {
            holder.badgeNew.setVisibility(View.GONE);
        }

        // ============================================================
        // CORRECCIÓN CLAVE AQUÍ:
        // El listener de la tarjeta se asigna SIEMPRE, no solo en el 'else'.
        // ============================================================
        holder.itemView.setOnClickListener(v -> {
            // Si el regalo está visible, ignoramos el clic en la tarjeta de abajo
            if (holder.imgGiftOverlay.getVisibility() == View.VISIBLE) return;
            listener.onThemeSelected(theme);
        });


        // 2. Lógica del Regalo
        SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
        boolean isOpened = prefsGift.getBoolean("opened_id_" + theme.id, false);

        if (theme.isNew && !isOpened) {
            holder.imgGiftOverlay.setVisibility(View.VISIBLE);
            holder.imgGiftOverlay.setImageResource(R.drawable.regalow1);
            holder.imgGiftOverlay.setAlpha(1.0f);

            // Aseguramos que el regalo capture el clic
            holder.imgGiftOverlay.setClickable(true);

            holder.imgGiftOverlay.setOnClickListener(v -> {
                if (isAnimacionEnCurso) return;
                isAnimacionEnCurso = true;

                reproducirSonidoPop();

                holder.imgGiftOverlay.setImageResource(R.drawable.regalow2);
                prefsGift.edit().putBoolean("opened_id_" + theme.id, true).apply();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    holder.imgGiftOverlay.animate()
                            .translationY(500f)
                            .alpha(0f)
                            .setDuration(600)
                            .withEndAction(() -> {
                                holder.imgGiftOverlay.setVisibility(View.GONE);
                                isAnimacionEnCurso = false; // Liberamos el bloqueo
                                listener.onThemeSelected(theme); // Seleccionamos la tarjeta
                            })
                            .start();
                }, 1000);
            });
        } else {
            holder.imgGiftOverlay.setVisibility(View.GONE);
            holder.imgGiftOverlay.setOnClickListener(null); // Limpieza
            holder.imgGiftOverlay.setClickable(false);
        }

        // 3. Animación de Entrada
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    private void reproducirSonidoPop() {
        if (soundPool != null && soundLoaded) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation translate = new TranslateAnimation(
                    Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f,
                    Animation.ABSOLUTE, 100.0f, Animation.ABSOLUTE, 0.0f);
            translate.setDuration(400);
            AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
            alpha.setDuration(400);
            animSet.addAnimation(translate);
            animSet.addAnimation(alpha);
            long delay = (position % 4) * 100;
            animSet.setStartOffset(delay);
            animSet.setInterpolator(new DecelerateInterpolator());
            viewToAnimate.startAnimation(animSet);
            lastPosition = position;
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, imgGiftOverlay;
        View badgeNew;

        ViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.imgThemePreview);
            imgGiftOverlay = v.findViewById(R.id.imgGiftOverlay);
            badgeNew = v.findViewById(R.id.badgeNew);
        }
    }
}