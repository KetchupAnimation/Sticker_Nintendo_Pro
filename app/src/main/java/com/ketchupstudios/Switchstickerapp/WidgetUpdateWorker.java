package com.ketchupstudios.Switchstickerapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WidgetUpdateWorker extends Worker {

    private static final String GITHUB_ROOT_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/";
    private static final String PREFS_NAME = "WidgetWeatherCache";

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // PASO 1: CACHÉ
        WeatherService.WeatherData cachedWeather = loadCachedWeather(context);
        if (cachedWeather != null) {
            actualizarTodosLosWidgets(context, appWidgetManager, cachedWeather, true);
        }

        // PASO 2: UBICACIÓN RÁPIDA
        Location bestLocation = getLastKnownLocation(context);
        if (bestLocation != null) {
            WeatherService.WeatherData weather = WeatherService.getWeather(context, bestLocation.getLatitude(), bestLocation.getLongitude());
            if (weather != null) {
                saveWeatherToCache(context, weather);
                actualizarTodosLosWidgets(context, appWidgetManager, weather, true);
            }
        }

        // PASO 3: UBICACIÓN FRESCA
        try {
            Location freshLocation = getFreshLocation(context);
            if (freshLocation != null) {
                WeatherService.WeatherData weather = WeatherService.getWeather(context, freshLocation.getLatitude(), freshLocation.getLongitude());
                if (weather != null) {
                    saveWeatherToCache(context, weather);
                    actualizarTodosLosWidgets(context, appWidgetManager, weather, true);
                }
            } else if (cachedWeather == null && bestLocation == null) {
                // PASO 4: EMERGENCIA
                WeatherService.WeatherData emptyData = new WeatherService.WeatherData("", "--°", "", "");
                actualizarTodosLosWidgets(context, appWidgetManager, emptyData, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (cachedWeather == null) {
                WeatherService.WeatherData emptyData = new WeatherService.WeatherData("", "--°", "", "");
                actualizarTodosLosWidgets(context, appWidgetManager, emptyData, true);
            }
        }

        return Result.success();
    }

    // --- MÉTODOS DE CACHÉ Y UBICACIÓN ---
    private void saveWeatherToCache(Context context, WeatherService.WeatherData data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("cached_city", data.city)
                .putString("cached_temp", data.temp)
                .putString("cached_desc", data.description)
                .putString("cached_minmax", data.minMax)
                .putLong("cached_time", System.currentTimeMillis())
                .apply();
    }

    private WeatherService.WeatherData loadCachedWeather(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains("cached_temp")) return null;
        return new WeatherService.WeatherData(
                prefs.getString("cached_city", ""),
                prefs.getString("cached_temp", "--°"),
                prefs.getString("cached_desc", ""),
                prefs.getString("cached_minmax", "")
        );
    }

    private Location getLastKnownLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location bestLocation = null;
        try {
            List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) continue;
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }
        } catch (SecurityException e) { }
        return bestLocation;
    }

    private Location getFreshLocation(Context context) throws InterruptedException {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        final Location[] result = {null};
        CountDownLatch latch = new CountDownLatch(1);
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(location -> { result[0] = location; latch.countDown(); })
                    .addOnFailureListener(e -> latch.countDown());
            latch.await(5, TimeUnit.SECONDS);
        } catch (SecurityException e) { return null; }
        return result[0];
    }

    // --- ACTUALIZACIÓN DE UI (CORREGIDO PARA 4x1) ---

    private void actualizarTodosLosWidgets(Context context, AppWidgetManager appWidgetManager, WeatherService.WeatherData weather, boolean updateBg) {
        // 4x2 sigue igual (Usa carpeta 4x2 y CenterCrop)
        updateWidgetsType(context, appWidgetManager, WidgetProvider4x2.class, R.layout.widget_layout_4x2, weather, updateBg, 600, 300, "4x2", new CenterCrop());

        // CORRECCIÓN AQUÍ: 4x1 ahora usa carpeta "4x1" y FitCenter para no cortar la imagen
        updateWidgetsType(context, appWidgetManager, WidgetProvider4x1.class, R.layout.widget_layout_4x1, weather, updateBg, 600, 150, "4x1", new FitCenter());

        // 2x2 sigue igual (Usa carpeta 2x2 y FitCenter)
        updateWidgetsType(context, appWidgetManager, WidgetProvider2x2.class, R.layout.widget_layout_2x2, weather, updateBg, 400, 400, "2x2", new FitCenter());
    }

    private void updateWidgetsType(Context context, AppWidgetManager appWidgetManager, Class<?> widgetClass, int layoutId, WeatherService.WeatherData weather, boolean updateBg, int targetWidth, int targetHeight, String folderType, Transformation<Bitmap> transformation) {
        ComponentName component = new ComponentName(context, widgetClass);
        int[] ids = appWidgetManager.getAppWidgetIds(component);

        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

            views.setViewVisibility(R.id.txt_loading_state, android.view.View.GONE);
            views.setViewVisibility(R.id.layout_data_container, android.view.View.VISIBLE);

            if (weather != null) {
                setTextIfExist(views, R.id.widgetTemp, weather.temp);
                setTextIfExist(views, R.id.widgetDesc, weather.description);
                setTextIfExist(views, R.id.widgetMinMax, weather.minMax);

                boolean mostrarCiudad = true;
                String ciudadLimpia = (weather.city != null) ? weather.city.trim() : "";
                if (ciudadLimpia.isEmpty() || ciudadLimpia.equalsIgnoreCase("Ubicación") || ciudadLimpia.equalsIgnoreCase("Ubicacion")) {
                    mostrarCiudad = false;
                }

                if (mostrarCiudad) {
                    views.setViewVisibility(R.id.widgetCity, android.view.View.VISIBLE);
                    views.setTextViewText(R.id.widgetCity, ciudadLimpia);
                } else {
                    views.setViewVisibility(R.id.widgetCity, android.view.View.GONE);
                }
            }

            if (updateBg) {
                try {
                    SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
                    Set<String> favs = prefs.getStringSet("fav_wallpapers", new HashSet<>());
                    List<String> favList = new ArrayList<>(favs);

                    String selectedImageId;
                    if (!favList.isEmpty()) {
                        Collections.sort(favList);
                        long timeBlock = System.currentTimeMillis() / (30 * 60 * 1000);
                        int index = Math.abs((id * 7) + (int)timeBlock) % favList.size();
                        selectedImageId = favList.get(index);
                    } else {
                        int randomNum = new Random().nextInt(20) + 1;
                        selectedImageId = String.valueOf(randomNum);
                    }

                    // Construye la URL usando el folderType que pasamos ("4x2", "4x1" o "2x2")
                    String fullUrl = GITHUB_ROOT_URL + folderType + "/BG_W_" + String.format(Locale.US, "%02d", Integer.parseInt(selectedImageId)) + ".png";

                    RequestOptions options = new RequestOptions()
                            .override(targetWidth, targetHeight)
                            .transform(transformation);

                    FutureTarget<Bitmap> futureTarget = Glide.with(context.getApplicationContext())
                            .asBitmap()
                            .load(fullUrl)
                            .apply(options)
                            .submit();

                    Bitmap bitmap = futureTarget.get();
                    views.setImageViewBitmap(R.id.widgetBgImage, bitmap);

                } catch (Exception e) { }
            }

            // REDIRECCIÓN A LA GALERÍA DE WIDGETS
            Intent intent = new Intent(context, FullListActivity.class);
            intent.putExtra("TYPE", "widgets");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

// CAMBIO: En lugar de 0, usamos la variable 'id' (que es el ID único del widget en Android)
            PendingIntent pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.rootContainer, pi);

            appWidgetManager.updateAppWidget(id, views);
        }
    }

    private void setTextIfExist(RemoteViews views, int viewId, String text) {
        try {
            views.setTextViewText(viewId, text);
        } catch (Exception ignored) {}
    }
}