package com.ketchupstudios.Switchstickerapp;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class CalendarUpdateWorker extends Worker {

    private static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/";
    private static final String JSON_URL = Config.STICKER_JSON_URL;

    private static final String[] BIRTHDAY_KEYWORDS = {
            "cumpleaños", "cumple", "aniversario", "santo", "festejo", "fiesta", "felicidades", "onomástico", "Aniv", "cum",
            "birthday", "bday", "b-day", "anniversary", "party", "celebration", "born", "bd",
            "aniversário", "aniversario", "niver", "parabéns", "parabens", "festa", "comemoração", "comemoracao"
    };

    private static final String[] BIRTHDAY_IMAGES = {
            "cumple_01.png", "cumple_02.png", "cumple_03.png", "cumple_04.png", "cumple_05.png"
    };

    public CalendarUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            String monthName = monthFormat.format(calendar.getTime());
            String dayNumber = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.DAY_OF_MONTH));

            SimpleDateFormat jsonDateFormat = new SimpleDateFormat("MM-dd", Locale.US);
            String todayKey = jsonDateFormat.format(calendar.getTime());

            SpecialEvent eventoJsonHoy = buscarEventoJsonExacto(todayKey);
            SpecialEvent eventoJsonFuturo = buscarEventoJsonFuturo();
            UserCalendarEvent eventoUsuarioObj = obtenerProximoEventoUsuarioObj(context);

            actualizarWidgets(context, "4x2", WidgetProviderCalendar4x2.class, R.layout.widget_calendar_4x2, monthName, dayNumber, eventoJsonHoy, eventoJsonFuturo, eventoUsuarioObj);
            actualizarWidgets(context, "2x2", WidgetProviderCalendar2x2.class, R.layout.widget_calendar_2x2, monthName, dayNumber, eventoJsonHoy, eventoJsonFuturo, eventoUsuarioObj);

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void actualizarWidgets(Context context, String type, Class<?> cls, int layoutId, String month, String day, SpecialEvent eventoJsonHoy, SpecialEvent eventoJsonFuturo, UserCalendarEvent eventoUsuarioObj) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, cls));

        for (int id : ids) {
            SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
            String imageUrl;
            String cornerText = "";
            boolean showCornerText = false;
            String bottomBarText = null;
            boolean esCumpleanos = false;

            if (eventoUsuarioObj != null && eventoUsuarioObj.rawTitle != null) {
                String tituloLower = eventoUsuarioObj.rawTitle.toLowerCase();
                for (String keyword : BIRTHDAY_KEYWORDS) {
                    // [ARREGLO 2]: Solo activamos el fondo de cumpleaños si el evento es HOY
                    if (tituloLower.contains(keyword)) {
                        if (eventoUsuarioObj.isToday) {
                            esCumpleanos = true;
                        }
                        break;
                    }
                }
            }

            // --- LÓGICA DE FONDO ---
            if (eventoJsonHoy != null) {
                imageUrl = GITHUB_BASE_URL + type + "/" + eventoJsonHoy.imageFile;
                cornerText = eventoJsonHoy.title;
                showCornerText = true;
            } else if (esCumpleanos) {
                Random random = new Random();
                String randomImage = BIRTHDAY_IMAGES[random.nextInt(BIRTHDAY_IMAGES.length)];
                imageUrl = GITHUB_BASE_URL + type + "/" + randomImage;
            } else {
                int index = obtenerIndiceMatematico(prefs, id);
                imageUrl = GITHUB_BASE_URL + type + "/BG_W_" + String.format("%02d", index) + ".png";
            }

            // --- TEXTO INFERIOR ---
            if (eventoUsuarioObj != null) {
                bottomBarText = eventoUsuarioObj.formattedText;
            } else if (eventoJsonFuturo != null) {
                bottomBarText = formatearFechaJson(eventoJsonFuturo.date) + " - " + eventoJsonFuturo.title;
            }

            // [ARREGLO 3]: PINTAR TEXTOS PRIMERO PARA QUE EL JSON ROTO NO BLOQUEE LA UI
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

            views.setTextViewText(R.id.txtMonth, month);
            views.setTextViewText(R.id.txtDay, day);

            if (showCornerText) {
                views.setViewVisibility(R.id.txtSpecialEvent, View.VISIBLE);
                views.setTextViewText(R.id.txtSpecialEvent, cornerText);
            } else {
                views.setViewVisibility(R.id.txtSpecialEvent, View.GONE);
            }

            int idEventoInf = context.getResources().getIdentifier("txtUserEvent", "id", context.getPackageName());
            int idLayoutInf = context.getResources().getIdentifier("layoutUserEvent", "id", context.getPackageName());

            if (idEventoInf != 0 && idLayoutInf != 0) {
                if (bottomBarText != null) {
                    views.setViewVisibility(idLayoutInf, View.VISIBLE);
                    views.setTextViewText(idEventoInf, bottomBarText);
                    Intent calendarIntent = new Intent(Intent.ACTION_VIEW);
                    calendarIntent.setData(Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()));
                    PendingIntent piCalendar = PendingIntent.getActivity(context, 1, calendarIntent, PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(idLayoutInf, piCalendar);
                } else {
                    views.setViewVisibility(idLayoutInf, View.GONE);
                }
            }

            Intent appIntent = new Intent(context, FullListActivity.class);
            appIntent.putExtra("TYPE", "widgets");
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent piApp = PendingIntent.getActivity(context, 301, appIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.rootContainer, piApp);

            // --- PINTAR LA IMAGEN CON PROTECCIÓN ANTI-CRASH ---
            int w = type.equals("2x2") ? 400 : 600;
            int h = type.equals("2x2") ? 400 : 300;
            int radius = type.equals("2x2") ? 30 : 40;

            com.bumptech.glide.load.Transformation<Bitmap> cropTransform = type.equals("2x2")
                    ? new com.bumptech.glide.load.resource.bitmap.CenterInside()
                    : new com.bumptech.glide.load.resource.bitmap.CenterCrop();

            RequestOptions options = new RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(w, h)
                    .transform(cropTransform, new RoundedCorners(radius));

            try {
                FutureTarget<Bitmap> futureTarget = Glide.with(context).asBitmap().load(imageUrl).apply(options).submit();
                Bitmap bitmap = futureTarget.get(10, java.util.concurrent.TimeUnit.SECONDS);
                views.setImageViewBitmap(R.id.widgetBgImage, bitmap);
                Glide.with(context).clear(futureTarget);

            } catch (Exception e) {
                // Si la imagen del JSON falla o no existe, pintamos un fondo por defecto
                e.printStackTrace();
                try {
                    int index = obtenerIndiceMatematico(prefs, id);
                    String fallbackUrl = GITHUB_BASE_URL + type + "/BG_W_" + String.format("%02d", index) + ".png";
                    FutureTarget<Bitmap> fbTarget = Glide.with(context).asBitmap().load(fallbackUrl).apply(options).submit();
                    Bitmap fbBitmap = fbTarget.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    views.setImageViewBitmap(R.id.widgetBgImage, fbBitmap);
                    Glide.with(context).clear(fbTarget);
                } catch (Exception ex) { ex.printStackTrace(); }
            }

            // Actualizamos siempre, sin importar si la imagen cargó
            appWidgetManager.updateAppWidget(id, views);
        }
    }

    private int obtenerIndiceMatematico(SharedPreferences prefs, int widgetId) {
        Set<String> favoritesSet = prefs.getStringSet("fav_wallpapers", new HashSet<>());
        List<String> favList = new ArrayList<>(favoritesSet);

        if (!favList.isEmpty()) {
            Collections.sort(favList);
            long timeBlock = System.currentTimeMillis() / (30 * 60 * 1000);
            int index = Math.abs((widgetId * 7) + (int)timeBlock) % favList.size();

            try {
                return Integer.parseInt(favList.get(index));
            } catch (Exception e) { return 1; }
        } else {
            return new Random().nextInt(20) + 1;
        }
    }

    private SpecialEvent buscarEventoJsonExacto(String todayKey) {
        try {
            JSONObject json = descargarJson();
            if (json != null && json.has("calendar_events")) {
                JSONArray events = json.getJSONArray("calendar_events");
                for (int i = 0; i < events.length(); i++) {
                    JSONObject e = events.getJSONObject(i);
                    if (e.getString("date").equals(todayKey)) {
                        return new SpecialEvent(e.getString("title"), e.getString("image"), todayKey);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private SpecialEvent buscarEventoJsonFuturo() {
        try {
            List<String> next7Days = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.US);
            for (int i = 0; i < 7; i++) {
                next7Days.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            JSONObject json = descargarJson();
            if (json != null && json.has("calendar_events")) {
                JSONArray events = json.getJSONArray("calendar_events");
                for (String dayToCheck : next7Days) {
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject e = events.getJSONObject(i);
                        if (e.getString("date").equals(dayToCheck)) {
                            return new SpecialEvent(e.getString("title"), e.getString("image"), dayToCheck);
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private JSONObject descargarJson() {
        try {
            URL url = new URL(JSON_URL + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return new JSONObject(result.toString());
        } catch(Exception e) { return null; }
    }

    private String formatearFechaJson(String dateJson) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("MM-dd", Locale.US);
            SimpleDateFormat outFormat = new SimpleDateFormat("MMMM dd", new Locale("es", "ES"));
            Calendar tempCal = Calendar.getInstance();
            Date dateObj = inFormat.parse(dateJson);
            if(dateObj != null) {
                Calendar current = Calendar.getInstance();
                tempCal.setTime(dateObj);
                tempCal.set(Calendar.YEAR, current.get(Calendar.YEAR));
                String fechaStr = outFormat.format(tempCal.getTime());
                return fechaStr.substring(0, 1).toUpperCase() + fechaStr.substring(1);
            }
        } catch (Exception e) { }
        return dateJson;
    }

    private UserCalendarEvent obtenerProximoEventoUsuarioObj(Context context) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            ContentResolver cr = context.getContentResolver();
            Uri uri = CalendarContract.Instances.CONTENT_URI;
            long start = System.currentTimeMillis();
            long diasFuturos = 30L;
            long end = start + (diasFuturos * 24 * 60 * 60 * 1000L);
            Uri.Builder builder = uri.buildUpon();
            ContentUris.appendId(builder, start);
            ContentUris.appendId(builder, end);

            // [ARREGLO 1]: AGREGAR 'ALL_DAY' PARA ARREGLAR LA ZONA HORARIA
            String[] projection = new String[]{
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY
            };
            String sortOrder = CalendarContract.Instances.BEGIN + " ASC LIMIT 1";

            try (Cursor cursor = cr.query(builder.build(), projection, null, null, sortOrder)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String titulo = cursor.getString(0);
                    long fechaMilis = cursor.getLong(1);
                    boolean isAllDay = cursor.getInt(2) == 1;

                    // VERIFICAR SI EL EVENTO ES HOY PARA EL PASTEL
                    SimpleDateFormat compareFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                    String todayStr = compareFormat.format(new Date());

                    SimpleDateFormat eventFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                    if (isAllDay) {
                        eventFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    }
                    String eventDateStr = eventFormat.format(new Date(fechaMilis));
                    boolean isToday = todayStr.equals(eventDateStr);

                    // FORMATEAR EL TEXTO DEL EVENTO PARA MOSTRARLO EN EL WIDGET
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd", new Locale("es", "ES"));
                    if (isAllDay) {
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    }
                    String fechaStr = sdf.format(new Date(fechaMilis));
                    if (!fechaStr.isEmpty()) fechaStr = fechaStr.substring(0, 1).toUpperCase() + fechaStr.substring(1);

                    return new UserCalendarEvent(titulo, fechaStr + " - " + titulo, isToday);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private static class SpecialEvent {
        String title, imageFile, date;
        SpecialEvent(String t, String i, String d) { title = t; imageFile = i; date = d; }
    }

    private static class UserCalendarEvent {
        String rawTitle;
        String formattedText;
        boolean isToday; // Agregado para el arreglo del fondo de cumpleaños
        UserCalendarEvent(String raw, String fmt, boolean today) {
            rawTitle = raw;
            formattedText = fmt;
            isToday = today;
        }
    }
}