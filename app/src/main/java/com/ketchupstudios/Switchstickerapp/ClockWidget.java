package com.ketchupstudios.Switchstickerapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class ClockWidget extends AppWidgetProvider {

    private static final String WIDGET_IMG_BASE_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/4x2/BG_W_";
    private static final long ROTATION_INTERVAL = 30 * 60 * 1000;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock_only);

        new Handler(Looper.getMainLooper()).post(() -> {
            // 1. CLICS
            // --- CAMBIO: Fondo abre Galería de Widgets ---
            Intent appIntent = new Intent(context, FullListActivity.class);
            appIntent.putExtra("TYPE", "widgets");
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent piApp = PendingIntent.getActivity(context, 102, appIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.imgWidgetBackground, piApp);
            // ---------------------------------------------

            // Clic en hora y alarma (SE MANTIENE IGUAL)
            Intent alarmIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piAlarm = PendingIntent.getActivity(context, 888, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.clockBig, piAlarm);
            views.setOnClickPendingIntent(R.id.containerAlarm, piAlarm);

            // 2. FONDO COORDINADO
            long currentChunk = System.currentTimeMillis() / ROTATION_INTERVAL;

            SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
            Set<String> favs = prefs.getStringSet("fav_wallpapers", new HashSet<>());
            List<String> favList = new ArrayList<>(favs);
            String selectedImageId;

            if (!favList.isEmpty()) {
                Collections.sort(favList);
                int index = Math.abs((appWidgetId * 7) + (int)currentChunk) % favList.size();
                selectedImageId = favList.get(index);
            } else {
                int randomNum = new Random().nextInt(20) + 1;
                selectedImageId = String.valueOf(randomNum);
            }

            try {
                int imageNumber = Integer.parseInt(selectedImageId);
                String formattedId = String.format(Locale.US, "%02d", imageNumber);
                String fullUrl = WIDGET_IMG_BASE_URL + formattedId + ".png";

                AppWidgetTarget target = new AppWidgetTarget(context, R.id.imgWidgetBackground, views, appWidgetId);
                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(fullUrl)
                        .override(512, 300)
                        .into(target);
            } catch (NumberFormatException e) {
                views.setImageViewResource(R.id.imgWidgetBackground, R.drawable.ic_launcher_background);
            }

            // 3. ALARMA
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            views.setViewVisibility(R.id.containerAlarm, View.GONE);

            if (am != null) {
                AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
                if (info != null) {
                    long triggerTime = info.getTriggerTime();
                    long roundedTime = (triggerTime + 30000) / 60000 * 60000;
                    SimpleDateFormat sdfTime = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    views.setTextViewText(R.id.txtNextAlarm, sdfTime.format(new Date(roundedTime)));
                    views.setViewVisibility(R.id.containerAlarm, View.VISIBLE);
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }
}