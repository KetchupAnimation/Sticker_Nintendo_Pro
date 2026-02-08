package com.ketchupstudios.Switchstickerapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsDetailActivity extends AppCompatActivity {

    private ImageView imgHeader;
    private TextView txtTitle, txtDate, txtCategory;
    private WebView webViewBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        imgHeader = findViewById(R.id.imgNewsHeader);
        txtTitle = findViewById(R.id.txtTitleFull);
        txtDate = findViewById(R.id.txtDateCategory);
        txtCategory = findViewById(R.id.txtCategoryTag);

        webViewBody = findViewById(R.id.webViewBody);
        webViewBody.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = webViewBody.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            cargarNoticiaDesdeUrl(data.toString());
        } else {
            String title = intent.getStringExtra("EXTRA_TITLE");
            String content = intent.getStringExtra("EXTRA_CONTENT");
            String imageUrl = intent.getStringExtra("EXTRA_IMAGE");
            String date = intent.getStringExtra("EXTRA_DATE");
            pintarDatos(title, content, imageUrl, date, "NOTICIA");
        }
        cargarBanner();
    }

    private void cargarNoticiaDesdeUrl(String fullUrl) {
        txtTitle.setText("Cargando noticia...");
        new Thread(() -> {
            try {
                String slug = "";
                Uri uri = Uri.parse(fullUrl);
                if (uri.getPathSegments().size() > 0) slug = uri.getLastPathSegment();

                String apiUrl = "https://nintendo.untal3d.com/wp-json/wp/v2/posts?_embed&slug=" + slug;
                if (slug == null || slug.isEmpty()) apiUrl = "https://nintendo.untal3d.com/wp-json/wp/v2/posts?_embed&per_page=1";

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray posts = new JSONArray(response.toString());
                if (posts.length() > 0) {
                    JSONObject post = posts.getJSONObject(0);

                    String title;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        title = Html.fromHtml(post.getJSONObject("title").getString("rendered"), Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        title = Html.fromHtml(post.getJSONObject("title").getString("rendered")).toString();
                    }

                    String content = post.getJSONObject("content").getString("rendered");
                    String date = post.getString("date").substring(0, 10);

                    String imgUrl = "";
                    // Prioridad A: Cabecera_
                    Pattern p = Pattern.compile("(https?:\\/\\/[^\\s\"']+\\/Cabecera_[^\\s\"']+\\.(?:jpg|jpeg|png|webp))", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(content);
                    if (m.find()) imgUrl = m.group(1);

                    // Prioridad B: Destacada
                    if (imgUrl.isEmpty() && post.has("_embedded")) {
                        JSONObject embedded = post.getJSONObject("_embedded");
                        if (embedded.has("wp:featuredmedia")) {
                            JSONArray media = embedded.getJSONArray("wp:featuredmedia");
                            if (media.length() > 0) imgUrl = media.getJSONObject(0).getString("source_url");
                        }
                    }

                    // Prioridad C: Cualquier src
                    if (imgUrl.isEmpty()) {
                        p = Pattern.compile("src=[\"']([^\"']+)[\"']");
                        m = p.matcher(content);
                        if (m.find()) imgUrl = m.group(1);
                    }

                    String finalTitle = title;
                    String finalContent = content;
                    String finalImg = imgUrl;
                    String finalDate = date;

                    new Handler(Looper.getMainLooper()).post(() ->
                            pintarDatos(finalTitle, finalContent, finalImg, finalDate, "WEB")
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void pintarDatos(String title, String content, String imageUrl, String date, String category) {
        txtTitle.setText(title);
        txtDate.setText(date);
        txtCategory.setText(category);

        if (content != null) {
            String viewport = "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>";

            // === CSS INTELIGENTE PARA GALERÍAS ===
            String css = "<style>" +
                    "@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap');" +
                    "body { font-family: 'Roboto', sans-serif; font-size: 16px; color: #333; margin: 0; padding: 0 10px; width: 100%; box-sizing: border-box; }" +

                    // REGLA GENERAL: Nada debe salirse del ancho
                    "* { max-width: 100%; box-sizing: border-box; }" +

                    // IMÁGENES NORMALES (SOLAS): 100% de ancho
                    "img { height: auto; border-radius: 8px; margin: 10px 0; }" +
                    // Si la imagen está sola en un párrafo, que ocupe todo
                    "p img { width: 100%; }" +

                    // === ARREGLO DE GALERÍAS (COLUMNAS) ===
                    // Detecta contenedores de galerías de WordPress (.gallery, .wp-block-gallery, etc)
                    ".gallery, .wp-block-gallery, .blocks-gallery-grid, figure.wp-block-gallery { " +
                    "   display: flex !important; " +
                    "   flex-wrap: wrap !important; " +
                    "   padding: 0 !important; " +
                    "   margin: 0 -5px !important; " + // Compensar márgenes
                    "}" +

                    // Los items individuales de la galería
                    ".gallery-item, .blocks-gallery-item, .wp-block-image { " +
                    "   width: 50% !important; " + // FORZAMOS 2 COLUMNAS (50% cada una)
                    "   padding: 0 5px !important; " + // Espacio entre fotos
                    "   margin: 0 0 10px 0 !important; " +
                    "   display: inline-block !important;" +
                    "   vertical-align: top !important;" +
                    "}" +

                    // Asegurar que la imagen dentro de la columna llene su espacio
                    ".gallery-item img, .blocks-gallery-item img { " +
                    "   width: 100% !important; " +
                    "   height: auto !important; " +
                    "}" +

                    // IFRAMES (Videos):
                    "iframe, video { width: 100% !important; height: auto; aspect-ratio: 16/9; }" +

                    "a { color: #E60012; text-decoration: none; font-weight: bold; }" +
                    "</style>";

            String htmlData = "<html><head>" + viewport + css + "</head><body>" + content + "</body></html>";
            webViewBody.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).asBitmap().load(imageUrl).centerCrop().into(imgHeader);
        } else {
            imgHeader.setImageResource(android.R.color.transparent);
            imgHeader.setBackgroundColor(0xFF333333);
        }
    }

    private void cargarBanner() {
        FrameLayout adContainer = findViewById(R.id.adContainerDetail);
        AdView adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-9087203932210009/7253353145");
        adView.setAdSize(AdSize.BANNER);
        adContainer.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}