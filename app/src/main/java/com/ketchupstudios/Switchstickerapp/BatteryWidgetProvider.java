package com.ketchupstudios.Switchstickerapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.BatteryManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.File;

public class BatteryWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "BatteryWidget";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        // Detectar conexión de cable para actualizar al instante
        if (Intent.ACTION_POWER_CONNECTED.equals(action) ||
                Intent.ACTION_POWER_DISCONNECTED.equals(action) ||
                Intent.ACTION_BATTERY_LOW.equals(action) ||
                Intent.ACTION_BATTERY_OKAY.equals(action)) {

            updateAllWidgets(context);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, BatteryWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int id : appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id);
        }
    }

    private static void updateSingleWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_battery_2x2);
            SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);

            // 1. VERIFICAR SI HAY TEMA (Si no hay color guardado, es nuevo)
            String savedColor = prefs.getString("battery_color_bg", null);

            if (savedColor == null) {
                // === ESTADO POR DEFECTO (VACÍO / APPLY THEME) ===
                views.setViewVisibility(R.id.txtApplyTheme, View.VISIBLE);
                views.setViewVisibility(R.id.textContainer, View.GONE); // Ocultamos textos de batería

                // Fondo Rojo por defecto (#a7423e)
                views.setInt(R.id.imgBgSolid, "setColorFilter", Color.parseColor("#a7423e"));

                // Imagen por defecto (batterydf)
                views.setImageViewResource(R.id.imgCharacter, R.drawable.batterydf);

            } else {
                // === ESTADO NORMAL (CON TEMA Y BATERÍA) ===
                views.setViewVisibility(R.id.txtApplyTheme, View.GONE);
                views.setViewVisibility(R.id.textContainer, View.VISIBLE);

                // --- Lógica de Batería Real ---
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);

                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int percentage = (int) ((level / (float) scale) * 100);

                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

                    // Colores
                    String textColorHex = prefs.getString("battery_text_color", "#FFFFFF");
                    int textColor;
                    try { textColor = Color.parseColor(textColorHex); } catch (Exception e) { textColor = Color.WHITE; }

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

                    // Imagen Descargada
                    String imageName;
                    if (isCharging) imageName = "cargando.png";
                    else if (percentage >= 80) imageName = "100.png";
                    else if (percentage >= 20) imageName = "50.png";
                    else imageName = "10.png";

                    File dir = new File(context.getFilesDir(), "battery_images");
                    File imageFile = new File(dir, imageName);
                    if (imageFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        views.setImageViewBitmap(R.id.imgCharacter, bitmap);
                    } else {
                        views.setImageViewResource(R.id.imgCharacter, R.drawable.batterydf);
                    }

                    // Fondo Real
                    try { views.setInt(R.id.imgBgSolid, "setColorFilter", Color.parseColor(savedColor)); } catch (Exception e) {}
                }
            }

            // === CLICK INTENT (CORREGIDO) ===
            // Aquí es donde decimos a dónde ir
            Intent intentList = new Intent(context, FullListActivity.class);

            // [IMPORTANTE] Usamos "battery" para que FullListActivity cargue la lista correcta
            intentList.putExtra("TYPE", "battery");

            intentList.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piList = PendingIntent.getActivity(context, 101, intentList, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.rootContainer, piList);

            // ACTUALIZAR
            appWidgetManager.updateAppWidget(widgetId, views);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 1. SE EJECUTA CUANDO SE AGREGA EL PRIMER WIDGET (Arranca el servicio)
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        try {
            android.content.Intent serviceIntent = new android.content.Intent(context, BatteryService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. SE EJECUTA CUANDO SE BORRA EL ÚLTIMO WIDGET (Apaga el servicio y ahorra batería)
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        try {
            context.stopService(new android.content.Intent(context, BatteryService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}