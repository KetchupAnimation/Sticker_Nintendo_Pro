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

public class GachaUnboxAdapter extends RecyclerView.Adapter<GachaUnboxAdapter.ViewHolder> {

    private Context context;
    private StickerPack pack;
    private GachaUnboxActivity activity;
    public boolean[] isUnlocked;

    public GachaUnboxAdapter(Context context, StickerPack pack, GachaUnboxActivity activity) {
        this.context = context;
        this.pack = pack;
        this.activity = activity;
        this.isUnlocked = new boolean[pack.stickers.size()];
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Asegúrate de que el padre tampoco recorte la animación
        parent.setClipChildren(false);
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker_single, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack.Sticker sticker = pack.stickers.get(position);

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        final String stickerUrl = baseUrl + pack.identifier + "/" + sticker.imageFile;

        // 👇 USAMOS fitCenter() PARA QUE EL STICKER SE VEA ÍNTEGRO SIN TOCAR LOS BORDES 👇
        Glide.with(context)
                .load(stickerUrl)
                .fitCenter()
                .into(holder.imgSticker);

        // Cargamos la imagen asegurando que no haya transformaciones raras que la recorten
        Glide.with(context)
                .load(stickerUrl)
                .centerInside() // Ayuda a mantener el sticker íntegro dentro del ImageView
                .into(holder.imgSticker);

        if (isUnlocked[position]) {
            holder.imgSticker.setColorFilter(null);
            holder.imgSticker.setAlpha(1.0f);
        } else {
            // La silueta ahora será un gris más suave para que se noten mejor las formas sin recortar
            holder.imgSticker.setColorFilter(Color.parseColor("#444444"), PorterDuff.Mode.SRC_IN);
            holder.imgSticker.setAlpha(0.6f);
        }

        // CLICK NORMAL: Desbloquear con animación "Pop"
        holder.itemView.setOnClickListener(v -> {
            if (!isUnlocked[position] && activity.unlockedCount < 3) {
                isUnlocked[position] = true;

                holder.imgSticker.setScaleX(0.5f);
                holder.imgSticker.setScaleY(0.5f);
                // La animación ahora debería salirse del cuadro gracias al paso 2
                holder.imgSticker.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction(() -> {
                    holder.imgSticker.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }).start();

                notifyItemChanged(position);
                activity.onStickerUnlocked();
            }
        });

        // 👇 CORREGIDO: Llamamos al método de la actividad en lugar de la clase inexistente 👇
        holder.itemView.setOnLongClickListener(v -> {
            activity.mostrarPreviewGrande(stickerUrl);
            return true;
        });
    }

    @Override
    public int getItemCount() { return pack.stickers.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSticker;
        View badge;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Aseguramos que el item individual tampoco recorte
            if (itemView instanceof ViewGroup) {
                ((ViewGroup) itemView).setClipChildren(false);
            }
            imgSticker = itemView.findViewById(R.id.sticker_image);
            badge = itemView.findViewById(R.id.layoutRewardBadge);
            if (badge != null) badge.setVisibility(View.GONE);
        }
    }
}