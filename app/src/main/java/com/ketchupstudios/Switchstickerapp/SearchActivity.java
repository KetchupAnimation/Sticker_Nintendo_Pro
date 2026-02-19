package com.ketchupstudios.Switchstickerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private RecyclerView rvResults;
    private SearchAdapter adapter;
    private List<Object> allItems = new ArrayList<>();

    // Variables para Anuncios
    private RewardedAd mRewardedAd;
    private boolean recompensaGanada = false;
    private Runnable accionPendiente;

    // Diálogo de carga
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        setContentView(R.layout.activity_search);

        SearchView searchView = findViewById(R.id.searchView);
        rvResults = findViewById(R.id.rvSearchResults);
        ImageView imgBanner = findViewById(R.id.imgSearchBanner);

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        String bannerUrl = baseUrl + "banner_8.png";
        Glide.with(this).load(bannerUrl).centerCrop().placeholder(android.R.color.darker_gray).into(imgBanner);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        rvResults.setLayoutManager(layoutManager);

        cargarDatosGlobales();
        List<Object> listaInicial = generarVistaPreviaRellena();

        adapter = new SearchAdapter(this, listaInicial, this::manejarClicItem);
        rvResults.setAdapter(adapter);

        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filtrar(query); return false; }
            @Override public boolean onQueryTextChange(String newText) { filtrar(newText); return false; }
        });
        searchView.setOnCloseListener(() -> { finish(); return false; });
    }

    private void manejarClicItem(Object item) {
        // SEGURIDAD: Try-Catch para evitar cierres inesperados en la lógica principal
        try {
            boolean requierePago = (item instanceof StickerPack) ||
                    (item instanceof Config.Wallpaper && ((Config.Wallpaper) item).isPremium);

            if (requierePago) {
                SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
                int tickets = prefs.getInt("skip_tickets", 0);
                final int COSTO = 3;

                if (tickets >= COSTO) {
                    mostrarDialogoGastarMonedas("Unlock Item?", COSTO, tickets, () -> {
                        prefs.edit().putInt("skip_tickets", tickets - COSTO).apply();
                        CustomToast.makeText(this, "Unlocked with Coins! \uD83D\uDD13", Toast.LENGTH_SHORT).show();
                        abrirDetalles(item);
                    }, () -> {
                        cargarAnuncioYEjecutar(() -> abrirDetalles(item));
                    });
                } else {
                    cargarAnuncioYEjecutar(() -> abrirDetalles(item));
                }
            } else {
                abrirDetalles(item);
            }
        } catch (Exception e) {
            // Si algo falla, abrimos el detalle directamente para no cerrar la app
            abrirDetalles(item);
        }
    }

    private void abrirDetalles(Object item) {
        try {
            if (item instanceof StickerPack) {
                Config.selectedPack = (StickerPack) item;
                startActivity(new Intent(this, StickerDetailsActivity.class));
            } else if (item instanceof Config.Wallpaper) {
                Config.Wallpaper w = (Config.Wallpaper) item;
                Intent intent = new Intent(this, WallpaperDetailsActivity.class);
                intent.putExtra("wall_name", w.name);
                intent.putExtra("wall_author", w.publisher);
                intent.putExtra("wall_image", w.imageFile);
                intent.putExtra("wall_link", w.artistLink);
                intent.putExtra("wall_color", w.colorBg);
                startActivity(intent);
            }
        } catch (Exception e) {
            CustomToast.makeText(this, "Error opening item", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoGastarMonedas(String title, int cost, int balance, Runnable onUseCoins, Runnable onWatchAd) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_spend_coins, null);
            builder.setView(view);
            AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            TextView txtTitle = view.findViewById(R.id.txtDialogTitle);
            TextView txtMsg = view.findViewById(R.id.txtDialogMessage);
            TextView txtBal = view.findViewById(R.id.txtCurrentBalance);
            View btnUse = view.findViewById(R.id.btnUseCoins);
            View btnAd = view.findViewById(R.id.btnWatchAd);

            txtTitle.setText(title);
            txtMsg.setText("Use " + cost + " coins to unlock this item instantly.");
            txtBal.setText("Balance: " + balance + " coins");

            if (btnUse instanceof Button) {
                ((Button) btnUse).setText("USE " + cost + " COINS");
            }

            btnUse.setOnClickListener(v -> {
                dialog.dismiss();

                // --- INICIO CÓDIGO SENSORIAL ---
                try {
                    // 1. SONIDO
                    android.media.MediaPlayer mp = android.media.MediaPlayer.create(this, R.raw.coin);
                    if (mp != null) {
                        mp.setOnCompletionListener(android.media.MediaPlayer::release);
                        mp.start();
                    }
                    // 2. VIBRACIÓN
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(50);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // --------------------------------

                if (onUseCoins != null) onUseCoins.run();
            });
            btnAd.setOnClickListener(v -> { dialog.dismiss(); if (onWatchAd != null) onWatchAd.run(); });

            dialog.show();
        } catch (Exception e) {
            // Si falla el diálogo (por tema o recursos), vamos directo a la opción segura: Anuncio
            if (onWatchAd != null) onWatchAd.run();
        }
    }

    private void cargarAnuncioYEjecutar(Runnable accion) {
        accionPendiente = accion;

        // DIÁLOGO DE CARGA SEGURO EN TRY-CATCH
        try {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);

            // Inflamos el XML simple que creaste
            View view = getLayoutInflater().inflate(R.layout.dialog_loading_simple, null);
            builder.setView(view);

            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            loadingDialog.show();
        } catch (Exception e) {
            // Si falla la UI de carga, seguimos con el anuncio sin mostrar diálogo
        }

        String idVideoReal = "ca-app-pub-9087203932210009/4573750306";
        AdRequest adRequest = new AdRequest.Builder().build();

        try {
            RewardedAd.load(this, idVideoReal, adRequest, new RewardedAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
                    if (accionPendiente != null) accionPendiente.run();
                }
                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                    if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();

                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mRewardedAd = null;
                            if (recompensaGanada && accionPendiente != null) accionPendiente.run();
                            recompensaGanada = false;
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                            mRewardedAd = null;
                            if (accionPendiente != null) accionPendiente.run();
                        }
                    });
                    recompensaGanada = false;
                    mRewardedAd.show(SearchActivity.this, rewardItem -> recompensaGanada = true);
                }
            });
        } catch (Exception e) {
            // Falla catastrófica de AdMob: Abrir contenido
            if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
            if (accionPendiente != null) accionPendiente.run();
        }
    }

    private void cargarDatosGlobales() {
        allItems.clear();
        if (Config.packs != null) allItems.addAll(Config.packs);
        if (Config.wallpapers != null) allItems.addAll(Config.wallpapers);
    }

    private boolean debeMostrarPack(StickerPack p) {
        if (p.isEvent) return false;
        return true;
    }

    private boolean debeMostrarWallpaper(Config.Wallpaper w) {
        if (w.isLimitedTime && w.isExpired) return false;
        if (w.isHidden) return false;
        return true;
    }

    private List<Object> generarVistaPreviaRellena() {
        List<StickerPack> stickersNuevos = new ArrayList<>();
        List<StickerPack> stickersNormales = new ArrayList<>();
        List<Config.Wallpaper> wallNuevos = new ArrayList<>();
        List<Config.Wallpaper> wallNormales = new ArrayList<>();

        if (Config.packs != null) {
            for (StickerPack p : Config.packs) {
                if (!debeMostrarPack(p)) continue;
                if (p.status != null && (p.status.equalsIgnoreCase("new") || p.status.equalsIgnoreCase("updated"))) {
                    stickersNuevos.add(p);
                } else {
                    stickersNormales.add(p);
                }
            }
        }

        if (Config.wallpapers != null) {
            for (Config.Wallpaper w : Config.wallpapers) {
                if (!debeMostrarWallpaper(w)) continue;
                if (w.isNew) wallNuevos.add(w);
                else wallNormales.add(w);
            }
        }

        Collections.shuffle(stickersNormales);
        Collections.shuffle(wallNormales);

        List<Object> vistaPrevia = new ArrayList<>();
        int countS = 0;
        for (StickerPack p : stickersNuevos) { if (countS < 6) { vistaPrevia.add(p); countS++; } }
        for (StickerPack p : stickersNormales) { if (countS < 6) { vistaPrevia.add(p); countS++; } }

        int countW = 0;
        for (Config.Wallpaper w : wallNuevos) { if (countW < 6) { vistaPrevia.add(w); countW++; } }
        for (Config.Wallpaper w : wallNormales) { if (countW < 6) { vistaPrevia.add(w); countW++; } }

        return vistaPrevia;
    }

    private void filtrar(String texto) {
        String query = texto.toLowerCase().trim();
        List<Object> listaFiltrada = new ArrayList<>();

        if (Config.packs != null) {
            for (StickerPack pack : Config.packs) {
                if (!debeMostrarPack(pack)) continue;
                boolean match = false;
                if (pack.name != null && pack.name.toLowerCase().contains(query)) match = true;
                else if (pack.publisher != null && pack.publisher.toLowerCase().contains(query)) match = true;
                else if (pack.tags != null) {
                    for (String tag : pack.tags) {
                        if (tag.toLowerCase().contains(query)) { match = true; break; }
                    }
                }
                if (match) listaFiltrada.add(pack);
            }
        }

        if (Config.wallpapers != null) {
            for (Config.Wallpaper wall : Config.wallpapers) {
                if (!debeMostrarWallpaper(wall)) continue;
                boolean match = false;
                if (wall.name != null && wall.name.toLowerCase().contains(query)) match = true;
                else if (wall.publisher != null && wall.publisher.toLowerCase().contains(query)) match = true;
                else if (wall.tags != null) {
                    for (String tag : wall.tags) {
                        if (tag.toLowerCase().contains(query)) { match = true; break; }
                    }
                }
                if (match) listaFiltrada.add(wall);
            }
        }

        if (adapter != null) adapter.updateList(listaFiltrada);
    }

    @Override public void finish() { super.finish(); overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down); }
    @Override protected void onResume() { super.onResume(); if (adapter != null) adapter.notifyDataSetChanged(); }
}