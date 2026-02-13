package com.ketchupstudios.Switchstickerapp;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WallpaperDetailsActivity extends AppCompatActivity {

    private ImageView imgFull;
    private ImageView btnShare, btnFavorite;
    private TextView txtTitle, txtAuthor;
    private Button btnSet, btnSupport;
    private ProgressBar progressBar;

    // --- VARIABLES PARA TU DISEÑO EXISTENTE ---
    private CardView badgeNew;      // La etiqueta que ya tienes
    private TextView txtBadgeNew;   // El texto dentro de la etiqueta
    private View holoEffectView;    // El efecto holo que ya tienes
    // ------------------------------------------

    private LinearLayout layoutColors;
    private HorizontalScrollView scrollColors;

    private String imageUrl;
    private String artistLink;
    private String colorHexInicial;

    private Bitmap bitmapDescargado;
    private int colorFondoActual = Color.WHITE;

    private InterstitialAd mInterstitialAd;
    private boolean isAdLoading = false;

    // Variables de estado
    private boolean isLimited = false;
    private boolean isHidden = false;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_details);

        // 1. RECUPERAR DATOS
        String name = getIntent().getStringExtra("wall_name");
        String author = getIntent().getStringExtra("wall_author");
        imageUrl = getIntent().getStringExtra("wall_image");
        String transitionName = getIntent().getStringExtra("TRANSITION_NAME");

        isLimited = getIntent().getBooleanExtra("is_limited", false);
        isHidden = getIntent().getBooleanExtra("is_hidden", false);

        artistLink = getIntent().getStringExtra("wall_artist_link");
        if (artistLink == null || artistLink.isEmpty()) {
            artistLink = getIntent().getStringExtra("wall_link");
        }

        colorHexInicial = getIntent().getStringExtra("wall_color");

        // 2. VINCULAR VISTAS (IDS ORIGINALES)
        imgFull = findViewById(R.id.imgFullWallpaper);
        txtTitle = findViewById(R.id.txtWallTitle);
        txtAuthor = findViewById(R.id.txtWallAuthor);
        btnSet = findViewById(R.id.btnApplyWall);
        btnSupport = findViewById(R.id.btnSupportArtist);
        btnShare = findViewById(R.id.btnShareApp);

        // --- CORRECCIÓN ID: Usamos el estándar de la actividad ---
        btnFavorite = findViewById(R.id.btnFavorite);
        // --------------------------------------------------------

        // --- RECUPERAR TUS VISTAS DE DISEÑO ---
        // Asumo que en tu activity_wallpaper_details.xml tienes estos IDs
        // Si no los tienes, avísame, pero deberían estar si copiaste el diseño anterior.
        badgeNew = findViewById(R.id.badgeNew);
        txtBadgeNew = findViewById(R.id.txtBadgeNew);
        holoEffectView = findViewById(R.id.holoEffectView);
        // --------------------------------------

        progressBar = findViewById(R.id.progressWall);
        layoutColors = findViewById(R.id.layoutColorsContainer);
        scrollColors = findViewById(R.id.scrollColors);

        if (transitionName != null) imgFull.setTransitionName(transitionName);

        txtTitle.setText(name);
        txtAuthor.setText(author);

        try {
            if (colorHexInicial != null && !colorHexInicial.startsWith("#")) colorHexInicial = "#" + colorHexInicial;
            colorFondoActual = Color.parseColor(colorHexInicial);
        } catch (Exception e) { colorFondoActual = Color.WHITE; }
        imgFull.setBackgroundColor(colorFondoActual);

        // --- 3. LÓGICA VISUAL DE EVENTO (REUTILIZANDO TU ETIQUETA) ---
        if (isLimited || isHidden) {
            // A. ACTIVAR HOLO
            if (holoEffectView != null) holoEffectView.setVisibility(View.VISIBLE);

            // B. TRANSFORMAR ETIQUETA "NEW" EN "EVENT"
            if (badgeNew != null && txtBadgeNew != null) {
                badgeNew.setVisibility(View.VISIBLE);
                badgeNew.setCardBackgroundColor(Color.parseColor("#FFD700")); // Dorado
                txtBadgeNew.setText("EVENT");
                txtBadgeNew.setTextColor(Color.BLACK);
            }
        } else {
            // SI ES NORMAL
            if (holoEffectView != null) holoEffectView.setVisibility(View.GONE);
            // La etiqueta New se maneja normalmente (aquí la ocultamos por defecto si no es nueva)
            // O puedes dejar tu lógica de "isNew" aquí si la traes en el intent.
            if (badgeNew != null) badgeNew.setVisibility(View.GONE);
        }
        // -------------------------------------------------------------

        checkIfFavorite();

        btnFavorite.setOnClickListener(v -> {
            if (isFavorite) {
                if (isLimited || isHidden) mostrarAlertaBorrado();
                else toggleFavorite(false);
            } else {
                toggleFavorite(true);
            }
        });

        btnShare.setOnClickListener(v -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this wallpaper! https://play.google.com/store/apps/details?id=" + getPackageName());
                startActivity(Intent.createChooser(shareIntent, "Share..."));
            } catch(Exception e) { }
        });

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        progressBar.setVisibility(View.VISIBLE);

        Glide.with(this).asBitmap().load(baseUrl + "wallpappers/" + imageUrl)
                .listener(new RequestListener<Bitmap>() {
                    @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE); return false;
                    }
                    @Override public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        bitmapDescargado = resource;
                        extraerColoresDeImagen(resource);
                        iniciarAnimacionLiveZoom();
                        return false;
                    }
                }).into(imgFull);

        if (artistLink != null && !artistLink.isEmpty()) {
            btnSupport.setVisibility(View.VISIBLE);
            btnSupport.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(artistLink))));
        } else {
            btnSupport.setVisibility(View.GONE);
        }

        btnSet.setOnClickListener(v -> iniciarProcesoAplicar());
    }

    // --- MÉTODOS DE FAVORITOS ---
    private void checkIfFavorite() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favorites = prefs.getStringSet("fav_wallpapers_ids", new HashSet<>());
        if (imageUrl != null && favorites.contains(imageUrl)) {
            isFavorite = true;
            btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            btnFavorite.setColorFilter(Color.RED);
        } else {
            isFavorite = false;
            btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            btnFavorite.setColorFilter(Color.WHITE);
        }
    }

    private void toggleFavorite(boolean agregar) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favorites = new HashSet<>(prefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));
        if (imageUrl == null) return;

        if (agregar) {
            favorites.add(imageUrl);
            Toast.makeText(this, "Saved! ❤️", Toast.LENGTH_SHORT).show();
        } else {
            favorites.remove(imageUrl);
            Toast.makeText(this, "Removed.", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("fav_wallpapers_ids", favorites).apply();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (agregar) db.collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayUnion(imageUrl));
            else db.collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayRemove(imageUrl));
        }
        checkIfFavorite();
    }

    private void mostrarAlertaBorrado() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.layout_dialog_warning, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        view.findViewById(R.id.btnKeepIt).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnRemoveAnyway).setOnClickListener(v -> {
            toggleFavorite(false);
            dialog.dismiss();
        });
        dialog.show();
    }

    // --- MÉTODOS DE COLOR ---
    private void extraerColoresDeImagen(Bitmap bitmap) {
        if (bitmap == null) return;
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;
            List<Integer> candidatos = new ArrayList<>();

            // 1. [NUEVO] COLOR FAVORITO DEL USUARIO (PRIORIDAD MÁXIMA)
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            if (prefs.contains("user_favorite_color")) {
                candidatos.add(prefs.getInt("user_favorite_color", Color.BLACK));
            }


            try { candidatos.add(Color.parseColor("#e8dbd3")); candidatos.add(Color.parseColor("#2e2828")); } catch (Exception e) {}
            if (colorFondoActual != Color.WHITE) candidatos.add(colorFondoActual);
            if (palette.getVibrantSwatch() != null) candidatos.add(palette.getVibrantSwatch().getRgb());
            if (palette.getDarkVibrantSwatch() != null) candidatos.add(palette.getDarkVibrantSwatch().getRgb());
            if (palette.getDominantSwatch() != null) candidatos.add(palette.getDominantSwatch().getRgb());

            List<Integer> coloresFinales = new ArrayList<>();
            for (Integer c : candidatos) {
                boolean parecido = false;
                for (Integer a : coloresFinales) if (sonColoresParecidos(c, a)) parecido = true;
                if (!parecido) coloresFinales.add(c);
            }
            crearBotonesDeColor(coloresFinales);
        });
    }

    private boolean sonColoresParecidos(int c1, int c2) {
        double dist = Math.sqrt(Math.pow(Color.red(c2)-Color.red(c1),2) + Math.pow(Color.green(c2)-Color.green(c1),2) + Math.pow(Color.blue(c2)-Color.blue(c1),2));
        return dist < 40.0;
    }

    private void crearBotonesDeColor(List<Integer> colores) {
        layoutColors.removeAllViews();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        int radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        for (int color : colores) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.setMargins(0, 0, margin, 0);
            card.setLayoutParams(p);
            card.setCardBackgroundColor(color);
            card.setRadius(radius);
            card.setCardElevation(0);
            card.setOnClickListener(v -> {
                colorFondoActual = color;
                imgFull.setBackgroundColor(colorFondoActual);
            });
            layoutColors.addView(card);
        }
    }

    // --- MÉTODOS DE APLICAR ---
    private void iniciarProcesoAplicar() {
        if (bitmapDescargado == null) { Toast.makeText(this, "Wait...", Toast.LENGTH_SHORT).show(); return; }
        btnSet.setEnabled(false); btnSet.setText("Loading...");
        cargarAnuncioYMostrar();
    }

    private void cargarAnuncioYMostrar() {
        AdRequest req = new AdRequest.Builder().build();
        InterstitialAd.load(this, Config.ADMOB_INTERSTITIAL_ID, req, new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitialAd = ad;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        btnSet.setText("Apply"); btnSet.setEnabled(true); abrirSelectorFusionado();
                    }
                });
                mInterstitialAd.show(WallpaperDetailsActivity.this);
            }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                btnSet.setText("Apply"); btnSet.setEnabled(true); abrirSelectorFusionado();
            }
        });
    }

    private void abrirSelectorFusionado() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_wallpaper_options, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        ImageView h = view.findViewById(R.id.previewHome), l = view.findViewById(R.id.previewLock), b = view.findViewById(R.id.previewBoth);
        if (bitmapDescargado != null) {
            h.setBackgroundColor(colorFondoActual); h.setImageBitmap(bitmapDescargado);
            l.setBackgroundColor(colorFondoActual); l.setImageBitmap(bitmapDescargado);
            b.setBackgroundColor(colorFondoActual); b.setImageBitmap(bitmapDescargado);
        }
        view.findViewById(R.id.btnOptHome).setOnClickListener(v -> { procesarYAplicarFondo(0); dialog.dismiss(); });
        view.findViewById(R.id.btnOptLock).setOnClickListener(v -> { procesarYAplicarFondo(1); dialog.dismiss(); });
        view.findViewById(R.id.btnOptBoth).setOnClickListener(v -> { procesarYAplicarFondo(2); dialog.dismiss(); });
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void procesarYAplicarFondo(int op) {
        Toast.makeText(this, "Applying...", Toast.LENGTH_SHORT).show();
        btnSet.setEnabled(false);
        new Thread(() -> {
            try {
                Bitmap fin = Bitmap.createBitmap(bitmapDescargado.getWidth(), bitmapDescargado.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(fin); c.drawColor(colorFondoActual); c.drawBitmap(bitmapDescargado, 0, 0, null);
                WallpaperManager wm = WallpaperManager.getInstance(WallpaperDetailsActivity.this);
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    int flag = (op==0)? WallpaperManager.FLAG_SYSTEM : (op==1)? WallpaperManager.FLAG_LOCK : WallpaperManager.FLAG_SYSTEM|WallpaperManager.FLAG_LOCK;
                    wm.setBitmap(fin, null, true, flag);
                } else wm.setBitmap(fin);
                runOnUiThread(() -> { Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show(); btnSet.setEnabled(true); });
            } catch (Exception e) { runOnUiThread(() -> { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show(); btnSet.setEnabled(true); }); }
        }).start();
    }

    private void iniciarAnimacionLiveZoom() {
        imgFull.setScaleX(1.1f); imgFull.setScaleY(1.1f);
        imgFull.animate().scaleX(1f).scaleY(1f).setDuration(3000).setInterpolator(new DecelerateInterpolator()).start();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
    }
}