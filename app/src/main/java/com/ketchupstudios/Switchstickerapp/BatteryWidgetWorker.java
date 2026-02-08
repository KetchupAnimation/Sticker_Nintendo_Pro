package com.ketchupstudios.Switchstickerapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.BatteryManager;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class BatteryWidgetWorker extends Worker {

    public BatteryWidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        try {
            // 1. OBTENER ESTADO DE BATERÍA
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus == null) return Result.failure();

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = (level / (float) scale) * 100;
            int percentage = (int) batteryPct;

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // 2. DETERMINAR QUÉ IMAGEN USAR
            String imageName;
            if (isCharging) {
                imageName = "cargando.png";
            } else if (percentage >= 80) {
                imageName = "100.png";
            } else if (percentage >= 20) {
                imageName = "50.png";
            } else {
                imageName = "10.png";
            }

            File dir = new File(context.getFilesDir(), "battery_images");
            File imageFile = new File(dir, imageName);
            Bitmap bitmap = null;

            if (imageFile.exists()) {
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            }

            // 3. LEER PREFERENCIAS DE COLOR
            SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);

            // VERIFICACIÓN: Si no hay tema (null), ponemos el estado "Apply Theme" por defecto
            String bgColorHex = prefs.getString("battery_color_bg", null);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_battery_2x2);

            if (bgColorHex == null) {
                // === ESTADO POR DEFECTO (VACÍO) ===
                views.setViewVisibility(R.id.txtApplyTheme, View.VISIBLE);
                views.setViewVisibility(R.id.textContainer, View.GONE);

                // Fondo Rojo default
                views.setInt(R.id.imgBgSolid, "setColorFilter", Color.parseColor("#a7423e"));
                // Imagen default
                views.setImageViewResource(R.id.imgCharacter, R.drawable.batterydf);
            } else {
                // === ESTADO NORMAL (CON TEMA) ===
                views.setViewVisibility(R.id.txtApplyTheme, View.GONE);
                views.setViewVisibility(R.id.textContainer, View.VISIBLE);

                String textColorHex = prefs.getString("battery_text_color", "#FFFFFF");
                int textColor;
                try { textColor = Color.parseColor(textColorHex); } catch (Exception e) { textColor = Color.WHITE; }

                // A) Lógica de Carga vs Textos
                if (isCharging) {
                    views.setViewVisibility(R.id.lblBatteryTitle, View.GONE);
                    views.setViewVisibility(R.id.txtBatteryLevel, View.GONE);
                    views.setViewVisibility(R.id.imgChargingStatus, View.VISIBLE);
                    try { views.setInt(R.id.imgChargingStatus, "setColorFilter", textColor); } catch (Exception e) {}
                } else {
                    views.setViewVisibility(R.id.lblBatteryTitle, View.VISIBLE);
                    views.setViewVisibility(R.id.txtBatteryLevel, View.VISIBLE);
                    views.setViewVisibility(R.id.imgChargingStatus, View.GONE);

                    views.setTextViewText(R.id.txtBatteryLevel, String.valueOf(percentage));
                    views.setTextColor(R.id.txtBatteryLevel, textColor);
                    views.setTextColor(R.id.lblBatteryTitle, textColor);
                }

                // B) Poner Imagen Personaje
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.imgCharacter, bitmap);
                } else {
                    views.setImageViewResource(R.id.imgCharacter, R.drawable.batterydf);
                }

                // C) Pintar el Fondo
                try {
                    views.setInt(R.id.imgBgSolid, "setColorFilter", Color.parseColor(bgColorHex));
                } catch (Exception e) { }
            }

            // 5. CLIC PARA ABRIR APP (CORREGIDO)
            // Cambiamos MainActivity por FullListActivity y añadimos el EXTRA
            Intent appIntent = new Intent(context, FullListActivity.class);
            appIntent.putExtra("TYPE", "battery"); // <--- ESTO ES LO IMPORTANTE
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent piApp = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.rootContainer, piApp);

            // 6. ¡ACTUALIZAR WIDGET!
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, BatteryWidgetProvider.class);
            appWidgetManager.updateAppWidget(thisWidget, views);

            // 7. MANTENIMIENTO
            descargarTemaAleatorioSiNoExiste(context);

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void descargarTemaAleatorioSiNoExiste(Context context) {
        // ... (Tu código de descarga original se mantiene igual) ...
        File dir = new File(context.getFilesDir(), "battery_images");
        if (dir.exists() && dir.list() != null && dir.list().length > 0) {
            return;
        }

        try {
            if (!dir.exists()) dir.mkdirs();

            String jsonUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
            URL url = new URL(jsonUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);

            JSONObject json = new JSONObject(result.toString());
            JSONArray themes = json.getJSONArray("themes");

            if (themes.length() > 0) {
                int randomIdx = new Random().nextInt(themes.length());
                JSONObject theme = themes.getJSONObject(randomIdx);

                String folder = theme.getString("folder");
                String colorBg = theme.getString("color_bg");
                String textColor = theme.getString("text_color");

                String baseUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/Bateria/";
                String[] files = {"100.png", "50.png", "10.png", "cargando.png"};

                for (String fName : files) {
                    URL imgUrl = new URL(baseUrl + folder + "/" + fName);
                    Bitmap bmp = BitmapFactory.decodeStream(imgUrl.openConnection().getInputStream());
                    if (bmp != null) {
                        File dest = new File(dir, fName);
                        FileOutputStream out = new FileOutputStream(dest);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush(); out.close();
                    }
                }

                SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("battery_color_bg", colorBg)
                        .putString("battery_text_color", textColor)
                        .apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}