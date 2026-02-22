package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;

public class GachaUnboxAdapter extends RecyclerView.Adapter<GachaUnboxAdapter.ViewHolder> {

    private Context context;
    private StickerPack pack;
    private GachaUnboxActivity activity;
    private boolean[] isUnlocked; // Para saber cuáles ya tocamos

    public GachaUnboxAdapter(Context context, StickerPack pack, GachaUnboxActivity activity) {
        this.context = context;
        this.pack = pack;
        this.activity = activity;
        this.isUnlocked = new boolean[pack.stickers.size()];
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reciclamos el diseño de sticker individual que ya tienes en tu app
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker_single, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack.Sticker sticker = pack.stickers.get(position);

        // 👇 CORRECCIÓN: Descargar directo de GitHub para la previsualización 👇
        String baseUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/contents/";
        String stickerUrl = baseUrl + pack.identifier + "/" + sticker.imageFile;

        Glide.with(context).load(stickerUrl).into(holder.imgSticker);

        // LÓGICA DE SILUETAS
        if (isUnlocked[position]) {
            // Si ya está revelado, le quitamos el filtro oscuro
            holder.imgSticker.clearColorFilter();
            holder.imgSticker.setAlpha(1.0f);
        } else {
            // Si no está revelado, lo teñimos de gris oscuro/negro
            holder.imgSticker.setColorFilter(Color.parseColor("#333333"), PorterDuff.Mode.SRC_IN);
            holder.imgSticker.setAlpha(0.8f);
        }

        // ACCIÓN AL TOCAR
        holder.itemView.setOnClickListener(v -> {
            if (!isUnlocked[position] && activity.unlockedCount < 3) {
                isUnlocked[position] = true;

                // Efecto Pop animado
                holder.imgSticker.setScaleX(0.5f);
                holder.imgSticker.setScaleY(0.5f);
                holder.imgSticker.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction(() -> {
                    holder.imgSticker.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }).start();

                notifyItemChanged(position);
                activity.onStickerUnlocked();
            }
        });
    }

    @Override
    public int getItemCount() {
        return pack.stickers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSticker;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSticker = itemView.findViewById(R.id.sticker_image); // Asumiendo que así se llama en tu item_sticker_single.xml
        }
    }
}