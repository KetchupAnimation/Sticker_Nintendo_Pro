package com.ketchupstudios.Switchstickerapp;

// AÑADIMOS LOS IMPORTS QUE FALTABAN
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class BatteryGalleryActivity extends AppCompatActivity {

    private RecyclerView rvThemes;
    private BatteryThemeAdapter adapter;
    private List<Config.BatteryTheme> themeList = new ArrayList<>();

    private static final String JSON_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
    private static final String BASE_IMG_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/Bateria/";

    private RewardedAd mRewardedAd;
    private Config.BatteryTheme selectedTheme;
    private boolean rewardEarned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_list);

        TextView title = findViewById(R.id.txtCategoryTitle);
        title.setText("Galería de Batería");

        View banner = findViewById(R.id.bannerViewPagerFull);
        if(banner != null) banner.setVisibility(View.GONE);
        View cacheBtn = findViewById(R.id.cardCleanCache);
        if(cacheBtn != null) cacheBtn.setVisibility(View.GONE);

        rvThemes = findViewById(R.id.rvFullList);
        rvThemes.setLayoutManager(new GridLayoutManager(this, 2));

        // Agregamos R.layout.item_battery_theme para decirle que use el diseño de galería
        adapter = new BatteryThemeAdapter(this, themeList, R.layout.item_battery_theme, theme -> iniciarProcesoAplicar(theme));
        rvThemes.setAdapter(adapter);

        cargarAnuncio();
        descargarJSON();
    }

    private void descargarJSON() {
        new Thread(() -> {
            try {
                URL url = new URL(JSON_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject json = new JSONObject(result.toString());
                JSONArray array = json.getJSONArray("themes");

                themeList.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    themeList.add(new Config.BatteryTheme(
                            o.getString("id"),
                            o.getString("name"),
                            o.getString("folder"),
                            o.getString("color_bg"),
                            o.getString("text_color"),
                            o.optBoolean("is_new", false),
                            o.optString("artist_link", "")
                    ));
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void iniciarProcesoAplicar(Config.BatteryTheme theme) {
        selectedTheme = theme;
        rewardEarned = false;
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Preparando descarga en 3...");
        dialog.setCancelable(false);
        dialog.show();

        new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {
                dialog.setMessage("Preparando descarga en " + (millisUntilFinished / 1000 + 1) + "...");
            }
            public void onFinish() {
                dialog.dismiss();
                mostrarAnuncio();
            }
        }.start();
    }

    private void cargarAnuncio() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {
            @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { mRewardedAd = null; }
            @Override public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        mRewardedAd = null;
                        cargarAnuncio();
                        if (rewardEarned) descargarYGuardarTema(selectedTheme);
                    }
                });
            }
        });
    }

    private void mostrarAnuncio() {
        if (mRewardedAd != null) {
            mRewardedAd.show(this, rewardItem -> rewardEarned = true);
        } else {
            CustomToast.makeText(this, "Cargando anuncio...", Toast.LENGTH_SHORT).show();
            descargarYGuardarTema(selectedTheme);
        }
    }

    private void descargarYGuardarTema(Config.BatteryTheme theme) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Descargando " + theme.name + "...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String[] files = {"100.png", "50.png", "10.png", "cargando.png"};
                File dir = new File(getFilesDir(), "battery_images");
                if (!dir.exists()) dir.mkdirs();

                for (String fileName : files) {
                    String fileUrl = BASE_IMG_URL + theme.folder + "/" + fileName;
                    URL url = new URL(fileUrl);
                    Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    File destFile = new File(dir, fileName);
                    FileOutputStream out = new FileOutputStream(destFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush(); out.close();
                }

                SharedPreferences prefs = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("battery_theme_id", theme.id)
                        .putString("battery_theme_name", theme.name)
                        .putString("battery_color_bg", theme.colorBg)
                        .putString("battery_text_color", theme.textColor).apply();

                Intent intent = new Intent(this, BatteryWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, BatteryWidgetProvider.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);

                runOnUiThread(() -> {
                    pd.dismiss();
                    CustomToast.makeText(this, "¡Tema aplicado! Revisa tu widget.", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> { pd.dismiss(); CustomToast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
            }
        }).start();
    }
}