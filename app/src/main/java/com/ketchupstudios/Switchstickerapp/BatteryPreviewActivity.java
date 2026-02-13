package com.ketchupstudios.Switchstickerapp;

import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BatteryPreviewActivity extends AppCompatActivity {

    private Config.BatteryTheme currentTheme;
    private RewardedAd mRewardedAd;
    private boolean rewardEarned = false;

    private Button btnApply, btnSupport;
    private ImageView imgBackground;
    private LinearLayout colorsContainer;

    private int selectedColor = Color.WHITE;
    private static final String BASE_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/Bateria/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_preview);

        String themeId = getIntent().getStringExtra("THEME_ID");
        for(Config.BatteryTheme t : Config.batteryThemes) {
            if(t.id.equals(themeId)) {
                currentTheme = t;
                break;
            }
        }

        if (currentTheme == null) {
            finish();
            return;
        }

        try {
            selectedColor = Color.parseColor(currentTheme.colorBg);
        } catch (Exception e) {
            selectedColor = Color.parseColor("#a7423e");
        }

        imgBackground = findViewById(R.id.imgPreviewWallpaper);
        btnApply = findViewById(R.id.btnApply);
        btnSupport = findViewById(R.id.btnSupportArtist);
        colorsContainer = findViewById(R.id.layoutColorsContainer);

        if (currentTheme.artistLink != null && !currentTheme.artistLink.isEmpty()) {
            btnSupport.setVisibility(View.VISIBLE);
            btnSupport.setOnClickListener(v -> {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(currentTheme.artistLink));
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnSupport.setVisibility(View.GONE);
        }

        setupPreviewState(findViewById(R.id.state100), "100", "100.png");
        setupPreviewState(findViewById(R.id.state50), "50", "50.png");
        setupPreviewState(findViewById(R.id.state10), "10", "10.png");
        setupPreviewState(findViewById(R.id.stateCharging), "⚡", "cargando.png");

        cargarWallpaperAleatorio();
        analizarColoresDelTema();

        btnApply.setOnClickListener(v -> iniciarCargaDeAnuncio());
    }

    private void analizarColoresDelTema() {
        String fullUrl = BASE_URL + currentTheme.folder + "/100.png";

        Glide.with(this)
                .asBitmap()
                .load(fullUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        generarPaleta(resource);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private void generarPaleta(Bitmap bitmap) {
        if (bitmap == null || isFinishing()) return;

        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;
            List<Integer> candidatos = new ArrayList<>();

            // 1. [NUEVO] COLOR FAVORITO DEL USUARIO
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE); // Nota: Usa AppPrefs que es global
            if (prefs.contains("user_favorite_color")) {
                candidatos.add(prefs.getInt("user_favorite_color", Color.BLACK));
            }


            // 1. Color del JSON (Si no es blanco)
            candidatos.add(selectedColor);

            // 2. Negro Carbón
            candidatos.add(Color.parseColor("#2e2828"));

            // 3. Colores de la imagen
            if (palette.getVibrantSwatch() != null) candidatos.add(palette.getVibrantSwatch().getRgb());
            if (palette.getDarkVibrantSwatch() != null) candidatos.add(palette.getDarkVibrantSwatch().getRgb());
            if (palette.getMutedSwatch() != null) candidatos.add(palette.getMutedSwatch().getRgb());
            if (palette.getDominantSwatch() != null) candidatos.add(palette.getDominantSwatch().getRgb());
            if (palette.getLightVibrantSwatch() != null) candidatos.add(palette.getLightVibrantSwatch().getRgb());

            // 4. Filtrado: Sin blancos, sin repetidos, máximo 7
            List<Integer> coloresFinales = new ArrayList<>();
            for (Integer c : candidatos) {
                if (esBlanco(c)) continue;

                boolean esRepetido = false;
                for (Integer cf : coloresFinales) {
                    if (sonColoresParecidos(c, cf)) {
                        esRepetido = true;
                        break;
                    }
                }

                if (!esRepetido) {
                    coloresFinales.add(c);
                }

                if (coloresFinales.size() >= 7) break;
            }

            // 5. Garantizar Mínimo 5 (Fallbacks seguros si la imagen es muy simple o blanca)
            int[] fallbacks = {Color.parseColor("#455A64"), Color.parseColor("#5D4037"), Color.parseColor("#388E3C"), Color.parseColor("#1976D2")};
            for (int f : fallbacks) {
                if (coloresFinales.size() >= 5) break;
                boolean repetido = false;
                for (int cf : coloresFinales) if (sonColoresParecidos(f, cf)) { repetido = true; break; }
                if (!repetido) coloresFinales.add(f);
            }

            crearBotonesDeColor(coloresFinales);
        });
    }

    private boolean esBlanco(int color) {
        // Umbral alto para detectar blanco o casi blanco
        return Color.red(color) > 240 && Color.green(color) > 240 && Color.blue(color) > 240;
    }

    private boolean sonColoresParecidos(int c1, int c2) {
        double dist = Math.sqrt(Math.pow(Color.red(c1)-Color.red(c2),2) +
                Math.pow(Color.green(c1)-Color.green(c2),2) +
                Math.pow(Color.blue(c1)-Color.blue(c2),2));
        return dist < 35.0; // Umbral de diferencia visual
    }

    private void crearBotonesDeColor(List<Integer> colores) {
        if (colorsContainer == null) return;
        colorsContainer.removeAllViews();

        int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        for (int color : colores) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(marginPx, 0, marginPx, 0);
            card.setLayoutParams(params);
            card.setCardBackgroundColor(color);
            card.setRadius(sizePx / 2f);
            card.setCardElevation(0f);

            card.setOnClickListener(v -> aplicarColorPreview(color));
            colorsContainer.addView(card);
        }
    }

    private void aplicarColorPreview(int color) {
        selectedColor = color;
        actualizarFondoVista(findViewById(R.id.state100), color);
        actualizarFondoVista(findViewById(R.id.state50), color);
        actualizarFondoVista(findViewById(R.id.state10), color);
        actualizarFondoVista(findViewById(R.id.stateCharging), color);
    }

    private void actualizarFondoVista(View view, int color) {
        if (view == null) return;
        ImageView bg = view.findViewById(R.id.previewBg);
        if (bg != null) bg.setColorFilter(color);
    }

    private void cargarWallpaperAleatorio() {
        if (isFinishing() || isDestroyed()) return;

        if (Config.wallpapers != null && !Config.wallpapers.isEmpty()) {
            try {
                int rand = new Random().nextInt(Config.wallpapers.size());
                Config.Wallpaper wall = Config.wallpapers.get(rand);
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
                        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
                        String wallUrl = baseUrl + "wallpappers/" + imageFile;

                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Glide.with(BatteryPreviewActivity.this).load(wallUrl).centerCrop().into(imgBackground);
                            }
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void setupPreviewState(View view, String percentText, String imgName) {
        if (isFinishing() || isDestroyed()) return;

        ImageView bg = view.findViewById(R.id.previewBg);
        ImageView character = view.findViewById(R.id.previewChar);
        TextView txt = view.findViewById(R.id.lblPercent);
        TextView title = view.findViewById(R.id.lblTitle);
        ImageView bolt = view.findViewById(R.id.imgChargingBolt);

        int colorTexto = Color.WHITE;

        try {
            bg.setColorFilter(selectedColor);
            colorTexto = Color.parseColor(currentTheme.textColor);
            txt.setTextColor(colorTexto);
            title.setTextColor(colorTexto);
            bolt.setColorFilter(colorTexto);
        } catch (Exception e) {}

        if (percentText.equals("⚡")) {
            txt.setVisibility(View.GONE);
            title.setVisibility(View.GONE);
            bolt.setVisibility(View.VISIBLE);
        } else {
            txt.setVisibility(View.VISIBLE);
            title.setVisibility(View.VISIBLE);
            bolt.setVisibility(View.GONE);
            txt.setText(percentText);
        }

        String fullUrl = BASE_URL + currentTheme.folder + "/" + imgName;
        Glide.with(this).load(fullUrl).into(character);
    }

    private void iniciarCargaDeAnuncio() {
        if (isFinishing() || isDestroyed()) return;

        btnApply.setEnabled(false);
        btnApply.setText("Loading Ad...");

        String idAdMob = "ca-app-pub-9087203932210009/4573750306";
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, idAdMob, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                if (isFinishing() || isDestroyed()) return;

                mRewardedAd = rewardedAd;
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        if (isFinishing() || isDestroyed()) return;
                        mRewardedAd = null;
                        restaurarBoton();
                        if (rewardEarned) descargarTemaReal();
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        if (isFinishing() || isDestroyed()) return;
                        mRewardedAd = null;
                        restaurarBoton();
                        descargarTemaReal();
                    }
                });

                rewardEarned = false;
                mRewardedAd.show(BatteryPreviewActivity.this, rewardItem -> rewardEarned = true);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (isFinishing() || isDestroyed()) return;
                mRewardedAd = null;
                restaurarBoton();
                descargarTemaReal();
            }
        });
    }

    private void restaurarBoton() {
        if (isFinishing() || isDestroyed()) return;
        if(btnApply != null) {
            btnApply.setText("Apply Battery Theme");
            btnApply.setEnabled(true);
            btnApply.setAlpha(1.0f);
        }
    }

    private void descargarTemaReal() {
        if (isFinishing() || isDestroyed()) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Applied " + currentTheme.name + "...");
        pd.setCancelable(false);
        try { pd.show(); } catch (Exception e) { return; }

        new Thread(() -> {
            try {
                String[] files = {"100.png", "50.png", "10.png", "cargando.png"};
                File dir = new File(getFilesDir(), "battery_images");
                if (!dir.exists()) dir.mkdirs();

                for (String fileName : files) {
                    URL url = new URL(BASE_URL + currentTheme.folder + "/" + fileName);
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    if (bmp != null) {
                        File dest = new File(dir, fileName);
                        FileOutputStream out = new FileOutputStream(dest);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush(); out.close();
                    }
                }

                String hexColor = String.format("#%06X", (0xFFFFFF & selectedColor));

                SharedPreferences prefs = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("battery_color_bg", hexColor)
                        .putString("battery_text_color", currentTheme.textColor)
                        .apply();

                Intent intent = new Intent(this, BatteryWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, BatteryWidgetProvider.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        try { if (pd != null && pd.isShowing()) pd.dismiss(); } catch (Exception e) {}
                        Toast.makeText(this, "Theme successfully implemented!", Toast.LENGTH_SHORT).show();
                        solicitarPinWidget();
                        finish();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        try { if (pd != null && pd.isShowing()) pd.dismiss(); } catch (Exception ex) {}
                        Toast.makeText(this, "Error downloading", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void solicitarPinWidget() {
        if (isFinishing() || isDestroyed()) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);
            ComponentName myProvider = new ComponentName(this, BatteryWidgetProvider.class);

            if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null);
            } else {
                Toast.makeText(this, "Go to your home screen and add the widget manually.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Go to your home screen and add the widget manually.", Toast.LENGTH_LONG).show();
        }
    }
}