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
import android.widget.Toast; // Importar
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class WidgetGalleryAdapter extends RecyclerView.Adapter<WidgetGalleryAdapter.ViewHolder> {

    private Context context;
    private List<Integer> imageIds;
    private Set<String> favorites;
    private SharedPreferences prefs;
    private boolean isHome;
    private int lastPosition = -1;
    private Map<Integer, String> statusMap;
    private boolean isAnimacionEnCurso = false;
    private boolean isFavoritesView = false;

    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;
    private static final String BASE_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/4x2/BG_W_";

    public WidgetGalleryAdapter(Context context, List<Integer> imageIds, boolean isHome) {
        this(context, imageIds, isHome, new HashMap<>(), false);
    }

    public WidgetGalleryAdapter(Context context, List<Integer> imageIds, boolean isHome, Map<Integer, String> statusMap) {
        this(context, imageIds, isHome, statusMap, false);
    }

    public WidgetGalleryAdapter(Context context, List<Integer> imageIds, boolean isHome, Map<Integer, String> statusMap, boolean isFavoritesView) {
        this.context = context;
        this.imageIds = imageIds;
        this.isHome = isHome;
        this.statusMap = statusMap != null ? statusMap : new HashMap<>();
        this.isFavoritesView = isFavoritesView;

        prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
        favorites = prefs.getStringSet("fav_wallpapers", new HashSet<>());

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build();
        soundId = soundPool.load(context, R.raw.open, 1);
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> { if (status == 0) soundLoaded = true; });
    }

    public void updateData() {
        SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
        this.favorites = prefs.getStringSet("fav_wallpapers", new HashSet<>());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_widget_gallery, parent, false);
        if (isHome) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            int anchoManualEnDp = 190;
            layoutParams.width = (int) (anchoManualEnDp * context.getResources().getDisplayMetrics().density);
            view.setLayoutParams(layoutParams);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int imageNumber = imageIds.get(position);
        String imageId = String.valueOf(imageNumber);
        String url = BASE_URL + String.format("%02d", imageNumber) + ".png";

        Glide.with(context).load(url).transform(new CenterCrop(), new RoundedCorners(24)).into(holder.imgWallpaper);

        // --- ESTADOS ---
        if (statusMap.containsKey(imageNumber)) {
            String status = statusMap.get(imageNumber);
            holder.cardStatus.setVisibility(View.VISIBLE);

            if (status != null && status.equalsIgnoreCase("limited")) {
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#4CAF50"));
                holder.txtStatus.setText("24H");
            }
            else if (status != null && status.equalsIgnoreCase("new")) {
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#D50000"));
                holder.txtStatus.setText("NEW");
            }
            else if (status != null && status.equalsIgnoreCase("updated")) {
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#2196F3"));
                holder.txtStatus.setText("UPDATED");
            }
            else {
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#4CAF50"));
                holder.txtStatus.setText(status != null ? status.toUpperCase() : "");
            }
        } else {
            holder.cardStatus.setVisibility(View.GONE);
        }

        // --- REGALO ---
        if (holder.imgGiftOverlay != null) {
            boolean isNew = false;
            if (statusMap.containsKey(imageNumber)) {
                String status = statusMap.get(imageNumber);
                if (status != null && (status.equalsIgnoreCase("new") || status.equalsIgnoreCase("limited"))) {
                    isNew = true;
                }
            }
            SharedPreferences prefsGift = context.getSharedPreferences("GiftMonitorWidgets", Context.MODE_PRIVATE);
            boolean isOpened = prefsGift.getBoolean("opened_" + imageId, false);

            if (isNew && !isOpened) {
                holder.imgGiftOverlay.setVisibility(View.VISIBLE);
                holder.imgGiftOverlay.setImageResource(R.drawable.regalog1);
                holder.cardStatus.setVisibility(View.GONE);
                holder.imgGiftOverlay.setOnClickListener(v -> {
                    if (isAnimacionEnCurso) return;
                    isAnimacionEnCurso = true;
                    reproducirSonidoPop();
                    holder.imgGiftOverlay.setImageResource(R.drawable.regalog2);
                    prefsGift.edit().putBoolean("opened_" + imageId, true).apply();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        holder.cardStatus.setVisibility(View.VISIBLE);
                        holder.imgGiftOverlay.animate().translationY(2000f).alpha(40f).setDuration(600).withEndAction(() -> {
                            holder.imgGiftOverlay.setVisibility(View.GONE);
                            isAnimacionEnCurso = false;
                        }).start();
                    }, 1000);
                });
            } else {
                holder.imgGiftOverlay.setVisibility(View.GONE);
            }
        }

        actualizarIconoCorazon(holder, imageId);

        // --- FAVORITOS (CON MONETIZACIÓN) ---
        holder.btnFavorite.setOnClickListener(v -> {
            Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers", new HashSet<>()));

            if (currentFavs.contains(imageId)) {
                // --- QUITAR LIKE ---
                String status = statusMap.get(imageNumber);
                boolean isLimited = status != null && status.equalsIgnoreCase("limited");

                if (isLimited) {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Warning!")
                            .setMessage("This is a Limited Time item.\nIf you remove it, you might lose it forever!")
                            .setPositiveButton("Remove", (dialog, which) -> {
                                ejecutarAccionQuitarLike(imageId, holder);
                            })
                            .setNegativeButton("Keep it", null)
                            .show();
                } else {
                    ejecutarAccionQuitarLike(imageId, holder);
                }
            } else {
                // --- DAR LIKE ---
                String status = statusMap.get(imageNumber);
                boolean isLimited = status != null && status.equalsIgnoreCase("limited");

                if (isLimited) {
                    // MONETIZACIÓN
                    if (context instanceof MainActivity) {
                        Toast.makeText(context, "Watch Ad to save this exclusive item!", Toast.LENGTH_SHORT).show();
                        ((MainActivity) context).cargarAnuncioYEjecutar(() -> {
                            procederADarLike(imageId, holder, true);
                        });
                    } else {
                        procederADarLike(imageId, holder, true);
                    }
                } else {
                    // NORMAL
                    procederADarLike(imageId, holder, false);
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (holder.imgGiftOverlay != null && holder.imgGiftOverlay.getVisibility() == View.VISIBLE) return;
            android.content.Intent intent = new android.content.Intent(context, WidgetPreviewActivity.class);
            intent.putExtra("IMAGE_ID", imageId);
            intent.putExtra("WIDGET_TYPE", "4x2");
            context.startActivity(intent);
        });

        setAnimation(holder.itemView, position);
    }

    // --- MÉTODOS AUXILIARES ---

    private void procederADarLike(String imageId, ViewHolder holder, boolean isLimited) {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers", new HashSet<>()));
        currentFavs.add(imageId);

        holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
        holder.btnFavorite.setColorFilter(Color.WHITE);

        prefs.edit().putStringSet("fav_wallpapers", currentFavs).apply();
        favorites = currentFavs;
        actualizarNube(imageId, true);

        // --- RASTREO ANALYTICS (NUEVO) ---
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, imageId);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "widget");
        mFirebaseAnalytics.logEvent("widget_liked", bundle); // Evento simple
        // ---------------------------------

        if (isLimited) {
            Toast.makeText(context, "Saved forever!", Toast.LENGTH_SHORT).show();
        }
    }

    private void ejecutarAccionQuitarLike(String imageId, ViewHolder holder) {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers", new HashSet<>()));
        currentFavs.remove(imageId);

        holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
        holder.btnFavorite.setColorFilter(Color.WHITE);

        if (isFavoritesView) {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                imageIds.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, imageIds.size());
            }
        }

        prefs.edit().putStringSet("fav_wallpapers", currentFavs).apply();
        favorites = currentFavs;
        actualizarNube(imageId, false);
    }

    private void actualizarNube(String itemId, boolean isAdding) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (isAdding) db.collection("users").document(user.getUid()).update("fav_widgets", FieldValue.arrayUnion(itemId));
            else db.collection("users").document(user.getUid()).update("fav_widgets", FieldValue.arrayRemove(itemId));
        }
    }

    private void actualizarIconoCorazon(ViewHolder holder, String imageId) {
        if (favorites.contains(imageId)) {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            holder.btnFavorite.setColorFilter(Color.WHITE);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            holder.btnFavorite.setColorFilter(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() { return imageIds.size(); }

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
        ImageView imgWallpaper, btnFavorite, imgGiftOverlay;
        CardView cardStatus;
        TextView txtStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgWallpaper = itemView.findViewById(R.id.imgWidgetPreview);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            cardStatus = itemView.findViewById(R.id.cardStatusBadge);
            txtStatus = itemView.findViewById(R.id.txtStatusBadge);
            imgGiftOverlay = itemView.findViewById(R.id.imgGiftOverlay);
        }
    }

    private void reproducirSonidoPop() {
        if (soundPool != null && soundLoaded) soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (soundPool != null) { soundPool.release(); soundPool = null; }
    }
}