package com.ketchupstudios.Switchstickerapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.bumptech.glide.Glide;

public class FullListActivity extends AppCompatActivity {

    public static final String TYPE_STICKERS = "stickers";
    public static final String TYPE_WALLPAPERS = "wallpapers";
    public static final String TYPE_PREMIUM = "premium";
    public static final String TYPE_WIDGETS = "widgets";
    public static final String TYPE_BATTERY = "battery";

    private ViewPager2 bannerViewPager;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private RecyclerView rvList;
    private TextView txtTitle;
    private String currentType;

    // VARIABLES PARA NUESTRO BOTÓN MANUAL
    private CardView btnCleanCacheContainer;
    private TextView txtCleanCacheText;
    private ImageView imgCleanCacheIcon;

    // VARIABLES DE ERROR DE CONEXIÓN
    private LinearLayout layoutConnectionError;
    private Button btnRetryConnection;

    private Map<Integer, String> widgetStatusMap = new HashMap<>();
    private int totalWidgetsCount = 20;

    // --- VARIABLES DE PUBLICIDAD REWARDED (STICKERS) ---
    private RewardedAd mRewardedAd;
    private boolean recompensaGanada = false;
    private Runnable accionPendiente;

    // --- VARIABLES INTERSTICIAL (WALLPAPERS) ---
    private InterstitialAd mInterstitialAd;
    private int wallpaperClickCount = 0;
    private final int WALLPAPER_ADS_THRESHOLD = 3; // Mostrar anuncio cada 3 clicks

    private boolean isCountingDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_list);

        // INICIALIZAR PUBLICIDAD
        MobileAds.initialize(this, initializationStatus -> {
            cargarAnuncioIntersticial(); // Cargar anuncio rápido al iniciar
        });

        currentType = getIntent().getStringExtra("TYPE");

        txtTitle = findViewById(R.id.txtCategoryTitle);
        rvList = findViewById(R.id.rvFullList);
        bannerViewPager = findViewById(R.id.bannerViewPagerFull);

        btnCleanCacheContainer = findViewById(R.id.cardCleanCache);
        txtCleanCacheText = findViewById(R.id.txtCacheText);
        imgCleanCacheIcon = findViewById(R.id.imgCacheIcon);

        layoutConnectionError = findViewById(R.id.layoutConnectionError);
        btnRetryConnection = findViewById(R.id.btnRetryConnection);

        btnRetryConnection.setOnClickListener(v -> {
            layoutConnectionError.setVisibility(View.GONE);
            Toast.makeText(this, "Retrying...", Toast.LENGTH_SHORT).show();
            new Thread(this::descargarJSON).start();
        });

        AdView adView = findViewById(R.id.adViewFull);
        if (adView != null) adView.loadAd(new AdRequest.Builder().build());

        // LÓGICA DEL BOTÓN CLEAN CACHE
        if (TYPE_WIDGETS.equals(currentType)) {
            btnCleanCacheContainer.setVisibility(View.VISIBLE);
            btnCleanCacheContainer.setOnClickListener(v -> {
                if (isCountingDown) return;
                isCountingDown = true;
                new CountDownTimer(4000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        txtCleanCacheText.setText("Wait " + millisUntilFinished / 1000 + "s");
                    }
                    public void onFinish() {
                        txtCleanCacheText.setText("Clear Cache");
                        imgCleanCacheIcon.setVisibility(View.VISIBLE);
                        isCountingDown = false;
                        cargarAnuncioYEjecutar(() -> limpiarCacheYProtegerFavoritos());
                    }
                }.start();
            });
        } else {
            btnCleanCacheContainer.setVisibility(View.GONE);
        }

        // BANNERS
        List<Config.Banner> sectionBanners = new ArrayList<>();
        if (TYPE_STICKERS.equals(currentType)) {
            sectionBanners.add(new Config.Banner("banner_4.png", ""));
        } else if (TYPE_WALLPAPERS.equals(currentType)) {
            sectionBanners.add(new Config.Banner("banner_5.png", ""));
        } else if (TYPE_PREMIUM.equals(currentType)) {
            sectionBanners.add(new Config.Banner("banner_6.png", ""));
        } else if (TYPE_WIDGETS.equals(currentType)) {
            sectionBanners.add(new Config.Banner("banner_7.png", ""));
        } else if (TYPE_BATTERY.equals(currentType)) {
            sectionBanners.add(new Config.Banner("banner_9.png", ""));
        } else {
            if (!Config.banners.isEmpty()) sectionBanners.addAll(Config.banners);
        }

        if (!sectionBanners.isEmpty()) {
            BannerAdapter bannerAdapter = new BannerAdapter(sectionBanners);
            bannerViewPager.setAdapter(bannerAdapter);
            bannerViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
            if (sectionBanners.size() > 1) {
                sliderHandler.postDelayed(sliderRunnable, 4000);
            }
        }

        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
        new Thread(this::descargarJSON).start();
    }

    // =================================================================
    // LÓGICA DE PUBLICIDAD & MONEDAS (ACTUALIZADA)
    // =================================================================

    // 1. REWARDED (OBLIGATORIO PARA STICKERS)
    public void cargarAnuncioYEjecutar(Runnable accion) {
        accionPendiente = accion;
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Loading Ad...");
        pd.setCancelable(false);
        pd.show();

        String idVideoReal = "ca-app-pub-9087203932210009/4573750306";
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, idVideoReal, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                pd.dismiss();
                if (accionPendiente != null) accionPendiente.run();
            }
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                pd.dismiss();
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mRewardedAd = null;
                        if (recompensaGanada && accionPendiente != null) {
                            accionPendiente.run();
                        }
                        recompensaGanada = false;
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        mRewardedAd = null;
                        if (accionPendiente != null) accionPendiente.run();
                    }
                });
                recompensaGanada = false;
                mRewardedAd.show(FullListActivity.this, rewardItem -> recompensaGanada = true);
            }
        });
    }

    // 2. INTERSTITIAL (FRECUENCIA PARA WALLPAPERS)
    private void cargarAnuncioIntersticial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, Config.ADMOB_INTERSTITIAL_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    // ===============================================================
    // CEREBRO CENTRAL DE WALLPAPERS (NUEVO - REQUERIDO POR ADAPTER)
    // ===============================================================

    public void analizarClickWallpaper(Config.Wallpaper wall) {
        if (wall.isPremium) {
            procesarWallpaperPremium(wall);
        } else {
            procesarWallpaperNormal(wall);
        }
    }

    private void procesarWallpaperPremium(Config.Wallpaper wall) {
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COSTO = 3;

        if (tickets >= COSTO) {
            mostrarDialogoGastarMonedas("Unlock Premium?", COSTO, tickets, () -> {
                prefs.edit().putInt("skip_tickets", tickets - COSTO).apply();
                Toast.makeText(this, "Premium Unlocked! 💎", Toast.LENGTH_SHORT).show();
                abrirWallpaperDetalles(wall);
            }, () -> {
                cargarAnuncioYEjecutar(() -> abrirWallpaperDetalles(wall));
            });
        } else {
            cargarAnuncioYEjecutar(() -> abrirWallpaperDetalles(wall));
        }
    }

    // ===============================================================
    // LÓGICA CORREGIDA: 3 CLICS + OPCIÓN DE GASTAR MONEDAS
    // ===============================================================

    private void procesarWallpaperNormal(Config.Wallpaper wall) {
        wallpaperClickCount++; // Sumamos un clic

        // ¿Llegamos al límite de 3 clics? -> TOCA ANUNCIO
        if (wallpaperClickCount >= WALLPAPER_ADS_THRESHOLD) {

            // 1. VERIFICAMOS SALDO ANTES DE LANZAR EL ANUNCIO
            android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
            int tickets = prefs.getInt("skip_tickets", 0);
            final int COSTO = 3;

            if (tickets >= COSTO) {
                // CASO A: TIENE MONEDAS -> LE PREGUNTAMOS
                mostrarDialogoGastarMonedas("Skip Ad?", COSTO, tickets, () -> {
                    // Eligió GASTAR monedas
                    prefs.edit().putInt("skip_tickets", tickets - COSTO).apply();
                    Toast.makeText(this, "Ad Skipped! ⚡", Toast.LENGTH_SHORT).show();

                    // Reiniciamos contador y abrimos SIN anuncio
                    wallpaperClickCount = 0;
                    abrirWallpaperDetalles(wall);

                }, () -> {
                    // Eligió VER el anuncio (o cerró el diálogo)
                    lanzarIntersticial(wall);
                });
            } else {
                // CASO B: NO TIENE MONEDAS -> ANUNCIO OBLIGATORIO
                lanzarIntersticial(wall);
            }

        } else {
            // NO TOCA ANUNCIO (Clic 1 o 2) -> PASE LIBRE
            abrirWallpaperDetalles(wall);
            // Cargar el siguiente anuncio por si acaso
            if (mInterstitialAd == null) cargarAnuncioIntersticial();
        }
    }

    // Método auxiliar para lanzar el Intersticial y manejar el contador
    private void lanzarIntersticial(Config.Wallpaper wall) {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    wallpaperClickCount = 0; // ¡IMPORTANTE! Reiniciar contador al cerrar anuncio
                    cargarAnuncioIntersticial();
                    abrirWallpaperDetalles(wall);
                }
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mInterstitialAd = null;
                    abrirWallpaperDetalles(wall); // Si falla, abrimos igual
                }
            });
        } else {
            // Si el anuncio no estaba listo, abrimos y NO reiniciamos el contador
            // (para intentar mostrarlo en el siguiente clic)
            abrirWallpaperDetalles(wall);
            cargarAnuncioIntersticial();
        }
    }

    // --- MÉTODOS DE COMPATIBILIDAD ---
    // (Por si acaso el adaptador antiguo sigue llamando a estos nombres)
    public void intentarAbrirWallpaperConIntersticial(Config.Wallpaper wall) {
        analizarClickWallpaper(wall);
    }
    public void intentarAbrirWallpaperPremium(Config.Wallpaper wall) {
        analizarClickWallpaper(wall);
    }

    // --- NUEVO: MÉTODO CON LÓGICA DE MONEDAS PARA STICKERS ---
    public void intentarAbrirPackPremium(StickerPack pack) {
        Config.selectedPack = pack;
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COST = 3;

        if (tickets >= COST) {
            mostrarDialogoGastarMonedas("Unlock Pack?", COST, tickets, () -> {
                prefs.edit().putInt("skip_tickets", tickets - COST).apply();
                Toast.makeText(this, "Pack Unlocked! 🔓", Toast.LENGTH_SHORT).show();
                abrirPantallaDetalles(pack);
            }, () -> {
                cargarAnuncioYEjecutar(() -> abrirPantallaDetalles(pack));
            });
        } else {
            cargarAnuncioYEjecutar(() -> abrirPantallaDetalles(pack));
        }
    }

    // =================================================================
    // CARGA DE DATOS Y VISTA
    // =================================================================

    private void cargarListaSegunTipo() {
        if (layoutConnectionError != null) layoutConnectionError.setVisibility(View.GONE);

        if (TYPE_STICKERS.equals(currentType)) {
            txtTitle.setText("All Stickers");
            rvList.setLayoutManager(new GridLayoutManager(this, 3));
            List<StickerPack> gratisList = new ArrayList<>();

            if (Config.packs != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date today = new Date();

                for (StickerPack p : Config.packs) {
                    if (p.isEvent) {
                        try {
                            Date startDate = sdf.parse(p.eventStartDate);
                            long diff = today.getTime() - startDate.getTime();
                            long daysPassed = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
                            if (daysPassed < p.stickers.size()) {
                                continue;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    gratisList.add(p);
                }
            }

            List<StickerPack> prioritarios = new ArrayList<>();
            List<StickerPack> normales = new ArrayList<>();
            for (StickerPack p : gratisList) {
                if ("new".equalsIgnoreCase(p.status) || "updated".equalsIgnoreCase(p.status)) prioritarios.add(p);
                else normales.add(p);
            }
            prioritarios.addAll(normales);
            rvList.setAdapter(new StickerPackAdapter(prioritarios, this, R.layout.item_sticker_grid));

        } else if (TYPE_WALLPAPERS.equals(currentType)) {
            txtTitle.setText("Wallpapers Gallery");
            rvList.setLayoutManager(new GridLayoutManager(this, 3));
            List<Config.Wallpaper> gratisList = new ArrayList<>();
            if (Config.wallpapers != null) {
                for (Config.Wallpaper w : Config.wallpapers) gratisList.add(w);
            }
            List<Config.Wallpaper> listaOrdenada = ordenarWallpapers(gratisList);
            rvList.setAdapter(new WallpaperAdapter(listaOrdenada, R.layout.item_wallpaper));

        } else if (TYPE_PREMIUM.equals(currentType)) {
            txtTitle.setText("Premium Zone");
            rvList.setLayoutManager(new GridLayoutManager(this, 3));
            List<StickerPack> pStickers = new ArrayList<>();
            List<Config.Wallpaper> pWalls = new ArrayList<>();
            if(Config.packs != null) for(StickerPack p : Config.packs) if(p.isPremium) pStickers.add(p);
            if(Config.wallpapers != null) for(Config.Wallpaper w : Config.wallpapers) if(w.isPremium) pWalls.add(w);

            List<PremiumItem> listaFinal = new ArrayList<>();
            for (StickerPack s : pStickers) listaFinal.add(new PremiumItem(s));
            for (Config.Wallpaper w : pWalls) listaFinal.add(new PremiumItem(w));
            rvList.setAdapter(new PremiumAdapter(listaFinal, this));

        } else if (TYPE_WIDGETS.equals(currentType)) {
            txtTitle.setText("Widget Gallery");
            rvList.setLayoutManager(new GridLayoutManager(this, 2));

            android.content.SharedPreferences wPrefs = getSharedPreferences("WidgetPrefs", MODE_PRIVATE);
            int totalCount = Math.max(wPrefs.getInt("count_4x2", 20), totalWidgetsCount);
            Set<String> favSet = wPrefs.getStringSet("fav_wallpapers", new HashSet<>());

            List<Integer> listLimited = new ArrayList<>();
            List<Integer> listPriority = new ArrayList<>();
            List<Integer> listFavs = new ArrayList<>();
            List<Integer> listNormal = new ArrayList<>();

            for (int i = 1; i <= totalCount; i++) {
                String status = widgetStatusMap.get(i);
                boolean isFav = favSet.contains(String.valueOf(i));

                if (status != null && status.equalsIgnoreCase("limited")) {
                    listLimited.add(i);
                } else if (status != null && (status.equalsIgnoreCase("new") || status.equalsIgnoreCase("updated"))) {
                    listPriority.add(i);
                } else if (isFav) {
                    listFavs.add(i);
                } else {
                    listNormal.add(i);
                }
            }
            Collections.sort(listLimited, Collections.reverseOrder());
            Collections.sort(listPriority, Collections.reverseOrder());

            List<Integer> finalList = new ArrayList<>();
            finalList.addAll(listLimited);
            finalList.addAll(listPriority);
            finalList.addAll(listFavs);
            finalList.addAll(listNormal);

            rvList.setAdapter(new WidgetGalleryAdapter(this, finalList, false, widgetStatusMap));

        } else if (TYPE_BATTERY.equals(currentType)) {
            txtTitle.setText("Battery Themes");
            rvList.setLayoutManager(new GridLayoutManager(this, 3));

            List<Config.BatteryTheme> batLim = new ArrayList<>();
            List<Config.BatteryTheme> batNew = new ArrayList<>();
            List<Config.BatteryTheme> batNormal = new ArrayList<>();

            for (Config.BatteryTheme t : Config.batteryThemes) {
                if (t.isLimitedTime) {
                    if (!t.isExpired) batLim.add(t);
                }
                else if (t.isNew) batNew.add(t);
                else batNormal.add(t);
            }

            List<Config.BatteryTheme> batFinal = new ArrayList<>(batLim);
            batFinal.addAll(batNew);
            batFinal.addAll(batNormal);

            rvList.setAdapter(new BatteryThemeAdapter(this, batFinal, R.layout.item_battery_theme, theme -> {
                Intent intent = new Intent(FullListActivity.this, BatteryPreviewActivity.class);
                intent.putExtra("THEME_ID", theme.id);
                startActivity(intent);
            }));
        }
    }

    private List<Config.Wallpaper> ordenarWallpapers(List<Config.Wallpaper> original) {
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favSet = prefs.getStringSet("fav_wallpapers_ids", new HashSet<>());

        List<Config.Wallpaper> limitados = new ArrayList<>();
        List<Config.Wallpaper> nuevos = new ArrayList<>();
        List<Config.Wallpaper> favoritos = new ArrayList<>();
        List<Config.Wallpaper> normales = new ArrayList<>();

        if (original != null) {
            for (Config.Wallpaper w : original) {
                if (w.isHidden) continue;
                if (w.isLimitedTime && w.isExpired) continue;
                if (w.isLimitedTime) {
                    if (!w.isExpired) limitados.add(w);
                } else if (w.isNew) {
                    nuevos.add(w);
                } else if (favSet.contains(w.imageFile)) {
                    favoritos.add(w);
                } else {
                    normales.add(w);
                }
            }
        }
        List<Config.Wallpaper> resultado = new ArrayList<>();
        resultado.addAll(limitados);
        resultado.addAll(nuevos);
        resultado.addAll(favoritos);
        resultado.addAll(normales);
        return resultado;
    }

    private void descargarJSON() {
        try {
            if (TYPE_BATTERY.equals(currentType)) {
                String batteryJsonUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
                URL url = new URL(batteryJsonUrl + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject json = new JSONObject(result.toString());
                JSONArray array = json.getJSONArray("themes");

                Config.batteryThemes.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);

                    Config.batteryThemes.add(new Config.BatteryTheme(
                            o.getString("id"),
                            o.getString("name"),
                            o.getString("folder"),
                            o.getString("color_bg"),
                            o.getString("text_color"),
                            o.optBoolean("is_new", false),
                            o.optString("artist_link", "")
                    ));
                }

            } else {
                URL url = new URL(Config.STICKER_JSON_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject json = new JSONObject(result.toString());

                // LIMITED EVENTS PARSING
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = sdf.format(new Date());
                Set<String> limitedWallIds = new HashSet<>();

                if (json.has("limited_events")) {
                    JSONArray events = json.getJSONArray("limited_events");
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        String date = event.getString("date");
                        String type = event.getString("type");
                        JSONObject d = event.getJSONObject("data");

                        boolean isToday = date.equals(today);

                        if (type.equals("widget") && isToday) {
                            // Lógica Widgets se mantiene igual
                            try {
                                int id = d.getInt("id");
                                widgetStatusMap.put(id, "limited");
                                if(id > totalWidgetsCount) totalWidgetsCount = id;
                            } catch (Exception e){}
                        }
                        else if (type.equals("wallpaper")) {
                            Config.Wallpaper w = new Config.Wallpaper(
                                    d.getString("identifier"), d.getString("name"), d.getString("image_file"),
                                    d.getString("publisher"), false, false, d.optString("artist_link", "")
                            );
                            w.colorBg = d.optString("ColorBG", "#FFFFFF");
                            w.isLimitedTime = true;
                            w.isExpired = !isToday;

                            Config.wallpapers.add(w);
                            limitedWallIds.add(d.getString("image_file"));
                        }
                    }
                }

                Config.banners.clear();
                if (json.has("banners")) {
                    JSONArray bannersArray = json.getJSONArray("banners");
                    for (int i = 0; i < bannersArray.length(); i++) {
                        JSONObject b = bannersArray.getJSONObject(i);
                        Config.banners.add(new Config.Banner(b.optString("image_file"), b.optString("link_url")));
                    }
                }

                JSONArray packsArray = null;
                if (json.has("sticker_packs")) packsArray = json.getJSONArray("sticker_packs");
                else if (json.has("android")) packsArray = json.getJSONArray("android");

                Config.packs.clear();
                if (packsArray != null) {
                    for (int i = 0; i < packsArray.length(); i++) {
                        JSONObject obj = packsArray.getJSONObject(i);
                        StickerPack pack = new StickerPack();
                        pack.identifier = obj.getString("identifier");
                        pack.name = obj.getString("name");
                        pack.publisher = obj.getString("publisher");
                        pack.trayImageFile = obj.getString("tray_image_file");
                        pack.status = obj.optString("status");
                        pack.isPremium = obj.optBoolean("is_premium", false);
                        pack.artistLink = obj.optString("artist_link", "");
                        pack.updateNoteImage = obj.optString("update_note_image");
                        pack.tags = new ArrayList<>();
                        JSONArray tagsArray = obj.optJSONArray("tags");
                        if (tagsArray != null) {
                            for (int k = 0; k < tagsArray.length(); k++) pack.tags.add(tagsArray.getString(k));
                        }
                        JSONArray stickersArray = obj.getJSONArray("stickers");
                        pack.stickers = new ArrayList<>();
                        for (int j = 0; j < stickersArray.length(); j++) {
                            JSONObject sObj = stickersArray.getJSONObject(j);
                            StickerPack.Sticker s = new StickerPack.Sticker();
                            s.imageFile = sObj.getString("image_file");
                            pack.stickers.add(s);
                        }

                        if (obj.has("start_date")) {
                            pack.isEvent = true;
                            pack.eventStartDate = obj.getString("start_date");
                            pack.totalDays = obj.optInt("total_days", 0);
                            pack.eventBanner = obj.optString("banner_image", "");
                        }

                        Config.packs.add(pack);
                    }
                }

                Config.wallpapers.clear();

                if (json.has("limited_events")) {
                    JSONArray events = json.getJSONArray("limited_events");
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        if (event.getString("date").equals(today) && event.getString("type").equals("wallpaper")) {
                            JSONObject d = event.getJSONObject("data");
                            Config.Wallpaper w = new Config.Wallpaper(
                                    d.getString("identifier"), d.getString("name"), d.getString("image_file"),
                                    d.getString("publisher"), false, false, d.optString("artist_link", "")
                            );
                            w.colorBg = d.optString("ColorBG", "#FFFFFF");
                            w.isLimitedTime = true;
                            Config.wallpapers.add(w);
                        }
                    }
                }

                if (json.has("wallpapers")) {
                    JSONArray wallArray = json.getJSONArray("wallpapers");
                    for (int i = 0; i < wallArray.length(); i++) {
                        JSONObject w = wallArray.getJSONObject(i);
                        String imgFile = w.getString("image_file");
                        if (limitedWallIds.contains(imgFile)) continue;

                        Config.Wallpaper wall = new Config.Wallpaper(
                                w.getString("identifier"), w.getString("name"), imgFile,
                                w.getString("publisher"), w.optBoolean("is_new", false), w.optBoolean("is_premium", false),
                                w.optString("artist_link", "")
                        );
                        wall.tags = new ArrayList<>();
                        JSONArray tagsArray = w.optJSONArray("tags");
                        if (tagsArray != null) {
                            for (int k = 0; k < tagsArray.length(); k++) wall.tags.add(tagsArray.getString(k));
                        }
                        wall.colorBg = w.optString("ColorBG", "#FFFFFF");
                        wall.isHidden = w.optBoolean("is_hidden", false);
                        Config.wallpapers.add(wall);
                    }
                }

                int widgetCount = json.optInt("4x2", 20);
                if(totalWidgetsCount > widgetCount) widgetCount = totalWidgetsCount;

                getSharedPreferences("WidgetPrefs", MODE_PRIVATE).edit().putInt("count_4x2", widgetCount).apply();

                JSONObject statusJson = json.optJSONObject("status_4x2");
                if (statusJson != null) {
                    Iterator<String> keys = statusJson.keys();
                    while (keys.hasNext()) {
                        String keyId = keys.next();
                        String valueStatus = statusJson.getString(keyId);
                        try {
                            int id = Integer.parseInt(keyId);
                            if (!"limited".equals(widgetStatusMap.get(id))) {
                                widgetStatusMap.put(id, valueStatus);
                            }
                        } catch (Exception e) {}
                    }
                }
            }

            runOnUiThread(this::cargarListaSegunTipo);

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                if (layoutConnectionError != null) layoutConnectionError.setVisibility(View.VISIBLE);
            });
        }
    }

    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (bannerViewPager.getAdapter() != null) {
                int current = bannerViewPager.getCurrentItem();
                int total = bannerViewPager.getAdapter().getItemCount();
                bannerViewPager.setCurrentItem((current + 1) % total, true);
                sliderHandler.postDelayed(this, 4000);
            }
        }
    };

    private void abrirPantallaDetalles(StickerPack pack) {
        Config.selectedPack = pack;
        Intent intent = new Intent(FullListActivity.this, StickerDetailsActivity.class);
        startActivity(intent);
    }

    public void abrirWallpaperDetalles(Config.Wallpaper wall) {
        Intent intent = new Intent(this, WallpaperDetailsActivity.class);
        intent.putExtra("wall_name", wall.name);
        intent.putExtra("wall_author", wall.publisher);
        intent.putExtra("wall_image", wall.imageFile);
        intent.putExtra("wall_color", wall.colorBg);
        intent.putExtra("wall_artist_link", wall.artistLink);
        intent.putExtra("is_limited", wall.isLimitedTime);
        intent.putExtra("is_hidden", wall.isHidden);
        startActivity(intent);
    }

    private void limpiarCacheYProtegerFavoritos() {
        Toast.makeText(this, "Optimizing...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Glide.get(this).clearDiskCache();
                android.content.SharedPreferences prefs = getSharedPreferences("WidgetPrefs", MODE_PRIVATE);
                java.util.Set<String> favorites = prefs.getStringSet("fav_wallpapers", new java.util.HashSet<>());
                String baseUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/";
                int recuperados = 0;
                for (String imageId : favorites) {
                    try {
                        int id = Integer.parseInt(imageId);
                        String fullUrl = baseUrl + "4x2/BG_W_" + String.format("%02d", id) + ".png";
                        Glide.with(this).asBitmap().load(fullUrl).submit().get();
                        recuperados++;
                    } catch (Exception e) { }
                }
                final int finalRecuperados = recuperados;
                runOnUiThread(() -> {
                    Toast.makeText(FullListActivity.this, "Done! " + finalRecuperados + " favorites were protected.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rvList != null && rvList.getAdapter() != null) {
            if (rvList.getAdapter() instanceof PremiumAdapter) {
                ((PremiumAdapter) rvList.getAdapter()).updateData();
            }
            else if (rvList.getAdapter() instanceof WallpaperAdapter) {
                rvList.getAdapter().notifyDataSetChanged();
            }
            else if (rvList.getAdapter() instanceof WidgetGalleryAdapter) {
                ((WidgetGalleryAdapter) rvList.getAdapter()).updateData();
            }
            else if (rvList.getAdapter() instanceof BatteryThemeAdapter) {
                ((BatteryThemeAdapter) rvList.getAdapter()).updateData();
            }
            else {
                rvList.getAdapter().notifyDataSetChanged();
            }
        }
    }


    private void mostrarDialogoGastarMonedas(String title, int cost, int balance, Runnable onUseCoins, Runnable onWatchAd) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_spend_coins, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView txtTitle = view.findViewById(R.id.txtDialogTitle);
        TextView txtMsg = view.findViewById(R.id.txtDialogMessage);
        TextView txtBal = view.findViewById(R.id.txtCurrentBalance);
        Button btnUse = view.findViewById(R.id.btnUseCoins);
        TextView btnAd = view.findViewById(R.id.btnWatchAd);

        txtTitle.setText(title);
        txtMsg.setText("Use " + cost + " coins to unlock this item instantly without ads.");
        txtBal.setText("Balance: " + balance + " coins");
        btnUse.setText("USE " + cost + " COINS");

        btnUse.setOnClickListener(v -> {
            try {
                MediaPlayer mp = MediaPlayer.create(this, R.raw.coin);
                if (mp != null) {
                    mp.setOnCompletionListener(MediaPlayer::release);
                    mp.start();
                }
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            dialog.dismiss();
            if (onUseCoins != null) onUseCoins.run();
        });

        btnAd.setOnClickListener(v -> {
            dialog.dismiss();
            if (onWatchAd != null) onWatchAd.run();
        });

        dialog.show();
    }
}