package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class StickerPreviewAdapter extends RecyclerView.Adapter<StickerPreviewAdapter.ViewHolder> {

    private List<StickerPack.Sticker> stickerList;
    private Context context;
    private int lastPosition = -1;
    private String packFolder;

    public StickerPreviewAdapter(List<StickerPack.Sticker> stickerList, Context context, String packIdentifier) {
        this.stickerList = stickerList;
        this.context = context;
        String jsonUrl = Config.STICKER_JSON_URL;
        String base = jsonUrl.substring(0, jsonUrl.lastIndexOf("/") + 1);
        this.packFolder = base + packIdentifier + "/";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_single, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack.Sticker sticker = stickerList.get(position);
        String fileName = sticker.imageFile;

        Glide.with(context)
                .load(packFolder + fileName)
                .into(holder.imageView);

        setAnimation(holder.itemView, position);

        // --- CAMBIO: AHORA ES CLICK NORMAL ---
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof StickerDetailsActivity) {
                ((StickerDetailsActivity) context).mostrarPreviewGrande(sticker);
            }
        });
        // -------------------------------------
    }

    @Override
    public int getItemCount() {
        return stickerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sticker_image);
        }
    }

    private void setAnimation(android.view.View viewToAnimate, int position) {
        if (position > lastPosition) {
            viewToAnimate.setAlpha(0.0f);
            viewToAnimate.setTranslationY(50.0f);
            long delay = (position % 3) * 100;

            viewToAnimate.animate()
                    .alpha(1.0f)
                    .translationY(0.0f)
                    .setStartDelay(delay)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();

            lastPosition = position;
        }
    }
}