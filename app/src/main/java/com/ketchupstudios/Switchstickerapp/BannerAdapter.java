package com.ketchupstudios.Switchstickerapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final List<Config.Banner> banners;

    public BannerAdapter(List<Config.Banner> banners) {
        this.banners = banners;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Config.Banner banner = banners.get(position);

        // Cargar imagen en segundo plano
        new Thread(() -> {
            try {
                // Construir URL completa
                String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
                URL url = new URL(baseUrl + banner.imageFile);
                InputStream inputStream = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                holder.imageView.post(() -> holder.imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Clic para abrir el enlace
        holder.imageView.setOnClickListener(v -> {
            if (banner.linkUrl != null && !banner.linkUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(banner.linkUrl));
                v.getContext().startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgBanner);
        }
    }
}