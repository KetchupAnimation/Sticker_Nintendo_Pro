package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class GameSearchAdapter extends RecyclerView.Adapter<GameSearchAdapter.ViewHolder> {

    private List<GameItem> games;
    private Context context;
    private OnGameClickListener listener;

    public interface OnGameClickListener {
        void onGameClick(GameItem game);
    }

    public GameSearchAdapter(Context context, List<GameItem> games, OnGameClickListener listener) {
        this.context = context;
        this.games = games;
        this.listener = listener;
    }

    public void updateList(List<GameItem> newGames) {
        this.games = newGames;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameItem game = games.get(position);

        // Borramos cualquier imagen previa del recycled view
        Glide.with(context).clear(holder.imgIcon);

        String iconUrl = "https://api.nlib.cc/nx/" + game.id + "/icon";

        Glide.with(context)
                .load(iconUrl)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            com.bumptech.glide.load.engine.GlideException e,
                            Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource) {

                        // 🧨 Si no carga, removemos el item de la lista
                        int adapterPosition = holder.getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            games.remove(adapterPosition);
                            notifyItemRemoved(adapterPosition);
                        }


                        return true; // true = manejamos el error (no intentará poner placeholder)
                    }

                    @Override
                    public boolean onResourceReady(
                            android.graphics.drawable.Drawable resource,
                            Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            com.bumptech.glide.load.DataSource dataSource,
                            boolean isFirstResource) {
                        return false; // false = deja que Glide muestre la imagen normalmente
                    }
                })
                .into(holder.imgIcon);

        holder.itemView.setOnClickListener(v -> listener.onGameClick(game));
    }


    @Override
    public int getItemCount() {
        return games.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
       // TextView txtName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgGameIcon);
            //txtName = itemView.findViewById(R.id.lblGameTitle);
        }
    }
}