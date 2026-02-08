package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.List;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

public class StickerPackAdapter extends RecyclerView.Adapter<StickerPackAdapter.ViewHolder> {

    private final List<StickerPack> packs;
    private final Context context;
    private final int layoutId;
    private int lastPosition = -1;
    private boolean isAnimacionEnCurso = false;

    // Variables de sonido SoundPool
    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;

    public StickerPackAdapter(List<StickerPack> packs, Context context) {
        this(packs, context, R.layout.item_sticker_pack);
    }

    public StickerPackAdapter(List<StickerPack> packs, Context context, int layoutId) {
        this.packs = packs;
        this.context = context;
        this.layoutId = layoutId;

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack pack = packs.get(position);
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);

        Glide.with(context)
                .load(baseUrl + pack.identifier + "/" + pack.trayImageFile)
                .transform(new CenterCrop(), new RoundedCorners(16))
                .into(holder.imgPreview);

        if (holder.badgeNew != null) holder.badgeNew.setVisibility("new".equalsIgnoreCase(pack.status) ? View.VISIBLE : View.GONE);
        if (holder.badgeUpdated != null) holder.badgeUpdated.setVisibility("updated".equalsIgnoreCase(pack.status) ? View.VISIBLE : View.GONE);
        // Ocultamos el candado visualmente porque ahora TODOS son "premium" (piden anuncio)
        if (holder.badgePremium != null) holder.badgePremium.setVisibility(View.GONE);

        boolean isAdded = context.getSharedPreferences("StickerMonitor", Context.MODE_PRIVATE)
                .getBoolean(pack.identifier, false);
        if (holder.iconAdded != null) {
            holder.iconAdded.setVisibility(isAdded ? View.VISIBLE : View.GONE);
        }

        // =================================================================
        // LÓGICA DEL REGALO
        // =================================================================
        if (holder.imgGiftOverlay != null) {
            boolean isNew = "new".equalsIgnoreCase(pack.status);
            SharedPreferences prefs = context.getSharedPreferences("GiftMonitor", Context.MODE_PRIVATE);
            boolean isOpened = prefs.getBoolean("opened_" + pack.identifier, false);

            if (isNew && !isOpened) {
                holder.imgGiftOverlay.setVisibility(View.VISIBLE);
                holder.imgGiftOverlay.setAlpha(1.0f);
                holder.imgGiftOverlay.setImageResource(R.drawable.regalo1);

                holder.txtName.setVisibility(View.INVISIBLE);
                holder.txtName.setText(pack.name);

                holder.imgGiftOverlay.setOnClickListener(v -> {
                    if (isAnimacionEnCurso) return;
                    isAnimacionEnCurso = true;

                    reproducirSonidoPop(); // USAR MÉTODO SOUNDPOOL
                    holder.imgGiftOverlay.setImageResource(R.drawable.regalo2);
                    prefs.edit().putBoolean("opened_" + pack.identifier, true).apply();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        holder.txtName.setVisibility(View.VISIBLE);

                        holder.imgGiftOverlay.animate()
                                .translationY(2000f)
                                .rotation(0f)
                                .alpha(40f)
                                .setDuration(600)
                                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                .withEndAction(() -> {
                                    holder.imgGiftOverlay.setVisibility(View.GONE);
                                    holder.imgGiftOverlay.setTranslationY(0f);
                                    holder.imgGiftOverlay.setRotation(0f);
                                    holder.imgGiftOverlay.setAlpha(1f);
                                    isAnimacionEnCurso = false;
                                })
                                .start();
                    }, 1000);
                });

            } else {
                holder.imgGiftOverlay.setVisibility(View.GONE);
                holder.txtName.setVisibility(View.VISIBLE);
                holder.txtName.setText(pack.name);
                holder.imgGiftOverlay.setOnClickListener(null);
            }
        } else {
            holder.txtName.setText(pack.name);
            holder.txtName.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (holder.imgGiftOverlay != null && holder.imgGiftOverlay.getVisibility() == View.VISIBLE) {
                return;
            }

            // --- MONETIZACIÓN TOTAL ---
            // En lugar de preguntar si es premium, mandamos TODOS al método de anuncio
            if (context instanceof MainActivity) {
                ((MainActivity) context).intentarAbrirPackPremium(pack);
            } else if (context instanceof FullListActivity) {
                ((FullListActivity) context).intentarAbrirPackPremium(pack);
            } else {
                // Fallback por si acaso
                Config.selectedPack = pack;
                Intent intent = new Intent(context, StickerDetailsActivity.class);
                context.startActivity(intent);
            }
        });

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return packs.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f,
                    Animation.ABSOLUTE, 100.0f, Animation.ABSOLUTE, 0.0f);
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
        ImageView imgPreview;
        TextView txtName;
        View badgeNew, badgeUpdated, badgePremium;
        ImageView iconAdded, imgGiftOverlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.img_pack_preview);
            txtName = itemView.findViewById(R.id.txt_pack_name);
            badgeNew = itemView.findViewById(R.id.badgeNew);
            badgeUpdated = itemView.findViewById(R.id.badgeUpdated);
            badgePremium = itemView.findViewById(R.id.badgePremium);
            iconAdded = itemView.findViewById(R.id.iconAdded);
            imgGiftOverlay = itemView.findViewById(R.id.imgGiftOverlay);
        }
    }

    // MÉTODO NUEVO CON SOUNDPOOL
    private void reproducirSonidoPop() {
        if (soundPool != null && soundLoaded) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    // --- LIMPIEZA DE MEMORIA (EVITA CRASHES) ---
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (soundPool != null) {
            soundPool.release(); // Libera la memoria del audio
            soundPool = null;
        }
    }
}