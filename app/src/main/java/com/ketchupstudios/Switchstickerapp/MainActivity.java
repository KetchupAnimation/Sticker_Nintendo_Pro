package com.ketchupstudios.Switchstickerapp;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// LIBRERÍAS EXTERNAS
import com.google.firebase.analytics.FirebaseAnalytics;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

// JSON PARSING
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// IMPORTS FIREBASE Y GOOGLE LOGIN
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;

public class MainActivity extends AppCompatActivity {

    // --- MONEDAS ---
    private CardView cardCoinContainer, cardMenuMain;
    private TextView txtCoinCount;

    // --- UI GENERAL ---
    private ImageView fabNews;
    private android.animation.ObjectAnimator levitateAnimator;
    private ProgressBar progressBar;
    private FrameLayout fragmentContainer;

    // Arriba, con las otras variables (ImageView navHome, etc.)
    private ImageView badgeNotification;
    private FrameLayout frameNavCard;

    // --- BOTONES DE NAVEGACIÓN ---
    private ImageView navHome, navStickers, navThemes, navSearch, navFavorites, navCardId, navInstagram;
    private FirebaseAnalytics mFirebaseAnalytics;

    // Variables para Deep Link
    private String pendingDeepLinkType = null;
    private String pendingDeepLinkId = null;

    // --- VARIABLES DE HOME ---
    private View viewHome;
    private ViewPager2 bannerViewPager;
    private LinearLayout layoutDots;
    private RecyclerView rvStickersHorizontal, rvWallpapersHorizontal, rvPremiumMixto, rvWidgetGallery, rvBatteryHorizontal;
    private WidgetGalleryAdapter widgetAdapter;
    private CardView cardLocationWarning, cardCalendarWarning, cardBatteryWarning;
    private Button btnEnableLocation, btnEnableCalendar, btnFixBattery;
    private TextView btnMoreStickers, btnMoreWallpapers, btnMorePremium, btnMoreBattery, btnViewAllWidgets;

    // --- LISTAS PERSISTENTES (Para que no se muevan al navegar) ---
    private List<StickerPack> finalHomeStickers = new ArrayList<>();
    private List<Config.Wallpaper> finalHomeWallpapers = new ArrayList<>();
    private List<Integer> finalHomeWidgets = new ArrayList<>();
    private List<Config.BatteryTheme> finalHomeBattery = new ArrayList<>();

    // --- VARIABLES DEL BANNER DE EVENTO ---
    private CardView cardDailyEvent;
    private ImageView imgDailyEventBg;
    private TextView txtEventDaysLeft;
    // ---------------------------------------------

    // --- VARIABLES DE LÓGICA ---
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private RewardedAd mRewardedAd;
    private boolean recompensaGanada = false;
    private Runnable accionPendiente;

    private FusedLocationProviderClient fusedLocationClient;

    // --- CONTROLADOR DEL BOTÓN ATRÁS Y DATOS GLOBALES ---
    private OnBackPressedCallback backCallback;
    private boolean isHome = true;
    private boolean isHomeInitialized = false; // Nos dirá si ya se mostró la animación inicial
    private Map<Integer, String> widgetStatusMap = new HashMap<>();

    // --- VARIABLES LOGIN ---
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    // --- VARIABLES PARA PUBLICIDAD INTERSTICIAL (WALLPAPERS) ---
    private com.google.android.gms.ads.interstitial.InterstitialAd mInterstitialAd;
    private int wallpaperClickCount = 0;
    private final int WALLPAPER_ADS_THRESHOLD = 3; // Mostrar anuncio cada 3 clicks

    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (bannerViewPager != null && bannerViewPager.getAdapter() != null) {
                int current = bannerViewPager.getCurrentItem();
                int total = bannerViewPager.getAdapter().getItemCount();
                int next = (current + 1) % total;
                bannerViewPager.setCurrentItem(next, true);
                sliderHandler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- FORZAR MODO CLARO PARA EVITAR BUGS DE COLOR ---
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
        // ---------------------------------------------------

        // --- RECUPERAR CACHÉ DE REACCIONES (INSTANTÁNEO) ---
        String cachedReactions = getSharedPreferences("AppData", MODE_PRIVATE).getString("cached_reactions", null);
        if (cachedReactions != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(cachedReactions);
                Config.reactions.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject r = jsonArray.getJSONObject(i);
                    Config.reactions.add(new Reaction(r.getString("id"), r.getString("image")));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        // ---------------------------------------------------

        MobileAds.initialize(this, initializationStatus -> {
            // Cargar el intersticial apenas inicie la publicidad
            cargarAnuncioIntersticial();
        });

        WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        // --- PEGA AQUÍ EL CÓDIGO QUE ESTABA EN ROJO ---
        // Inicializamos las vistas de la UI principal (Monedas y Menú)
        cardCoinContainer = findViewById(R.id.cardCoinContainer);
        cardMenuMain = findViewById(R.id.cardMenuMain);
        txtCoinCount = findViewById(R.id.txtCoinCount);
        // --- NUEVO: CLICK PARA EXPLICACIÓN DE MONEDAS ---
        if (cardCoinContainer != null) {
            cardCoinContainer.setOnClickListener(v -> mostrarExplicacionMonedas());
        }
        // -----------------------------------------------
        //  REGALO DE BIENVENIDA (3 MONEDAS INICIALES)
        // ---------------------------------------------------------
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        boolean esPrimeraVez = prefs.getBoolean("first_time_coins", true);

        if (esPrimeraVez) {
            // Le damos 3 monedas
            prefs.edit()
                    .putInt("skip_tickets", 999)
                    .putBoolean("first_time_coins", false) // Marcamos que ya recibió el regalo
                    .apply();

            // Opcional: Mostrar un mensajito
            Toast.makeText(this, "🎁 Welcome Gift: 9 Coins!", Toast.LENGTH_LONG).show();
        }
        // -------------------------------------------------------


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SOURCE, "fcm_notification");
            bundle.putString(FirebaseAnalytics.Param.MEDIUM, "push");
            bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, "novedades_stickers");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
        }

        if (getIntent() != null && getIntent().getExtras() != null) {
            pendingDeepLinkType = getIntent().getStringExtra("type");
            pendingDeepLinkId = getIntent().getStringExtra("id");
        }

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                mostrarVistaHome();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        pedirPermisoNotificaciones();
        MobileAds.initialize(this, initializationStatus -> {});

        sincronizarFavoritosDesdeNube();

        progressBar = findViewById(R.id.progressBarMain);
        fabNews = findViewById(R.id.fabNews);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        navHome = findViewById(R.id.navHome);
        navStickers = findViewById(R.id.navStickers);
        navThemes = findViewById(R.id.navThemes);
        navSearch = findViewById(R.id.navSearch);
        navFavorites = findViewById(R.id.navFavorites);
        navCardId = findViewById(R.id.navCardId);
        navInstagram = findViewById(R.id.navInstagram);

        // --- INICIALIZACIÓN DE LA CAMPANA Y EL FRAME ---
        navCardId = findViewById(R.id.navCardId);
        frameNavCard = findViewById(R.id.frameNavCard);
        badgeNotification = findViewById(R.id.badgeNotification);

        navHome.setOnClickListener(v -> mostrarVistaHome());
        navStickers.setOnClickListener(v -> mostrarVistaStickers());
        navThemes.setOnClickListener(v -> mostrarVistaThemes());
        navSearch.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
            overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
        });
        navFavorites.setOnClickListener(v -> mostrarVistaFavoritos());

        // Este es el único lugar donde debe estar el listener para esta acción.
        // --- LÓGICA CORRECTA DEL BOTÓN DE LA TARJETA (FRAME) ---
        frameNavCard.setOnClickListener(v -> {
            // 1. Apagado visual inmediato
            if (badgeNotification != null) {
                badgeNotification.setVisibility(View.GONE);
            }

            // 2. Guardado seguro en memoria
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("has_notification", false)
                    .putBoolean("wallet_visited", true) // <--- NUEVO: Marca que ya entró una vez
                    .apply();

            // 3. Navegación segura
            resetNavIcons();
            if (navCardId != null) {
                navCardId.setColorFilter(Color.parseColor("#4CAF50"));
            }

            startActivity(new Intent(MainActivity.this, WalletHomeActivity.class));
            overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
        });

        navInstagram.setOnClickListener(v -> abrirInstagram());

        viewHome = getLayoutInflater().inflate(R.layout.layout_home_content, null);

        Button btnRetry = viewHome.findViewById(R.id.btnRetryConnection);
        LinearLayout errorLayout = viewHome.findViewById(R.id.layoutConnectionError);
        androidx.core.widget.NestedScrollView scrollContent = viewHome.findViewById(R.id.homeScrollContent);

        btnRetry.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            new Thread(this::descargarJSON).start();
        });

        // --- AQUÍ EMPIEZA EL CAMBIO (Reemplazamos la carga automática) ---

        if (Config.dataLoaded && !Config.packs.isEmpty()) {
            // 1. SI YA TENEMOS DATOS: Usamos la memoria (Carga Instantánea)
            progressBar.setVisibility(View.GONE);

            // NOTA: Asegúrate de haber agregado 'promoData' a Config (ver abajo)
            prepararVistaHome(Config.promoData, Config.totalWidgetsCount, Config.widgetStatusMap);

        } else {
            // 2. SI NO TENEMOS DATOS: Descargamos normal
            new Thread(this::descargarJSON).start();
        }

        programarActualizacionAutomatica();

        resetNavIcons();
        if(navHome != null) navHome.setColorFilter(Color.parseColor("#ff4700"));
        isHome = true;

        try {
            Intent serviceIntent = new Intent(this, BatteryService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        getSharedPreferences("IdWalletPrefs", MODE_PRIVATE).edit().putString("user_uid", user.getUid()).apply();
                        sincronizarFavoritosDesdeNube();
                        mostrarVistaFavoritos();
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================================================
    // MÉTODOS DE NAVEGACIÓN
    // =========================================================

    private void resetNavIcons() {
        int colorInactive = Color.parseColor("#B0B0B0");

        // Verificamos cada icono por separado antes de cambiar su color
        if (navHome != null) navHome.setColorFilter(colorInactive);
        if (navStickers != null) navStickers.setColorFilter(colorInactive);
        if (navThemes != null) navThemes.setColorFilter(colorInactive);
        if (navSearch != null) navSearch.setColorFilter(colorInactive);
        if (navFavorites != null) navFavorites.setColorFilter(colorInactive);
        if (navCardId != null) navCardId.setColorFilter(colorInactive);
        if (navInstagram != null) navInstagram.setColorFilter(colorInactive);
    }

    private void mostrarVistaHome() {
        isHome = true;
        if (backCallback != null) backCallback.setEnabled(false);

        resetNavIcons();
        navHome.setColorFilter(Color.parseColor("#ff4700"));

        if (viewHome == null) return;

        // --- CAMBIAR LA VISTA ---
        fragmentContainer.removeAllViews();
        fragmentContainer.addView(viewHome);

        if (bannerViewPager != null && bannerViewPager.getAdapter() != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderHandler.postDelayed(sliderRunnable, 5000);
        }

        verificarYActualizarUbicacion();
        actualizarVisibilidadAlerta();

        // --- LÓGICA DE ANIMACIÓN INTELIGENTE ---
        if (!isHomeInitialized) {
            // Solo creamos y aplicamos la animación la PRIMERA VEZ
            int resId = R.anim.layout_animation_fall_down;
            android.view.animation.LayoutAnimationController animation =
                    android.view.animation.AnimationUtils.loadLayoutAnimation(this, resId);

            rvWidgetGallery.setLayoutAnimation(animation);
            rvStickersHorizontal.setLayoutAnimation(animation);
            rvWallpapersHorizontal.setLayoutAnimation(animation);
            rvBatteryHorizontal.setLayoutAnimation(animation);

            // Marcamos como inicializado para la próxima vez
            isHomeInitialized = true;
        } else {
            // Si ya se inicializó, quitamos las animaciones para que no parpadee al volver
            rvWidgetGallery.setLayoutAnimation(null);
            rvStickersHorizontal.setLayoutAnimation(null);
            rvWallpapersHorizontal.setLayoutAnimation(null);
            rvBatteryHorizontal.setLayoutAnimation(null);
        }

        // Actualizamos datos sin recrear adapters (Cero parpadeo)
        if (widgetAdapter != null) widgetAdapter.updateData();
        if (rvWallpapersHorizontal.getAdapter() != null) ((WallpaperAdapter) rvWallpapersHorizontal.getAdapter()).updateData();
        if (rvBatteryHorizontal.getAdapter() != null) ((BatteryThemeAdapter) rvBatteryHorizontal.getAdapter()).updateData();
    }

    private void mostrarVistaStickers() {
        isHome = false;
        if (backCallback != null) backCallback.setEnabled(true);

        resetNavIcons();
        navStickers.setColorFilter(Color.parseColor("#12a2cf"));

        View viewStickers = getLayoutInflater().inflate(R.layout.layout_simple_list, fragmentContainer, false);
        RecyclerView rv = viewStickers.findViewById(R.id.rvSimpleList);
        TextView title = viewStickers.findViewById(R.id.txtListTitle);
        ImageView banner = viewStickers.findViewById(R.id.imgListBanner);

        title.setText("All Stickers");
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        Glide.with(this).load(baseUrl + "banner_10.png").transform(new CenterCrop()).into(banner);

        List<StickerPack> allStickers = new ArrayList<>();
        List<StickerPack> priority = new ArrayList<>();
        List<StickerPack> normal = new ArrayList<>();

        if (Config.packs != null) {
            // Preparamos las herramientas de fecha para el filtro
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date today = new Date();

            for (StickerPack p : Config.packs) {

                // --- LÓGICA DE FILTRADO: OCULTAR SI EL EVENTO SIGUE ACTIVO ---
                if (p.isEvent) {
                    try {
                        Date startDate = sdf.parse(p.eventStartDate);
                        long diff = today.getTime() - startDate.getTime();
                        long daysPassed = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;

                        // Si los días que han pasado son MENORES o IGUALES al total de stickers,
                        // significa que el evento sigue en curso.
                        // Lo ocultamos de esta lista para que sea exclusivo del Banner.
                        if (daysPassed <= p.stickers.size()) {
                            continue;
                        }
                    } catch (Exception e) {
                        continue; // Si falla la fecha, mejor no mostrarlo por seguridad
                    }
                }
                // -------------------------------------------------------------

                if (p.status != null && (p.status.equalsIgnoreCase("new") || p.status.equalsIgnoreCase("updated"))) {
                    priority.add(p);
                } else {
                    normal.add(p);
                }
            }
        }
        allStickers.addAll(priority);
        allStickers.addAll(normal);

        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.setAdapter(new StickerPackAdapter(allStickers, this, R.layout.item_sticker_grid));

        fragmentContainer.removeAllViews();
        fragmentContainer.addView(viewStickers);
    }

    private void mostrarVistaThemes() {
        isHome = false;
        if (backCallback != null) backCallback.setEnabled(true);

        resetNavIcons();
        navThemes.setColorFilter(Color.parseColor("#b53939"));

        View viewThemes = getLayoutInflater().inflate(R.layout.layout_themes, fragmentContainer, false);

        ImageView banner = viewThemes.findViewById(R.id.imgListBanner);
        RecyclerView rvWall = viewThemes.findViewById(R.id.rvThemesWallpapers);
        RecyclerView rvWid = viewThemes.findViewById(R.id.rvThemesWidgets);
        RecyclerView rvBat = viewThemes.findViewById(R.id.rvThemesBattery);

        TextView btnMoreWall = viewThemes.findViewById(R.id.btnMoreThemesWall);
        TextView btnMoreWid = viewThemes.findViewById(R.id.btnMoreThemesWidget);
        TextView btnMoreBat = viewThemes.findViewById(R.id.btnMoreThemesBattery);

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        Glide.with(this).load(baseUrl + "banner_11.png").transform(new CenterCrop()).into(banner);

        // --- WALLPAPERS (CORREGIDO) ---
        List<Config.Wallpaper> wallLim = new ArrayList<>();
        List<Config.Wallpaper> wallNew = new ArrayList<>();
        List<Config.Wallpaper> wallNormal = new ArrayList<>();

        if (Config.wallpapers != null) {
            for (Config.Wallpaper w : Config.wallpapers) {
                if (w.isHidden) continue;
                if (w.isLimitedTime) {
                    if (!w.isExpired) {
                        wallLim.add(w);
                    }
                }
                else if (w.isNew) wallNew.add(w);
                else wallNormal.add(w);
            }
        }
        List<Config.Wallpaper> wallSorted = new ArrayList<>(wallLim);
        wallSorted.addAll(wallNew);
        wallSorted.addAll(wallNormal);

        rvWall.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        rvWall.setAdapter(new WallpaperAdapter(wallSorted, R.layout.item_wallpaper_fixed));

        // --- WIDGET THEMES ---
        android.content.SharedPreferences widgetPrefs = getSharedPreferences("WidgetPrefs", MODE_PRIVATE);
        int totalWidgets = widgetPrefs.getInt("count_4x2", 20);

        // --- AQUÍ ESTÁ EL TRUCO: USAMOS EL MAXIMO ENTRE EL LOCAL Y EL STATUS ---
        int maxWidgetId = totalWidgets;
        for(Integer id : widgetStatusMap.keySet()){
            if(id > maxWidgetId) maxWidgetId = id;
        }

        List<Integer> widLim = new ArrayList<>();
        List<Integer> widNew = new ArrayList<>();
        List<Integer> widNormal = new ArrayList<>();

        // Iteramos hasta el máximo encontrado (para incluir el 25 aunque total sea 23)
        for (int i = 1; i <= maxWidgetId; i++) {
            String status = widgetStatusMap.get(i);
            if (status != null && status.equalsIgnoreCase("limited")) widLim.add(i);
            else if (status != null && (status.equalsIgnoreCase("new") || status.equalsIgnoreCase("updated"))) widNew.add(i);
            else widNormal.add(i);
        }
        List<Integer> widSorted = new ArrayList<>(widLim);
        widSorted.addAll(widNew);
        widSorted.addAll(widNormal);

        rvWid.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false));
        rvWid.setAdapter(new WidgetGalleryAdapter(this, widSorted, true, widgetStatusMap));

        // --- BATTERY THEMES ---
        List<Config.BatteryTheme> batLim = new ArrayList<>();
        List<Config.BatteryTheme> batNew = new ArrayList<>();
        List<Config.BatteryTheme> batNormal = new ArrayList<>();
        if (Config.batteryThemes != null) {
            for (Config.BatteryTheme t : Config.batteryThemes) {
                if (t.isLimitedTime) batLim.add(t);
                else if (t.isNew) batNew.add(t);
                else batNormal.add(t);
            }
        }
        List<Config.BatteryTheme> batSorted = new ArrayList<>(batLim);
        batSorted.addAll(batNew);
        batSorted.addAll(batNormal);

        rvBat.setLayoutManager(new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false));
        rvBat.setAdapter(new BatteryThemeAdapter(this, batSorted, R.layout.item_battery_home, theme -> {
            Intent intent = new Intent(MainActivity.this, BatteryPreviewActivity.class);
            intent.putExtra("THEME_ID", theme.id);
            startActivity(intent);
        }));

        View.OnClickListener moreListener = v -> {
            Intent intent = new Intent(MainActivity.this, FullListActivity.class);
            if (v == btnMoreWall) intent.putExtra("TYPE", FullListActivity.TYPE_WALLPAPERS);
            else if (v == btnMoreWid) intent.putExtra("TYPE", FullListActivity.TYPE_WIDGETS);
            else if (v == btnMoreBat) intent.putExtra("TYPE", FullListActivity.TYPE_BATTERY);
            startActivity(intent);
        };
        btnMoreWall.setOnClickListener(moreListener);
        btnMoreWid.setOnClickListener(moreListener);
        btnMoreBat.setOnClickListener(moreListener);

        fragmentContainer.removeAllViews();
        fragmentContainer.addView(viewThemes);
    }

    private void mostrarVistaFavoritos() {
        isHome = false;
        if (backCallback != null) backCallback.setEnabled(true);

        resetNavIcons();
        navFavorites.setColorFilter(Color.parseColor("#ffc03a"));

        View viewFavs = getLayoutInflater().inflate(R.layout.layout_favorites, fragmentContainer, false);

        Button btnLogin = viewFavs.findViewById(R.id.btnLoginFavorites);
        if (btnLogin != null) {
            if (mAuth.getCurrentUser() != null) {
                btnLogin.setVisibility(View.GONE);
            } else {
                btnLogin.setVisibility(View.VISIBLE);
                btnLogin.setOnClickListener(v -> signIn());
            }
        }

        ImageView banner = viewFavs.findViewById(R.id.imgListBanner);
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        Glide.with(this).load(baseUrl + "banner_12.png").transform(new CenterCrop()).into(banner);

        RecyclerView rvWallpapers = viewFavs.findViewById(R.id.rvFavWallpapers);
        RecyclerView rvWidgets = viewFavs.findViewById(R.id.rvFavWidgets);
        RecyclerView rvBattery = viewFavs.findViewById(R.id.rvFavBattery);

        TextView txtTitleWall = viewFavs.findViewById(R.id.lblFavWallpapers);
        TextView txtTitleWid = viewFavs.findViewById(R.id.lblFavWidgets);
        TextView txtTitleBat = viewFavs.findViewById(R.id.lblFavBattery);
        TextView txtEmpty = viewFavs.findViewById(R.id.txtEmptyFavorites);

        android.content.SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favWallIds = appPrefs.getStringSet("fav_wallpapers_ids", new HashSet<>());
        List<Config.Wallpaper> favWallList = new ArrayList<>();
        if (Config.wallpapers != null) {
            for (Config.Wallpaper w : Config.wallpapers) {
                if (favWallIds.contains(w.imageFile)) favWallList.add(w);
            }
        }

        android.content.SharedPreferences widgetPrefs = getSharedPreferences("WidgetPrefs", MODE_PRIVATE);
        Set<String> favWidgetIds = widgetPrefs.getStringSet("fav_wallpapers", new HashSet<>());
        List<Integer> favWidgetList = new ArrayList<>();
        for (String id : favWidgetIds) {
            try { favWidgetList.add(Integer.parseInt(id)); } catch (NumberFormatException e) { }
        }

        Set<String> favBatIds = appPrefs.getStringSet("fav_battery_ids", new HashSet<>());
        List<Config.BatteryTheme> favBatList = new ArrayList<>();
        if (Config.batteryThemes != null) {
            for (Config.BatteryTheme t : Config.batteryThemes) {
                if (favBatIds.contains(t.id)) favBatList.add(t);
            }
        }

        boolean hasWall = !favWallList.isEmpty();
        boolean hasWid = !favWidgetList.isEmpty();
        boolean hasBat = !favBatList.isEmpty();
        boolean isEmptyAll = !hasWall && !hasWid && !hasBat;

        if (hasWall) {
            if (txtTitleWall != null) txtTitleWall.setVisibility(View.VISIBLE);
            rvWallpapers.setVisibility(View.VISIBLE);
            rvWallpapers.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
            rvWallpapers.setAdapter(new WallpaperAdapter(favWallList, R.layout.item_wallpaper_fixed, true));
        } else {
            if (txtTitleWall != null) txtTitleWall.setVisibility(View.GONE);
            rvWallpapers.setVisibility(View.GONE);
        }

        if (hasWid) {
            if (txtTitleWid != null) txtTitleWid.setVisibility(View.VISIBLE);
            rvWidgets.setVisibility(View.VISIBLE);
            rvWidgets.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false));
            rvWidgets.setAdapter(new WidgetGalleryAdapter(this, favWidgetList, true, widgetStatusMap, true));
        } else {
            if (txtTitleWid != null) txtTitleWid.setVisibility(View.GONE);
            rvWidgets.setVisibility(View.GONE);
        }

        if (hasBat) {
            if (txtTitleBat != null) txtTitleBat.setVisibility(View.VISIBLE);
            rvBattery.setVisibility(View.VISIBLE);
            rvBattery.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
            rvBattery.setAdapter(new BatteryThemeAdapter(this, favBatList, R.layout.item_battery_home, theme -> {
                Intent intent = new Intent(MainActivity.this, BatteryPreviewActivity.class);
                intent.putExtra("THEME_ID", theme.id);
                startActivity(intent);
            }, true));
        } else {
            if (txtTitleBat != null) txtTitleBat.setVisibility(View.GONE);
            rvBattery.setVisibility(View.GONE);
        }

        if (isEmptyAll) {
            if (txtEmpty != null) txtEmpty.setVisibility(View.VISIBLE);
        } else {
            if (txtEmpty != null) txtEmpty.setVisibility(View.GONE);
        }

        fragmentContainer.removeAllViews();
        fragmentContainer.addView(viewFavs);
    }

    private void abrirInstagram() {
        resetNavIcons();
        // Seguro: Verificar que navInstagram no sea nulo antes de pintar
        if (navInstagram != null) {
            navInstagram.setColorFilter(Color.BLACK);
        }

        String url = "https://www.instagram.com/untal3d/";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Instagram", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================
    // LÓGICA DE DATOS
    // =========================================================

    private void descargarJSON() {
        try {
            URL url = new URL(Config.STICKER_JSON_URL + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);

            JSONObject json = new JSONObject(result.toString());

            // ... (Resto de parsing de News, Promo, Banners igual) ...
            if (json.has("news")) {
                JSONObject n = json.getJSONObject("news");
                Config.newsData = new Config.News();
                Config.newsData.enabled = n.optBoolean("enabled", false);
                Config.newsData.versionId = n.optString("version_id", "v0");
                // --- LÍNEA VITAL: CAPTURAMOS EL CÓDIGO MÍNIMO ---
                Config.newsData.minVersionCode = n.optInt("min_version_code", 0);
                Config.newsData.text = n.optString("text", "");
                Config.newsData.iconImage = n.optString("icon_image", "");
                Config.newsData.bgImage = n.optString("bg_image", "");
                Config.newsData.overlayImage = n.optString("overlay_image", "");
                Config.newsData.closeImage = n.optString("close_image", "");
            }

            Config.Promo promoData = null;
            if (json.has("promo")) {
                JSONObject p = json.getJSONObject("promo");
                promoData = new Config.Promo();
                promoData.enabled = p.optBoolean("enabled", false);
                promoData.image = p.optString("image");
                promoData.title = p.optString("title");
                promoData.subtitle = p.optString("subtitle");
                promoData.link = p.optString("link");
                promoData.current = p.optInt("current", 0);
                promoData.goal = p.optInt("goal", 1);
                promoData.start = p.optInt("start", 0);
            }
            final Config.Promo finalPromo = promoData;

            Config.banners.clear();
            if (json.has("banners")) {
                JSONArray bannersArray = json.getJSONArray("banners");
                for (int i = 0; i < bannersArray.length(); i++) {
                    JSONObject b = bannersArray.getJSONObject(i);
                    Config.banners.add(new Config.Banner(b.optString("image_file"), b.optString("link_url")));
                }
            }

            JSONArray packsArray = json.has("sticker_packs") ? json.getJSONArray("sticker_packs") : json.optJSONArray("android");
            Config.packs.clear();
            if (packsArray != null) {
                for (int i = 0; i < packsArray.length(); i++) {
                    JSONObject obj = packsArray.getJSONObject(i);
                    StickerPack pack = new StickerPack();
                    pack.identifier = obj.getString("identifier");
                    pack.name = obj.getString("name");
                    pack.publisher = obj.getString("publisher");
                    pack.trayImageFile = obj.getString("tray_image_file");
                    pack.status = obj.optString("status");
                    pack.isPremium = obj.optBoolean("is_premium", false);
                    pack.artistLink = obj.optString("artist_link", "");
                    pack.updateNoteImage = obj.optString("update_note_image", "");
                    org.json.JSONArray tagsArray = obj.optJSONArray("tags");
                    if (tagsArray != null) {
                        for (int k = 0; k < tagsArray.length(); k++) pack.tags.add(tagsArray.getString(k).toLowerCase());
                    }
                    JSONArray stickersArray = obj.getJSONArray("stickers");
                    pack.stickers = new ArrayList<>();
                    for (int j = 0; j < stickersArray.length(); j++) {
                        JSONObject sObj = stickersArray.getJSONObject(j);
                        StickerPack.Sticker s = new StickerPack.Sticker();
                        s.imageFile = sObj.getString("image_file");
                        pack.stickers.add(s);
                    }

                    // --- NUEVA LÓGICA EVENTO DIARIO ---
                    if (obj.has("start_date")) {
                        pack.isEvent = true;
                        pack.eventStartDate = obj.getString("start_date");
                        pack.totalDays = obj.optInt("total_days", 0);
                        pack.eventBanner = obj.optString("banner_image", "");
                    }
                    // ----------------------------------

                    Config.packs.add(pack);
                }
            }

            Config.wallpapers.clear();
            if (json.has("wallpapers")) {
                JSONArray wallArray = json.getJSONArray("wallpapers");
                for (int i = 0; i < wallArray.length(); i++) {
                    JSONObject w = wallArray.getJSONObject(i);
                    Config.Wallpaper wall = new Config.Wallpaper(w.getString("identifier"), w.getString("name"), w.getString("image_file"), w.getString("publisher"), w.optBoolean("is_new", false), w.optBoolean("is_premium", false), w.optString("artist_link", ""));
                    JSONArray tagsArray = w.optJSONArray("tags");
                    if (tagsArray != null) {
                        for (int k = 0; k < tagsArray.length(); k++) wall.tags.add(tagsArray.getString(k).toLowerCase());
                    }
                    wall.colorBg = w.optString("ColorBG", "#FFFFFF");
                    wall.isHidden = w.optBoolean("is_hidden", false);
                    wall.rewardDay = w.optString("day", "");
                    Config.wallpapers.add(wall);
                }
            }

            try {
                String batteryJsonUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
                URL urlBat = new URL(batteryJsonUrl + "?t=" + System.currentTimeMillis());
                HttpURLConnection connBat = (HttpURLConnection) urlBat.openConnection();
                BufferedReader readerBat = new BufferedReader(new InputStreamReader(connBat.getInputStream()));
                StringBuilder resBat = new StringBuilder();
                String lineBat;
                while ((lineBat = readerBat.readLine()) != null) resBat.append(lineBat);
                JSONObject jsonBat = new JSONObject(resBat.toString());
                JSONArray arrayBat = jsonBat.getJSONArray("themes");
                Config.batteryThemes.clear();
                for (int i = 0; i < arrayBat.length(); i++) {
                    JSONObject o = arrayBat.getJSONObject(i);
                    // AQUÍ ESTÁ LA CORRECCIÓN: 7 PARÁMETROS
                    Config.batteryThemes.add(new Config.BatteryTheme(
                            o.getString("id"),
                            o.getString("name"),
                            o.getString("folder"),
                            o.getString("color_bg"),
                            o.getString("text_color"),
                            o.optBoolean("is_new", false),
                            o.optString("artist_link", "") // <--- 7. EL LINK NUEVO
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }

            // 1. CAPTURAR EL TOTAL
            int tempCount = json.optInt("4x2", 20);

            widgetStatusMap = new HashMap<>();
            JSONObject statusJson = json.optJSONObject("status_4x2");
            if (statusJson != null) {
                Iterator<String> keys = statusJson.keys();
                while (keys.hasNext()) {
                    String keyId = keys.next();
                    String valueStatus = statusJson.getString(keyId);
                    try { widgetStatusMap.put(Integer.parseInt(keyId), valueStatus); } catch (Exception e) {}
                }
            }

            // 2. PROCESAR LIMITADOS
            if (json.has("limited_events")) {
                JSONArray events = json.getJSONArray("limited_events");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = sdf.format(new Date());

                // NOTA: Ya no necesitamos leer 'favWalls' aquí porque cargaremos todo.

                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);
                    String date = event.getString("date");
                    String type = event.getString("type");
                    JSONObject data = event.getJSONObject("data");

                    boolean isToday = date.equals(today);

                    if (type.equals("wallpaper")) {
                        // --- CAMBIO CLAVE: SIEMPRE LO AGREGAMOS ---
                        Config.Wallpaper w = new Config.Wallpaper(
                                data.getString("identifier"),
                                data.getString("name"),
                                data.getString("image_file"),
                                data.getString("publisher"),
                                false, false, data.optString("artist_link", "")
                        );
                        w.colorBg = data.optString("ColorBG", "#FFFFFF");

                        w.isLimitedTime = true;     // Es de evento
                        w.isExpired = !isToday;     // Si la fecha no es hoy, está EXPIRADO

                        // Lo agregamos a la lista global.
                        // El filtro en 'prepararVistaHome' se encargará de ocultarlo del inicio.
                        Config.wallpapers.add(w);
                    }
                    else if (type.equals("battery")) {
                        // Misma lógica para baterías: Cargar siempre, marcar expirado si no es hoy
                        Config.BatteryTheme b = new Config.BatteryTheme(
                                data.getString("id"),
                                data.getString("name"),
                                data.getString("folder"),
                                data.getString("color_bg"),
                                data.getString("text_color"),
                                false,
                                data.optString("artist_link", "")
                        );
                        b.isLimitedTime = true;
                        b.isExpired = !isToday;
                        Config.batteryThemes.add(b);
                    }
                    else if (type.equals("widget") && isToday) {
                        // Los widgets sí se manejan solo si es hoy por la lógica del mapa de status
                        try {
                            int id = data.getInt("id");
                            widgetStatusMap.put(id, "limited");
                            if (id > tempCount) tempCount = id;
                        } catch (Exception e){}
                    }
                }
            }

            final int finalTotalCount = tempCount;


            Config.totalWidgetsCount = tempCount;
            Config.widgetStatusMap.putAll(widgetStatusMap);
            Config.promoData = promoData;  // <--- ESTA ES LA CLAVE para que no parpadee
            Config.dataLoaded = true;      // <--- Confirma que ya hay datos


            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                verificarActualizacionForzada();
                checkAndShowNews();
                // Usa las variables de Config en lugar de las locales
                prepararVistaHome(finalPromo, Config.totalWidgetsCount, Config.widgetStatusMap);
            });


            final Map<Integer, String> finalStatusMap = widgetStatusMap;

            // --- NUEVO: PARSEAR REACCIONES ---
            Config.reactions.clear();
            if (json.has("reactions")) {
                JSONArray reactArray = json.getJSONArray("reactions");
                Config.reactions.clear(); // Limpiamos lista en memoria

                // --- NUEVO: GUARDAR EN CACHÉ ---
                getSharedPreferences("AppData", MODE_PRIVATE)
                        .edit()
                        .putString("cached_reactions", reactArray.toString()) // Guardamos el JSON crudo
                        .apply();
                // -------------------------------
                for (int i = 0; i < reactArray.length(); i++) {
                    JSONObject r = reactArray.getJSONObject(i);
                    Config.reactions.add(new Reaction(
                            r.getString("id"),
                            r.getString("image")
                    ));
                }
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                // 🔥 AGREGA ESTA LÍNEA AQUÍ
                verificarActualizacionForzada();
                checkAndShowNews();
                prepararVistaHome(finalPromo, finalTotalCount, finalStatusMap);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (fragmentContainer.getChildCount() == 0 || fragmentContainer.getChildAt(0) != viewHome) {
                    fragmentContainer.removeAllViews();
                    fragmentContainer.addView(viewHome);
                }
                LinearLayout errorLayout = viewHome.findViewById(R.id.layoutConnectionError);
                androidx.core.widget.NestedScrollView scrollContent = viewHome.findViewById(R.id.homeScrollContent);
                if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
                if (scrollContent != null) scrollContent.setVisibility(View.GONE);
            });
        }
    }



    private void prepararVistaHome(Config.Promo promo, int totalCount, Map<Integer, String> finalStatusMap) {
        // 1. Ocultar Errores y Mostrar Contenido
        LinearLayout errorLayout = viewHome.findViewById(R.id.layoutConnectionError);
        androidx.core.widget.NestedScrollView scrollContent = viewHome.findViewById(R.id.homeScrollContent);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        if (scrollContent != null) scrollContent.setVisibility(View.VISIBLE);

        // 2. Inicializar Vistas (Referencias)
        bannerViewPager = viewHome.findViewById(R.id.bannerViewPager);
        layoutDots = viewHome.findViewById(R.id.layoutDots);
        if (layoutDots != null) layoutDots.setVisibility(View.GONE);

        rvStickersHorizontal = viewHome.findViewById(R.id.rvStickersHorizontal);
        rvWidgetGallery = viewHome.findViewById(R.id.rvWidgetGallery);
        rvWallpapersHorizontal = viewHome.findViewById(R.id.rvWallpapersHorizontal);
        rvPremiumMixto = viewHome.findViewById(R.id.rvPremiumMixto);
        rvBatteryHorizontal = viewHome.findViewById(R.id.rvBatteryHorizontal);

        // --- CONFIGURAR LAYOUT MANAGERS (UNA SOLA VEZ) ---
        rvWidgetGallery.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        rvStickersHorizontal.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        rvWallpapersHorizontal.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        rvBatteryHorizontal.setLayoutManager(new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false));

        // Inicializar botones y listeners (Siempre se asignan para seguridad)
        View cardSearch = viewHome.findViewById(R.id.cardSearchFake);
        if (cardSearch != null) {
            cardSearch.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
                overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
            });
        }

        btnMoreStickers = viewHome.findViewById(R.id.btnMoreStickers);
        btnMoreWallpapers = viewHome.findViewById(R.id.btnMoreWallpapers);
        btnMorePremium = viewHome.findViewById(R.id.btnMorePremium);
        btnMoreBattery = viewHome.findViewById(R.id.btnMoreBattery);
        btnViewAllWidgets = viewHome.findViewById(R.id.btnViewAllWidgets);

        cardLocationWarning = viewHome.findViewById(R.id.cardLocationWarning);
        btnEnableLocation = viewHome.findViewById(R.id.btnEnableLocation);
        cardCalendarWarning = viewHome.findViewById(R.id.cardCalendarWarning);
        btnEnableCalendar = viewHome.findViewById(R.id.btnEnableCalendar);
        cardBatteryWarning = viewHome.findViewById(R.id.cardBatteryWarning);
        btnFixBattery = viewHome.findViewById(R.id.btnFixBattery);

        AdView adBot = viewHome.findViewById(R.id.adViewBottom);
        if (adBot != null) adBot.loadAd(new AdRequest.Builder().build());

        View.OnClickListener moreListener = v -> {
            Intent intent = new Intent(MainActivity.this, FullListActivity.class);
            if (v == btnMoreStickers) intent.putExtra("TYPE", FullListActivity.TYPE_STICKERS);
            else if (v == btnMoreWallpapers) intent.putExtra("TYPE", FullListActivity.TYPE_WALLPAPERS);
            else if (v == btnMorePremium) intent.putExtra("TYPE", FullListActivity.TYPE_PREMIUM);
            else if (v == btnMoreBattery) intent.putExtra("TYPE", FullListActivity.TYPE_BATTERY);
            else if (v == btnViewAllWidgets) intent.putExtra("TYPE", FullListActivity.TYPE_WIDGETS);
            startActivity(intent);
        };
        btnMoreStickers.setOnClickListener(moreListener);
        btnMoreWallpapers.setOnClickListener(moreListener);
        if (btnMorePremium != null) btnMorePremium.setOnClickListener(moreListener);
        if (btnMoreBattery != null) btnMoreBattery.setOnClickListener(moreListener);
        if (btnViewAllWidgets != null) btnViewAllWidgets.setOnClickListener(moreListener);

        btnEnableLocation.setOnClickListener(v -> verificarYActualizarUbicacion());
        btnEnableCalendar.setOnClickListener(v -> androidx.core.app.ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_CALENDAR}, 103));
        if (btnFixBattery != null) {
            btnFixBattery.setOnClickListener(v -> {
                try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } catch (Exception e) { Toast.makeText(this, "Go to Settings -> Battery", Toast.LENGTH_LONG).show(); }
            });
        }

        // --- LÓGICA DE EVENTO Y BANNERS ---
        StickerPack activeEventPack = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();

        if (Config.packs != null) {
            for (StickerPack p : Config.packs) {
                if (p.isEvent) {
                    try {
                        Date startDate = sdf.parse(p.eventStartDate);
                        long diff = today.getTime() - startDate.getTime();
                        long daysPassed = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
                        if (daysPassed <= p.stickers.size()) {
                            activeEventPack = p;
                            break;
                        }
                    } catch (Exception e) { }
                }
            }
        }

        View cardMainBannerContainer = viewHome.findViewById(R.id.cardMainBannerContainer);
        cardDailyEvent = viewHome.findViewById(R.id.cardDailyEvent);
        imgDailyEventBg = viewHome.findViewById(R.id.imgDailyEventBg);
        txtEventDaysLeft = viewHome.findViewById(R.id.txtEventDaysLeft);

        if (activeEventPack != null && cardDailyEvent != null) {
            cardDailyEvent.setVisibility(View.VISIBLE);
            if (cardMainBannerContainer != null) cardMainBannerContainer.setVisibility(View.GONE);
            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this).load(baseUrl + activeEventPack.identifier + "/" + activeEventPack.eventBanner).transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop()).into(imgDailyEventBg);
            }
            try {
                Date startDate = sdf.parse(activeEventPack.eventStartDate);
                long diff = today.getTime() - startDate.getTime();
                long daysPassed = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
                long daysLeft = activeEventPack.stickers.size() - daysPassed;
                if (daysLeft < 0) daysLeft = 0;
                txtEventDaysLeft.setText(daysLeft + " DAYS LEFT");
            } catch (Exception e) { txtEventDaysLeft.setText("EVENT"); }
            final StickerPack finalPack = activeEventPack;
            cardDailyEvent.setOnClickListener(v -> abrirPantallaDetalles(finalPack));
        } else {
            if (cardDailyEvent != null) cardDailyEvent.setVisibility(View.GONE);
            if (cardMainBannerContainer != null) cardMainBannerContainer.setVisibility(View.VISIBLE);
        }

        // --- PREPARACIÓN DE DATOS (ANTI RE-SHUFFLE) ---
        // Solo barajamos si las listas están vacías (primera carga)
        if (finalHomeStickers.isEmpty()) {
            List<StickerPack> stickersVip = new ArrayList<>();
            List<StickerPack> stickersComunes = new ArrayList<>();
            if (Config.packs != null) {
                for (StickerPack p : Config.packs) {
                    if (activeEventPack != null && p == activeEventPack) continue;
                    if (p.status != null && (p.status.equalsIgnoreCase("new") || p.status.equalsIgnoreCase("updated"))) stickersVip.add(p);
                    else stickersComunes.add(p);
                }
            }
            Collections.shuffle(stickersComunes);
            finalHomeStickers.clear();
            finalHomeStickers.addAll(stickersVip);
            finalHomeStickers.addAll(stickersComunes);
            if (finalHomeStickers.size() > 12) finalHomeStickers = new ArrayList<>(finalHomeStickers.subList(0, 12));
        }

        if (finalHomeWidgets.isEmpty()) {
            List<Integer> wLim = new ArrayList<>(), wPri = new ArrayList<>(), wNor = new ArrayList<>();
            for (int i = 1; i <= totalCount; i++) {
                String status = finalStatusMap.get(i);
                if (status != null && status.equalsIgnoreCase("limited")) wLim.add(i);
                else if (status != null && (status.equalsIgnoreCase("new") || status.equalsIgnoreCase("updated"))) wPri.add(i);
                else wNor.add(i);
            }
            Collections.shuffle(wNor);
            finalHomeWidgets.clear();
            finalHomeWidgets.addAll(wLim); finalHomeWidgets.addAll(wPri); finalHomeWidgets.addAll(wNor);
            int limitW = Math.min(finalHomeWidgets.size(), 8);
            finalHomeWidgets = new ArrayList<>(finalHomeWidgets.subList(0, limitW));
        }

        if (finalHomeWallpapers.isEmpty()) {
            List<Config.Wallpaper> wallLim = new ArrayList<>(), wallVip = new ArrayList<>(), wallCom = new ArrayList<>();
            if (Config.wallpapers != null) {
                for (Config.Wallpaper w : Config.wallpapers) {
                    if (w.isHidden) continue;
                    if (w.isLimitedTime) { if (!w.isExpired) wallLim.add(w); }
                    else if (w.isNew) wallVip.add(w);
                    else wallCom.add(w);
                }
            }
            Collections.shuffle(wallCom);
            finalHomeWallpapers.clear();
            finalHomeWallpapers.addAll(wallLim); finalHomeWallpapers.addAll(wallVip); finalHomeWallpapers.addAll(wallCom);
            if (finalHomeWallpapers.size() > 10) finalHomeWallpapers = new ArrayList<>(finalHomeWallpapers.subList(0, 10));
        }

        if (finalHomeBattery.isEmpty()) {
            List<Config.BatteryTheme> batLim = new ArrayList<>(), batNew = new ArrayList<>(), batNor = new ArrayList<>();
            if (Config.batteryThemes != null) {
                for (Config.BatteryTheme t : Config.batteryThemes) {
                    if (t.isLimitedTime) { if (!t.isExpired) batLim.add(t); }
                    else if (t.isNew) batNew.add(t);
                    else batNor.add(t);
                }
            }
            Collections.shuffle(batNor);
            finalHomeBattery.clear();
            finalHomeBattery.addAll(batLim); finalHomeBattery.addAll(batNew); finalHomeBattery.addAll(batNor);
            if (finalHomeBattery.size() > 6) finalHomeBattery = new ArrayList<>(finalHomeBattery.subList(0, 6));
        }

        // --- ASIGNACIÓN DE ADAPTADORES (ANTI-PARPADEO) ---


        int resId = R.anim.layout_animation_fall_down;
        android.view.animation.LayoutAnimationController animation = android.view.animation.AnimationUtils.loadLayoutAnimation(this, resId);

        if (!isHomeInitialized) {
            // Solo creamos y animamos la primera vez
            // 1. STICKERS
            if (rvStickersHorizontal.getAdapter() == null) {
                // Si no tiene adaptador, creamos uno nuevo
                rvStickersHorizontal.setAdapter(new StickerPackAdapter(finalHomeStickers, this, R.layout.item_sticker_mini));
            } else {
                // Si YA tiene adaptador, solo le avisamos que los datos están listos (sin recrear)
                rvStickersHorizontal.getAdapter().notifyDataSetChanged();
            }

            // 2. WALLPAPERS
            if (rvWallpapersHorizontal.getAdapter() == null) {
                rvWallpapersHorizontal.setAdapter(new WallpaperAdapter(finalHomeWallpapers, R.layout.item_wallpaper_fixed));
            } else {
                ((WallpaperAdapter) rvWallpapersHorizontal.getAdapter()).updateData();
            }

            // 3. WIDGETS
            if (rvWidgetGallery.getAdapter() == null) {
                widgetAdapter = new WidgetGalleryAdapter(MainActivity.this, finalHomeWidgets, true, widgetStatusMap);
                rvWidgetGallery.setAdapter(widgetAdapter);
            } else {
                if (widgetAdapter != null) widgetAdapter.updateData();
            }

            // 4. BATERÍA (Esto es lo que faltaba para que se vean)
            if (rvBatteryHorizontal.getAdapter() == null) {
                rvBatteryHorizontal.setAdapter(new BatteryThemeAdapter(this, finalHomeBattery, R.layout.item_battery_home, theme -> {
                    Intent intent = new Intent(MainActivity.this, BatteryPreviewActivity.class);
                    intent.putExtra("THEME_ID", theme.id);
                    startActivity(intent);
                }));
            } else {
                ((BatteryThemeAdapter) rvBatteryHorizontal.getAdapter()).updateData();
            }

            isHomeInitialized = true;
        } else {
            // Si ya existe, solo actualizamos los datos suavemente (sin animación de caída)
            if (widgetAdapter != null) widgetAdapter.updateData();
            if (rvStickersHorizontal.getAdapter() != null) rvStickersHorizontal.getAdapter().notifyDataSetChanged();
            if (rvWallpapersHorizontal.getAdapter() != null) ((WallpaperAdapter) rvWallpapersHorizontal.getAdapter()).updateData();
            if (rvBatteryHorizontal.getAdapter() != null) ((BatteryThemeAdapter) rvBatteryHorizontal.getAdapter()).updateData();
        }

        if (!Config.banners.isEmpty()) setupSlider();
        configurarPromo(promo, viewHome);
        rvPremiumMixto.setVisibility(View.GONE);

        mostrarVistaHome();
        procesarDeepLinkPendiente();
    }



    private void configurarPromo(Config.Promo promo, View parentView) {
        CardView cardPromo = parentView.findViewById(R.id.cardPromo);
        if (promo == null || !promo.enabled || cardPromo == null) {
            if (cardPromo != null) cardPromo.setVisibility(View.GONE);
            return;
        }
        cardPromo.setVisibility(View.VISIBLE);
        ImageView imgBg = parentView.findViewById(R.id.imgPromoBg);
        TextView txtTitle = parentView.findViewById(R.id.txtPromoTitle);
        TextView txtPercent = parentView.findViewById(R.id.txtPromoPercent);
        TextView txtSubtitle = parentView.findViewById(R.id.txtPromoSubtitle);
        if (txtTitle != null) txtTitle.setText(promo.title);
        if (txtSubtitle != null) txtSubtitle.setText(promo.subtitle);
        int rango = Math.max(1, promo.goal - promo.start);
        int progreso = Math.max(0, promo.current - promo.start);
        int porcentaje = Math.min(100, (progreso * 100) / rango);
        if (txtPercent != null) txtPercent.setText(porcentaje + "%");
        if (promo.image != null && !promo.image.isEmpty() && imgBg != null) {
            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            Glide.with(this).load(baseUrl + promo.image).transform(new CenterCrop()).into(imgBg);
        }
        cardPromo.setOnClickListener(v -> {
            if (promo.link != null && !promo.link.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(promo.link)));
                } catch (Exception e) {
                }
            }
        });
    }


    private void setupSlider() {
        if(bannerViewPager == null) return;
        bannerViewPager.setAdapter(new BannerAdapter(Config.banners));
        bannerViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        crearPuntitos(Config.banners.size());
        actualizarPuntitos(0);
        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { actualizarPuntitos(position); }
        });
        sliderHandler.postDelayed(sliderRunnable, 5000);
    }

    private void crearPuntitos(int count) {
        if(layoutDots == null) return;
        layoutDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 50);
            params.setMargins(10, 0, 10, 0);
            dot.setLayoutParams(params);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(0x80808080);
            dot.setImageDrawable(drawable);
            layoutDots.addView(dot);
        }
    }

    private void actualizarPuntitos(int position) {
        if(layoutDots == null) return;
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            ImageView dot = (ImageView) layoutDots.getChildAt(i);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            if (i == position) {
                drawable.setColor(0xFF000000);
                drawable.setSize(12, 12);
            } else {
                drawable.setColor(0x80808080);
                drawable.setSize(5, 5);
            }
            dot.setImageDrawable(drawable);
        }
    }

    public void cargarAnuncioYEjecutar(Runnable accion) {
        accionPendiente = accion;
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Loading Premium Ad...");
        pd.setCancelable(false);
        pd.show();
        String idVideoReal = "ca-app-pub-9087203932210009/4573750306";
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, idVideoReal, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                pd.dismiss();
                if (accionPendiente != null) accionPendiente.run();
            }
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                pd.dismiss();
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mRewardedAd = null;
                        if (recompensaGanada && accionPendiente != null) {
                            accionPendiente.run();
                        }
                        recompensaGanada = false;
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        mRewardedAd = null;
                        if (accionPendiente != null) accionPendiente.run();
                    }
                });
                recompensaGanada = false;
                mRewardedAd.show(MainActivity.this, rewardItem -> recompensaGanada = true);
            }
        });
    }

    // 1. CARGAR EL ANUNCIO EN SEGUNDO PLANO (Para que esté listo rápido)
    private void cargarAnuncioIntersticial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        // Usamos el ID que tienes en Config
        com.google.android.gms.ads.interstitial.InterstitialAd.load(this, Config.ADMOB_INTERSTITIAL_ID, adRequest,
                new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    // 2. LÓGICA DE NAVEGACIÓN INTELIGENTE
    // ===============================================================
    // LÓGICA DE MONEDAS VS ANUNCIOS (CON PREGUNTA)
    // ===============================================================

    public void intentarAbrirWallpaperConIntersticial(Config.Wallpaper wall) {
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COSTO = 3;

        // CASO A: TIENE SUFICIENTES MONEDAS -> USAMOS EL DIÁLOGO NUEVO
        if (tickets >= COSTO) {
            mostrarDialogoGastarMonedas("Skip Ad?", COSTO, tickets, () -> {
                // ACCIÓN: GASTAR
                prefs.edit().putInt("skip_tickets", tickets - COSTO).apply();
                actualizarMonedasUI();
                Toast.makeText(this, "Redeemed! Opening ad-free", Toast.LENGTH_SHORT).show();
                abrirWallpaperDetalles(wall);
            }, () -> {
                // ACCIÓN: VER ANUNCIO
                lanzarLogicaAnuncio(wall);
            });
        }
        // CASO B: NO ALCANZA -> ANUNCIO DIRECTO
        else {
            lanzarLogicaAnuncio(wall);
        }
    }

    // Método auxiliar para no escribir esto dos veces
    private void lanzarLogicaAnuncio(Config.Wallpaper wall) {
        wallpaperClickCount++;

        // Si llegó al contador (3 clics)
        if (wallpaperClickCount >= WALLPAPER_ADS_THRESHOLD) {
            if (mInterstitialAd != null) {
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        wallpaperClickCount = 0; // RESET SOLO SI SE MOSTRÓ
                        cargarAnuncioIntersticial();
                        abrirWallpaperDetalles(wall);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        mInterstitialAd = null;
                        wallpaperClickCount = 0; // RESET TAMBIÉN SI FALLA AL MOSTRAR
                        abrirWallpaperDetalles(wall);
                    }
                });
                mInterstitialAd.show(this);
            } else {
                // Si el anuncio no está listo al 3er clic, abrimos directo pero NO reseteamos
                // el contador, para que el 4to clic intente mostrarlo de nuevo.
                abrirWallpaperDetalles(wall);
                cargarAnuncioIntersticial();
            }
        } else {
            // Clics 1 y 2
            abrirWallpaperDetalles(wall);
            if (mInterstitialAd == null) cargarAnuncioIntersticial();
        }
    }



    public void intentarAbrirPackPremium(StickerPack pack) {
        Config.selectedPack = pack;
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COST = 3;

        if (tickets >= COST) {
            mostrarDialogoGastarMonedas("Unlock Pack?", COST, tickets, () -> {
                prefs.edit().putInt("skip_tickets", tickets - COST).apply();
                actualizarMonedasUI();
                Toast.makeText(this, "Pack Unlocked! 🔓", Toast.LENGTH_SHORT).show();
                abrirPantallaDetalles(pack);
            }, () -> {
                cargarAnuncioYEjecutar(() -> abrirPantallaDetalles(pack));
            });
        } else {
            cargarAnuncioYEjecutar(() -> abrirPantallaDetalles(pack));
        }
    }

    public void intentarAbrirWallpaperPremium(Config.Wallpaper wall) {
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COST = 3;

        if (tickets >= COST) {
            mostrarDialogoGastarMonedas("Unlock Premium?", COST, tickets, () -> {
                prefs.edit().putInt("skip_tickets", tickets - COST).apply();
                actualizarMonedasUI();
                Toast.makeText(this, "Redeemed! Opening... 😎", Toast.LENGTH_SHORT).show();
                abrirWallpaperDetalles(wall);
            }, () -> {
                cargarAnuncioYEjecutar(() -> abrirWallpaperDetalles(wall));
            });
        } else {
            cargarAnuncioYEjecutar(() -> abrirWallpaperDetalles(wall));
        }
    }

    private void abrirPantallaDetalles(StickerPack pack) {
        Config.selectedPack = pack;
        startActivity(new Intent(this, StickerDetailsActivity.class));
    }

    public void abrirWallpaperDetalles(Config.Wallpaper wall) {
        Intent intent = new Intent(this, WallpaperDetailsActivity.class);
        intent.putExtra("wall_name", wall.name);
        intent.putExtra("wall_author", wall.publisher);
        intent.putExtra("wall_image", wall.imageFile);
        intent.putExtra("wall_color", wall.colorBg);
        intent.putExtra("wall_artist_link", wall.artistLink);
        // --- NUEVOS DATOS PARA LA ALERTA ---
        intent.putExtra("is_limited", wall.isLimitedTime);
        intent.putExtra("is_hidden", wall.isHidden);
        // -----------------------------------
        startActivity(intent);
    }

    private void pedirPermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void checkAndShowNews() {
        if (Config.newsData == null || !Config.newsData.enabled) {
            if(fabNews != null) fabNews.setVisibility(View.GONE);
            return;
        }
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lastSeenVersion = prefs.getString("news_last_seen_id", "");
        if (!Config.newsData.versionId.equals(lastSeenVersion)) showNewsIcon();
        else fabNews.setVisibility(View.GONE);
    }

    private void showNewsIcon() {
        // Validación extra de seguridad
        if (fabNews == null || isFinishing() || isDestroyed()) return;

        fabNews.setVisibility(View.VISIBLE);
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);

        // --- PROTECCIÓN CONTRA CRASH ---
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this).load(baseUrl + Config.newsData.iconImage).into(fabNews);
        }

        levitateAnimator = android.animation.ObjectAnimator.ofFloat(fabNews, "translationY", 0f, -30f, 0f);
        levitateAnimator.setDuration(2000);
        levitateAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        levitateAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        levitateAnimator.start();
        fabNews.setOnClickListener(v -> openNewsDialog());
    }

    private void openNewsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_news, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        ImageView imgBg = view.findViewById(R.id.imgNewsBg);
        ImageView imgOverlay = view.findViewById(R.id.imgNewsOverlay);
        TextView txtBody = view.findViewById(R.id.txtNewsBody);
        ImageView btnClose = view.findViewById(R.id.btnCloseNews);

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        String textoFormateado = (Config.newsData != null && Config.newsData.text != null) ? Config.newsData.text.replace("\n", "<br>") : "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            txtBody.setText(android.text.Html.fromHtml(textoFormateado, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            txtBody.setText(android.text.Html.fromHtml(textoFormateado));
        }
        txtBody.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        if (!Config.newsData.bgImage.isEmpty()) Glide.with(this).load(baseUrl + Config.newsData.bgImage).into(imgBg);
        if (!Config.newsData.overlayImage.isEmpty()) Glide.with(this).load(baseUrl + Config.newsData.overlayImage).into(imgOverlay);
        if (!Config.newsData.closeImage.isEmpty()) Glide.with(this).load(baseUrl + Config.newsData.closeImage).into(btnClose);
        dialog.setOnDismissListener(d -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putString("news_last_seen_id", Config.newsData.versionId).apply();
            fabNews.setVisibility(View.GONE);
            if(levitateAnimator != null) levitateAnimator.cancel();
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sincronizarFavoritosDesdeNube();
        if(widgetAdapter != null) widgetAdapter.updateData();
        if (rvWallpapersHorizontal != null && rvWallpapersHorizontal.getAdapter() instanceof WallpaperAdapter) ((WallpaperAdapter) rvWallpapersHorizontal.getAdapter()).updateData();
        if (rvPremiumMixto != null && rvPremiumMixto.getAdapter() instanceof PremiumAdapter) ((PremiumAdapter) rvPremiumMixto.getAdapter()).updateData();
        else if (rvPremiumMixto != null && rvPremiumMixto.getAdapter() != null) rvPremiumMixto.getAdapter().notifyDataSetChanged();
        if (rvBatteryHorizontal != null && rvBatteryHorizontal.getAdapter() instanceof BatteryThemeAdapter) ((BatteryThemeAdapter) rvBatteryHorizontal.getAdapter()).updateData();
        if (rvStickersHorizontal != null && rvStickersHorizontal.getAdapter() != null) rvStickersHorizontal.getAdapter().notifyDataSetChanged();
        actualizarVisibilidadAlerta();
        if (isHome) { resetNavIcons(); if (navHome != null) navHome.setColorFilter(Color.parseColor("#ff4700")); }

        actualizarMonedasUI(); // <--- AGREGAR ESTA LÍNEA

        // --- LÓGICA FINAL DE LA CAMPANA (ONBOARDING + NOTIFICACIÓN) ---
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // 1. ¿Hay una notificación nueva de un amigo?
        boolean hayNotificacion = prefs.getBoolean("has_notification", false);

        // 2. ¿Es la primera vez? (Si NO existe la variable 'wallet_visited', es que nunca ha entrado)
        boolean nuncaHaEntrado = !prefs.getBoolean("wallet_visited", false);

        // MOSTRAR SI: Hay algo nuevo O el usuario nunca ha entrado (para que sepa que existe)
        if (badgeNotification != null) {
            if (hayNotificacion || nuncaHaEntrado) {
                badgeNotification.setVisibility(View.VISIBLE);

                // Opcional: Pequeña animación para llamar la atención si es primera vez
                if (nuncaHaEntrado) {
                    badgeNotification.animate().scaleX(1.2f).scaleY(1.2f).setDuration(500).withEndAction(() ->
                            badgeNotification.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).start()
                    ).start();
                }
            } else {
                badgeNotification.setVisibility(View.GONE);
            }
        }
    }

    private void verificarYActualizarUbicacion() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
        } else {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();
                        WorkManager.getInstance(MainActivity.this).enqueue(updateRequest);
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void actualizarVisibilidadAlerta() {
        if(viewHome == null) return;
        if (cardLocationWarning != null) {
            boolean gpsOk = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            cardLocationWarning.setVisibility(gpsOk ? View.GONE : View.VISIBLE);
        }
        if (cardCalendarWarning != null) {
            boolean calOk = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            cardCalendarWarning.setVisibility(calOk ? View.GONE : View.VISIBLE);
        }
        if (cardBatteryWarning != null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) cardBatteryWarning.setVisibility(View.VISIBLE);
            else cardBatteryWarning.setVisibility(View.GONE);
        }
    }

    private void programarActualizacionAutomatica() {
        try {
            PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(WidgetUpdateWorker.class, 30, TimeUnit.MINUTES).build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("WIDGET_AUTO_UPDATE", ExistingPeriodicWorkPolicy.KEEP, periodicRequest);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void onRequestPermissionsResult(int r, @NonNull String[] p, @NonNull int[] g) { super.onRequestPermissionsResult(r, p, g); if(r==102 && g.length>0 && g[0]==0) { verificarYActualizarUbicacion(); actualizarVisibilidadAlerta(); } if(r==103 && g.length>0 && g[0]==0) { actualizarVisibilidadAlerta(); OneTimeWorkRequest req=new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build(); WorkManager.getInstance(this).enqueue(req); } }

    private void procesarDeepLinkPendiente() {
        if (pendingDeepLinkType == null || pendingDeepLinkId == null) return;
        if (pendingDeepLinkType.equalsIgnoreCase("sticker")) {
            if (Config.packs != null) {
                for (StickerPack pack : Config.packs) {
                    if (pack.identifier.equalsIgnoreCase(pendingDeepLinkId)) { abrirPantallaDetalles(pack); break; }
                }
            }
        }
        else if (pendingDeepLinkType.equalsIgnoreCase("wallpaper")) {
            if (Config.wallpapers != null) {
                for (Config.Wallpaper wall : Config.wallpapers) {
                    if (wall.imageFile.equalsIgnoreCase(pendingDeepLinkId)) { abrirWallpaperDetalles(wall); break; }
                }
            }
        }
        pendingDeepLinkType = null;
        pendingDeepLinkId = null;
    }

    // =========================================================
    // SINCRONIZACIÓN COMPLETA DE PERFIL Y FAVORITOS
    // =========================================================
    private void sincronizarFavoritosDesdeNube() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // --- 1. SINCRONIZAR FAVORITOS (WALLPAPERS, WIDGETS, BATERÍA) ---

                        List<String> cloudWalls = (List<String>) doc.get("fav_wallpapers");
                        if (cloudWalls != null) {
                            Set<String> set = new HashSet<>(cloudWalls);
                            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                    .edit().putStringSet("fav_wallpapers_ids", set).apply();
                        }

                        List<String> cloudWidgets = (List<String>) doc.get("fav_widgets");
                        if (cloudWidgets != null) {
                            Set<String> set = new HashSet<>(cloudWidgets);
                            getSharedPreferences("WidgetPrefs", MODE_PRIVATE)
                                    .edit().putStringSet("fav_wallpapers", set).apply();
                        }

                        List<String> cloudBattery = (List<String>) doc.get("fav_battery");
                        if (cloudBattery != null) {
                            Set<String> set = new HashSet<>(cloudBattery);
                            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                    .edit().putStringSet("fav_battery_ids", set).apply();
                        }

                        // --- 2. SINCRONIZAR DATOS DE ID WALLET (PERFIL Y AMIGOS) ---

                        android.content.SharedPreferences walletPrefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
                        android.content.SharedPreferences.Editor editor = walletPrefs.edit();

                        // Guardar UID esencial
                        editor.putString("user_uid", user.getUid());

                        // Recuperar Datos Básicos
                        String name = doc.getString("user_name");
                        if (name != null && !name.isEmpty()) editor.putString("user_name", name);

                        String friendCode = doc.getString("friend_code");
                        if (friendCode != null) editor.putString("friend_code", friendCode);

                        String userCode = doc.getString("user_code"); // ID de 12 dígitos
                        if (userCode != null) editor.putString("user_code", userCode);

                        String date = doc.getString("created_at");
                        if (date != null) editor.putString("created_at", date);

                        // Recuperar Avatar (URL o ID)
                        String avatarUrl = doc.getString("profile_image_url");
                        if (avatarUrl != null) editor.putString("profile_image_url", avatarUrl);

                        String avatarId = doc.getString("avatar_id"); // Por si usas avatares internos
                        if (avatarId != null) editor.putString("avatar_id", avatarId);

                        // --- AQUÍ ESTABA EL FALTANTE: EL TEMA DEL ID ---
                        String cardTheme = doc.getString("selected_card_id");
                        if (cardTheme != null && !cardTheme.isEmpty()) {
                            editor.putString("selected_card_id", cardTheme);
                        }

                        // Recuperar Lista de Amigos
                        List<String> friends = (List<String>) doc.get("friends");
                        if (friends != null) {
                            Set<String> friendSet = new HashSet<>(friends);
                            editor.putStringSet("saved_friends", friendSet);
                        }

                        // ¡GUARDAR CAMBIOS!
                        editor.apply();

                        // Refrescar vistas
                        if (viewHome != null) {
                            if(widgetAdapter != null) widgetAdapter.updateData();

                            // *** SOLUCIÓN DEL SALTO A FAVORITOS ***
                            // Comentamos la línea que forzaba el cambio de pantalla
                            // if(!isHome) mostrarVistaFavoritos();

                            // En su lugar, si estamos viendo la lista de temas, actualizamos el adapter
                            // para que los corazones se refresquen sin saltar
                            if (fragmentContainer.getChildCount() > 0) {
                                View currentView = fragmentContainer.getChildAt(0);
                                RecyclerView rvWall = currentView.findViewById(R.id.rvThemesWallpapers);
                                if (rvWall != null && rvWall.getAdapter() instanceof WallpaperAdapter) {
                                    ((WallpaperAdapter) rvWall.getAdapter()).updateData();
                                }
                            }
                        }
                    }
                });
    }


    // --- AGREGAR AL FINAL ---
    public void showNotificationBadge() {
        runOnUiThread(() -> {
            if (badgeNotification != null) {
                badgeNotification.setVisibility(View.VISIBLE);
            }
        });
    }



    private void actualizarMonedasUI() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);

        // 1. INICIAR LA ANIMACIÓN SUAVE
        // Esto le dice al layout: "Anima cualquier cambio de tamaño que ocurra a continuación"
        if (cardCoinContainer != null && cardCoinContainer.getParent() instanceof android.view.ViewGroup) {
            android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) cardCoinContainer.getParent());
        }

        // 2. CAMBIAR VISIBILIDAD
        if (tickets > 0) {
            // Al aparecer la moneda, el menú se encogerá suavemente hacia la derecha
            if (cardCoinContainer != null) cardCoinContainer.setVisibility(View.VISIBLE);
            if (txtCoinCount != null) txtCoinCount.setText(String.valueOf(tickets));
        } else {
            // Al desaparecer la moneda, el menú se estirará suavemente hacia la izquierda
            if (cardCoinContainer != null) cardCoinContainer.setVisibility(View.GONE);
        }
    }


    private void mostrarExplicacionMonedas() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_coin_info, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        // ESTO ES LO QUE HACE LA MAGIA DE "FLOTAR"
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnGotIt).setOnClickListener(v -> dialog.dismiss());

        // Animación de entrada (Efecto rebote pequeño)
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0f);
        dialog.show(); // Mostrar primero para que la vista exista

        view.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();
    }



    // =========================================================
    // NUEVO MÉTODO: DIÁLOGO DE GASTO (DISEÑO PERSONALIZADO)
    // =========================================================
    private void mostrarDialogoGastarMonedas(String title, int cost, int balance, Runnable onUseCoins, Runnable onWatchAd) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_spend_coins, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        // FONDO TRANSPARENTE PARA EFECTO FLOTANTE
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            // Evita que se estire
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // VINCULAR VISTAS
        TextView txtTitle = view.findViewById(R.id.txtDialogTitle);
        TextView txtMsg = view.findViewById(R.id.txtDialogMessage);
        TextView txtBal = view.findViewById(R.id.txtCurrentBalance);
        Button btnUse = view.findViewById(R.id.btnUseCoins);
        TextView btnAd = view.findViewById(R.id.btnWatchAd);

        // SETEAR TEXTOS
        txtTitle.setText(title);
        txtMsg.setText("Use " + cost + " coins to unlock this item instantly without ads.");
        txtBal.setText("Balance: " + balance + " coins");
        btnUse.setText("USE " + cost + " COINS");

        // CLICK: USAR MONEDAS
        btnUse.setOnClickListener(v -> {
            dialog.dismiss();
            if (onUseCoins != null) onUseCoins.run();
        });

        // CLICK: VER ANUNCIO
        btnAd.setOnClickListener(v -> {
            dialog.dismiss();
            if (onWatchAd != null) onWatchAd.run();
        });

        dialog.show();
    }



    private void verificarActualizacionForzada() {
        if (Config.newsData == null) return;

        try {
            int currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int minVersion = Config.newsData.minVersionCode;

            // Mantenemos el Log.d solo para que tú lo veas en la consola de Android Studio,
            // pero el usuario ya no verá nada en la pantalla si todo está bien.
            android.util.Log.d("UpdateCheck", "Local: " + currentVersion + " | Servidor: " + minVersion);

            if (currentVersion < minVersion) {
                mostrarDialogoForzado();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarDialogoForzado() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_force_update, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        // ESTO ES LO ÚNICO NECESARIO:
        // Quita el fondo blanco cuadrado del sistema para que se vea tu CardView redondito y flotante
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // Si ves que sigue saliendo muy ancho, agrega esta línea después del show:
        dialog.getWindow().setLayout(convertDpToPx(320), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // Función auxiliar para que el diseño sea exacto en cualquier pantalla
    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }



}