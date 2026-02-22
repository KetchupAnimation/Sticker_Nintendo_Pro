package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.Set;

public class GachaUnboxAdapter extends RecyclerView.Adapter<GachaUnboxAdapter.ViewHolder> {

    private Context context;
    private StickerPack pack;
    private GachaUnboxActivity activity;
    private boolean[] isUnlocked;

    // Recibimos la colección ya desbloqueada
    public GachaUnboxAdapter(Context context, StickerPack pack, GachaUnboxActivity activity, Set<String> unlockedSet) {
        this.context = context;
        this.pack = pack;
        this.activity = activity;
        this.isUnlocked = new boolean[pack.stickers.size()];

        // Coloreamos los que ya ganó en el pasado
        for(int i = 0; i < pack.stickers.size(); i++) {
            if(unlockedSet.contains(pack.stickers.get(i).imageFile)) {
                isUnlocked[i] = true;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker_single, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack.Sticker sticker = pack.stickers.get(position);

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        final String stickerUrl = baseUrl + pack.identifier + "/" + sticker.imageFile;

        // 👇 USO DE fitCenter() PARA QUE NO SE RECORTE LA IMAGEN 👇
        Glide.with(context).load(stickerUrl).fitCenter().into(holder.imgSticker);

        if (isUnlocked[position]) {
            holder.imgSticker.clearColorFilter();
            holder.imgSticker.setAlpha(1.0f);
        } else {
            holder.imgSticker.setColorFilter(Color.parseColor("#444444"), PorterDuff.Mode.SRC_IN);
            holder.imgSticker.setAlpha(0.6f);
        }

        // 👇 LÓGICA DE UN SOLO TOQUE 👇
        holder.itemView.setOnClickListener(v -> {
            if (!isUnlocked[position]) {
                // Es una silueta. Revisamos si aún puede desbloquear en esta sesión
                if (activity.sessionUnlockedCount < activity.maxSessionUnlocks) {
                    isUnlocked[position] = true;

                    holder.imgSticker.setScaleX(0.5f);
                    holder.imgSticker.setScaleY(0.5f);
                    holder.imgSticker.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction(() -> {
                        holder.imgSticker.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    }).start();

                    notifyItemChanged(position);
                    activity.onStickerUnlocked(sticker.imageFile);
                }
            } else {
                // Ya estaba desbloqueado, abrimos la vista previa gigante
                activity.mostrarPreviewGrande(stickerUrl);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pack.stickers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSticker;
        View badge;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSticker = itemView.findViewById(R.id.sticker_image);
            badge = itemView.findViewById(R.id.layoutRewardBadge);
            if (badge != null) badge.setVisibility(View.GONE);
        }
    }
}