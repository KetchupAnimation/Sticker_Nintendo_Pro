package com.ketchupstudios.Switchstickerapp;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class WidgetPreviewActivity extends AppCompatActivity {

    // CAMBIO 1: Agregamos imgWidget3 para controlar la tercera imagen (Reloj)
    private ImageView imgBackground, imgWidget, imgWidget2, imgWidget3;
    private Button btnFavorite, btnPin;
    private String imageId;
    private String widgetType;
    private SharedPreferences prefs;
    private Set<String> favorites;
    private static final String WIDGET_BASE_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_preview);

        imageId = getIntent().getStringExtra("IMAGE_ID");
        widgetType = getIntent().getStringExtra("WIDGET_TYPE");
        if (widgetType == null) widgetType = "4x2";

        // Vincular vistas
        imgBackground = findViewById(R.id.imgBackgroundLayer);
        imgWidget = findViewById(R.id.imgWidgetLayer);   // Clima
        imgWidget2 = findViewById(R.id.imgWidgetLayer2); // Calendario
        imgWidget3 = findViewById(R.id.imgWidgetLayer3); // Nuevo: Reloj/Híbrido

        btnFavorite = findViewById(R.id.btnToggleFavorite);
        btnPin = findViewById(R.id.btnPinWidget);
        btnPin.setOnClickListener(v -> mostrarDialogoSeleccionWidget());

        prefs = getSharedPreferences("WidgetPrefs", MODE_PRIVATE);
        favorites = prefs.getStringSet("fav_wallpapers", new HashSet<>());

        cargarWidgetImage();
        cargarWallpaperAleatorio();
        actualizarBoton();
        cargarBanner();

        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void cargarBanner() {
        FrameLayout bannerContainer = findViewById(R.id.bannerContainer);
        AdView adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-9087203932210009/7253353145");
        adView.setAdSize(AdSize.BANNER);
        bannerContainer.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void cargarWidgetImage() {
        try {
            int id = Integer.parseInt(imageId);
            String fullUrl = WIDGET_BASE_URL + widgetType + "/BG_W_" + String.format("%02d", id) + ".png";

            int radius = 40;
            if(widgetType.equals("2x2")) radius = 30;
            if(widgetType.equals("4x1")) radius = 45;

            // 1. Cargar imagen Clima
            Glide.with(this)
                    .load(fullUrl)
                    .transform(new CenterCrop(), new RoundedCorners(radius))
                    .into(imgWidget);

            // 2. Cargar imagen Calendario
            if (imgWidget2 != null) {
                Glide.with(this)
                        .load(fullUrl)
                        .transform(new CenterCrop(), new RoundedCorners(radius))
                        .into(imgWidget2);
            }

            // 3. CAMBIO 2: Cargar imagen Reloj/Híbrido
            if (imgWidget3 != null) {
                Glide.with(this)
                        .load(fullUrl)
                        .transform(new CenterCrop(), new RoundedCorners(radius))
                        .into(imgWidget3);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cargarWallpaperAleatorio() {
        // ... (Tu código existente de cargar wallpaper se mantiene igual)
        // He resumido esta parte para no hacer el mensaje muy largo,
        // pero MANTÉN tu lógica original de cargarWallpaperAleatorio aquí.
        if (Config.wallpapers != null && !Config.wallpapers.isEmpty()) {
            try {
                int randomIdx = new Random().nextInt(Config.wallpapers.size());
                Config.Wallpaper wall = Config.wallpapers.get(randomIdx);
                String colorHex = wall.colorBg;
                if (colorHex == null || colorHex.isEmpty()) colorHex = "#E0E0E0";
                if (!colorHex.startsWith("#")) colorHex = "#" + colorHex;
                try { imgBackground.setBackgroundColor(Color.parseColor(colorHex)); }
                catch (Exception e) { imgBackground.setBackgroundColor(Color.LTGRAY); }
                String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
                String wallUrl = baseUrl + "wallpappers/" + wall.imageFile;
                Glide.with(this).load(wallUrl).centerCrop().into(imgBackground);
                return;
            } catch (Exception e) { e.printStackTrace(); }
        }

        new Thread(() -> {
            try {
                URL url = new URL(Config.STICKER_JSON_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    JSONObject json = new JSONObject(result.toString());
                    JSONArray wallpapersArray = json.getJSONArray("wallpapers");
                    if (wallpapersArray.length() > 0) {
                        int randomIdx = new Random().nextInt(wallpapersArray.length());
                        JSONObject wallObj = wallpapersArray.getJSONObject(randomIdx);
                        String imageFile = wallObj.getString("image_file");
                        String rawColor = wallObj.optString("ColorBG", "#E0E0E0");
                        if (!rawColor.startsWith("#")) rawColor = "#" + rawColor;
                        int colorTemporal;
                        try { colorTemporal = Color.parseColor(rawColor); } catch (Exception e) { colorTemporal = Color.LTGRAY; }
                        final int finalColor = colorTemporal;
                        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
                        String wallUrl = baseUrl + "wallpappers/" + imageFile;
                        runOnUiThread(() -> {
                            if (!isDestroyed()) {
                                imgBackground.setBackgroundColor(finalColor);
                                Glide.with(WidgetPreviewActivity.this).load(wallUrl).centerCrop().into(imgBackground);
                            }
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void actualizarBoton() {
        btnFavorite.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        if (favorites.contains(imageId)) {
            btnFavorite.setText("Remove from favorites");
            ViewCompat.setBackgroundTintList(btnFavorite, ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        } else {
            btnFavorite.setText("Add to favorites");
            ViewCompat.setBackgroundTintList(btnFavorite, null);
        }
    }

    private void toggleFavorite() {
        Set<String> currentFavs = new HashSet<>(prefs.getStringSet("fav_wallpapers", new HashSet<>()));
        if (currentFavs.contains(imageId)) {
            currentFavs.remove(imageId);
            CustomToast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            currentFavs.add(imageId);
            CustomToast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("fav_wallpapers", currentFavs).apply();
        favorites = currentFavs;
        actualizarBoton();
    }

    private void mostrarDialogoSeleccionWidget() {
        // CREAR Y MOSTRAR EL BOTTOM SHEET
        WidgetSelectionBottomSheet bottomSheet = new WidgetSelectionBottomSheet();

        // RECIBIR EL CLIC Y AÑADIR EL WIDGET
        bottomSheet.setListener(widgetClass -> {
            solicitarPin(widgetClass);
        });

        bottomSheet.show(getSupportFragmentManager(), "WidgetSelection");
    }

    private void solicitarPin(Class<?> widgetClass) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.appwidget.AppWidgetManager appWidgetManager = getSystemService(android.appwidget.AppWidgetManager.class);
            android.content.ComponentName myProvider = new android.content.ComponentName(this, widgetClass);

            if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null);
            } else {
                CustomToast.makeText(this, "Your launcher does not support this feature", Toast.LENGTH_SHORT).show();
            }
        } else {
            CustomToast.makeText(this, "Feature available only on Android 8+", Toast.LENGTH_SHORT).show();
        }
    }
}