package com.ketchupstudios.Switchstickerapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.StrictMode;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsWidget extends AppWidgetProvider {

    private static final String WP_API = "https://www.nintenderos.com/wp-json/wp/v2/posts?per_page=5&_embed";
    public static final String ACTION_MANUAL_REFRESH = "com.ketchupstudios.Switchstickerapp.ACTION_MANUAL_REFRESH_NEWS";
    private static final String PREFS_NAME = "WidgetNewsPrefs";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_MANUAL_REFRESH.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), NewsWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

            for (int appWidgetId : appWidgetIds) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_news_only);

                // --- 1. ESTADO DE CARGA (Al pulsar el botón) ---

                // Spinner visible, botón oculto
                views.setViewVisibility(R.id.loadingSpinner, View.VISIBLE);
                views.setViewVisibility(R.id.btnRefresh, View.GONE);

                // Texto de espera
                views.setTextViewText(R.id.txtNewsTitle, "CARGANDO...");

                // ¡AQUÍ ESTÁ LA LÍNEA RECUPERADA! Reseteamos el fondo al drawable rojo
                views.setImageViewResource(R.id.imgWidgetBackground, R.drawable.newsbg);

                // Ocultamos elementos que estorban (Sombra negra y Etiqueta pequeña)
                views.setViewVisibility(R.id.viewOverlay, View.GONE);
                views.setViewVisibility(R.id.containerCategory, View.GONE);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            // Iniciar la descarga
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_news_only);

        // --- 2. ESTADO DE CARGA (Al actualizar el sistema) ---
        views.setViewVisibility(R.id.loadingSpinner, View.VISIBLE);
        views.setViewVisibility(R.id.btnRefresh, View.GONE);
        views.setTextViewText(R.id.txtNewsTitle, "CARGANDO...");

        // ¡TAMBIÉN AQUÍ! Aseguramos que empiece con el fondo rojo limpio
        views.setImageViewResource(R.id.imgWidgetBackground, R.drawable.newsbg);

        // Ocultamos sombra y etiqueta
        views.setViewVisibility(R.id.viewOverlay, View.GONE);
        views.setViewVisibility(R.id.containerCategory, View.GONE);

        appWidgetManager.updateAppWidget(appWidgetId, views);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new Thread(() -> {
            boolean hasFreshNews = false;
            String newsTitle = "Cargando...";
            String newsCategory = "NOTICIA";
            String newsImgUrl = "";
            String newsLink = "";

            try {
                // ... CONEXIÓN (Sin cambios) ...
                URL url = new URL(WP_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray posts = new JSONArray(response.toString());
                if (posts.length() > 0) {
                    int indexToUse = 0;
                    try {
                        JSONObject latestPost = posts.getJSONObject(0);
                        String newestLink = latestPost.getString("link");
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        String lastLink = prefs.getString("last_shown_link", "");
                        boolean forceShuffle = false;
                        if (newestLink.equals(lastLink)) {
                            forceShuffle = true;
                        } else {
                            String dateGmt = latestPost.optString("date_gmt", "");
                            if (!dateGmt.isEmpty()) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date postDate = sdf.parse(dateGmt);
                                long now = System.currentTimeMillis();
                                long diff = now - postDate.getTime();
                                if (diff > 1 * 60 * 60 * 1000) forceShuffle = true;
                            }
                        }
                        if (forceShuffle) {
                            indexToUse = new Random().nextInt(posts.length());
                            JSONObject candidate = posts.getJSONObject(indexToUse);
                            if (candidate.getString("link").equals(lastLink)) {
                                indexToUse = (indexToUse + 1) % posts.length();
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }

                    JSONObject post = posts.getJSONObject(indexToUse);
                    hasFreshNews = true;
                    newsLink = post.getString("link");
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("last_shown_link", newsLink);
                    editor.apply();

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        newsTitle = Html.fromHtml(post.getJSONObject("title").getString("rendered"), Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        newsTitle = Html.fromHtml(post.getJSONObject("title").getString("rendered")).toString();
                    }

                    if (post.has("_embedded")) {
                        JSONObject embedded = post.getJSONObject("_embedded");
                        if (embedded.has("wp:featuredmedia")) {
                            JSONArray media = embedded.getJSONArray("wp:featuredmedia");
                            if (media.length() > 0) {
                                JSONObject mObj = media.getJSONObject(0);
                                if (mObj.has("source_url")) newsImgUrl = mObj.getString("source_url");
                            }
                        }
                        if (embedded.has("wp:term")) {
                            JSONArray terms = embedded.getJSONArray("wp:term");
                            if (terms.length() > 0) {
                                JSONArray cats = terms.getJSONArray(0);
                                if (cats.length() > 0) newsCategory = cats.getJSONObject(0).getString("name");
                            }
                        }
                    }
                    if (newsImgUrl.isEmpty() && post.has("content")) {
                        String content = post.getJSONObject("content").getString("rendered");
                        Pattern p = Pattern.compile("src=[\"']([^\"']+\\.(?:jpg|jpeg|png|webp))[\"']", Pattern.CASE_INSENSITIVE);
                        Matcher m = p.matcher(content);
                        if (m.find()) newsImgUrl = m.group(1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // --- 3. MOSTRAR RESULTADO ---
            String finalTitle = newsTitle;
            String finalCat = newsCategory;
            String finalImg = newsImgUrl;
            String finalLink = newsLink;

            if (hasFreshNews) {
                // EXITO: Quitamos spinner
                views.setViewVisibility(R.id.loadingSpinner, View.GONE);
                views.setViewVisibility(R.id.btnRefresh, View.VISIBLE);

                // Texto + Etiqueta Categoría Visible
                views.setTextViewText(R.id.txtNewsTitle, finalTitle);
                views.setTextViewText(R.id.txtCategoryTag, finalCat.toUpperCase());
                views.setViewVisibility(R.id.containerCategory, View.VISIBLE); // <--- Aparece la etiqueta

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalLink));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pi = PendingIntent.getActivity(context, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.imgWidgetBackground, pi);

                Intent refreshIntent = new Intent(context, NewsWidget.class);
                refreshIntent.setAction(ACTION_MANUAL_REFRESH);
                PendingIntent refreshPi = PendingIntent.getBroadcast(context, 202, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.btnRefresh, refreshPi);

                if (!finalImg.isEmpty()) {
                    try {
                        FutureTarget<Bitmap> futureTarget = Glide.with(context.getApplicationContext())
                                .asBitmap()
                                .load(finalImg)
                                .submit(800, 400);
                        Bitmap bitmap = futureTarget.get();

                        int defaultColor = Color.DKGRAY;
                        Palette p = Palette.from(bitmap).generate();
                        int vibrant = p.getVibrantColor(defaultColor);
                        int dominant = p.getDominantColor(defaultColor);
                        int finalColor = (vibrant != defaultColor) ? vibrant : dominant;
                        Bitmap coloredBg = crearFondoExacto(finalColor, finalCat.toUpperCase());

                        views.setImageViewBitmap(R.id.imgWidgetBackground, bitmap);
                        views.setImageViewBitmap(R.id.imgCategoryBg, coloredBg);
                        views.setTextColor(R.id.txtCategoryTag, esColorOscuro(finalColor) ? Color.WHITE : Color.BLACK);

                        // FOTO CARGADA -> PONEMOS SOMBRA
                        views.setViewVisibility(R.id.viewOverlay, View.VISIBLE);

                    } catch (Exception e) {
                        // FALLBACK: Volver al rojo
                        views.setImageViewResource(R.id.imgWidgetBackground, R.drawable.newsbg);
                        views.setViewVisibility(R.id.viewOverlay, View.GONE);
                    }
                } else {
                    // SIN FOTO: Volver al rojo
                    views.setImageViewResource(R.id.imgWidgetBackground, R.drawable.newsbg);
                    views.setViewVisibility(R.id.viewOverlay, View.GONE);
                }
            } else {
                // ERROR
                views.setViewVisibility(R.id.loadingSpinner, View.GONE);
                views.setViewVisibility(R.id.btnRefresh, View.VISIBLE);

                views.setTextViewText(R.id.txtNewsTitle, "Error de conexión...");
                views.setImageViewResource(R.id.imgWidgetBackground, R.drawable.newsbg);

                // En error, ocultamos etiqueta y sombra
                views.setViewVisibility(R.id.containerCategory, View.GONE);
                views.setViewVisibility(R.id.viewOverlay, View.GONE);
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }).start();
    }

    private static Bitmap crearFondoExacto(int color, String texto) {
        Paint textPaint = new Paint();
        textPaint.setTextSize(30f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        float anchoTexto = textPaint.measureText(texto);
        int anchoTotal = (int) (anchoTexto + 50);
        if (anchoTotal < 100) anchoTotal = 100;

        Bitmap bitmap = Bitmap.createBitmap(anchoTotal, 70, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paintBg = new Paint();
        paintBg.setColor(color);
        paintBg.setAntiAlias(true);
        canvas.drawRoundRect(new RectF(0, 0, anchoTotal, 70), 30f, 30f, paintBg);
        return bitmap;
    }

    private static boolean esColorOscuro(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
}