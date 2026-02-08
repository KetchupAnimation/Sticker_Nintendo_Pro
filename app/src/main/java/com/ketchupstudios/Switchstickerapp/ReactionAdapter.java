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

public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ViewHolder> {

    private List<Reaction> reactions;
    private Context context;
    private OnReactionClickListener listener;

    public interface OnReactionClickListener {
        void onReactionClick(Reaction reaction);
    }

    public ReactionAdapter(Context context, List<Reaction> reactions, OnReactionClickListener listener) {
        this.context = context;
        this.reactions = reactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usamos un layout simple de imagen cuadrada
        View view = LayoutInflater.from(context).inflate(R.layout.item_reaction_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reaction r = reactions.get(position);

        // Base URL donde tienes guardados los stickers
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + "Widget/Reactions/";

        Glide.with(context)
                .load(baseUrl + r.image)
                .into(holder.imgIcon);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(r);
        });
    }

    @Override
    public int getItemCount() { return reactions.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgReactionIcon);
        }
    }
}