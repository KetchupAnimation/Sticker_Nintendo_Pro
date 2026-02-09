package com.ketchupstudios.Switchstickerapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletHomeActivity extends AppCompatActivity {

    // Vistas
    private CardView cardMyWallet, qrContainerFront;
    private ImageView imgCardBg, imgQrCode, imgQrFront;
    private TextView txtCardName, txtCardCode, txtScanLabel, txtCardArtist;
    private RelativeLayout containerFront, containerBack;
    private ImageView btnEditWallet, btnMenu, btnFlipWallet;
    private Button btnShareHome;

    // --- NUEVO: CAPA DE MIS REACCIONES ---
    private FrameLayout myReactionsLayer;

    // Estado Tarjeta
    private boolean isFrontShowing = true;
    private boolean isFlipping = false;
    private String currentThemeUrl = "";
    private String currentThemeColor = "#ca3537";

    // VARIABLES GUARDADAS
    private String savedName = "";
    private String savedCode = "";
    private String savedThemeId = "";
    private String savedUid = "";

    // --- VARIABLES WITTY STATUS ---
    private androidx.cardview.widget.CardView cardMyStatus;
    private TextView txtMyStatusTitle, txtMyStatusGame;

    // Lista Amigos
    private RecyclerView rvFriends;
    private TextView txtFriendCount;
    private LinearLayout emptyStateView;
    private FriendsAdapter adapter;
    private List<Friend> friendList;

    private Map<String, String> themeColorMap = new HashMap<>();
    private Map<String, String> themeImageMap = new HashMap<>();

    // Anuncios
    private RewardedAd mRewardedAd;
    private static final String AD_UNIT_SHARE = "ca-app-pub-9087203932210009/4573750306";
    private boolean isProcessing = false;

    private static final String JSON_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
    private static final String ID_IMG_BASE = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/ID/";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_home);

        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        MobileAds.initialize(this, initializationStatus -> {});

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Vincular Vistas
        cardMyWallet = findViewById(R.id.cardMyWallet);
        imgCardBg = findViewById(R.id.imgCardBg);
        imgQrCode = findViewById(R.id.imgQrCode);
        txtCardName = findViewById(R.id.txtCardName);
        txtCardCode = findViewById(R.id.txtCardCode);
        txtScanLabel = findViewById(R.id.txtScanLabel);
        txtCardArtist = findViewById(R.id.txtCardArtist);

        containerFront = findViewById(R.id.containerFront);
        containerBack = findViewById(R.id.containerBack);

        // --- NUEVO: VINCULAR LA CAPA DE STICKERS ---
        myReactionsLayer = findViewById(R.id.myReactionsLayer);

        btnEditWallet = findViewById(R.id.btnEditWallet);
        btnShareHome = findViewById(R.id.btnShareHome);
        btnMenu = findViewById(R.id.btnMenu);

        btnFlipWallet = findViewById(R.id.btnFlipWallet);
        qrContainerFront = findViewById(R.id.qrContainerFront);
        imgQrFront = findViewById(R.id.imgQrFront);

        // --- VARIABLES WITTY STATUS ---
        cardMyStatus = findViewById(R.id.cardMyStatus);
        txtMyStatusTitle = findViewById(R.id.txtMyStatusTitle);
        txtMyStatusGame = findViewById(R.id.txtMyStatusGame);

        if (cardMyStatus != null) {
            cardMyStatus.setOnClickListener(v -> mostrarMenuWitty());
        }

        rvFriends = findViewById(R.id.rvFriendsHome);
        txtFriendCount = findViewById(R.id.txtFriendCount);
        emptyStateView = findViewById(R.id.emptyStateView);
        androidx.cardview.widget.CardView btnScanQr = findViewById(R.id.btnScanQr);

        cardMyWallet.setOnClickListener(v -> girarTarjeta());

        btnEditWallet.setOnClickListener(v -> {
            startActivity(new Intent(WalletHomeActivity.this, IdWalletActivity.class));
        });

        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(WalletHomeActivity.this, FriendsActivity.class);
            intent.putExtra("AUTO_SCAN", true);
            startActivity(intent);
        });

        if(btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                startActivity(new Intent(WalletHomeActivity.this, FriendsActivity.class));
                overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
            });
        }

        rvFriends.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        friendList = new ArrayList<>();

        adapter = new FriendsAdapter(
                friendList,
                themeImageMap,
                themeColorMap,
                this,
                new FriendsAdapter.OnFriendActionListener() {
                    @Override
                    public void onDelete(int position) {
                        eliminarAmigo(position);
                    }

                    @Override
                    public void onDataUpdated() {
                        guardarListaAmigos();
                    }

                    @Override
                    public void onAutoScrollRequest(int position) {
                        if (position >= 0 && position < friendList.size()) {
                            rvFriends.post(() -> {
                                rvFriends.smoothScrollToPosition(position);
                            });
                        }
                    }

                    @Override
                    public void onNewNotification() {
                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("has_notification", true)
                                .apply();
                    }
                }
        );
        rvFriends.setAdapter(adapter);

        new Thread(this::descargarMapasTemas).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAuth.getCurrentUser() != null) {
            descargarDatosDeNube(mAuth.getCurrentUser());
        }

        actualizarBotonPrincipal();
        cargarDatosMiTarjeta();
        cargarListaAmigos();
        actualizarUIStatusLocal();

        // --- NUEVO: CARGAR MIS STICKERS Y CHEQUEAR SI HAY NUEVOS ---
        cargarMisReacciones();
    }

    // =================================================================
    // LÓGICA DE STICKERS PROPIA (NUEVO)
    // =================================================================

    private void cargarMisReacciones() {
        String myUid = FirebaseAuth.getInstance().getUid();
        // Si no hay usuario o no existe la capa visual, salimos
        if (myUid == null || myReactionsLayer == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(myUid)
                .collection("received_reactions")
                .limit(30)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Escudo de seguridad
                    if (isFinishing() || querySnapshot.isEmpty()) {
                        if (myReactionsLayer != null) myReactionsLayer.removeAllViews();
                        return;
                    }

                    // 1. DIBUJAR LOS STICKERS EN EL FONDO
                    myReactionsLayer.removeAllViews();
                    long latestTimestamp = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String rId = doc.getString("reactionId");
                        Long ts = doc.getLong("timestamp");

                        // Guardamos la fecha del más reciente
                        if (ts != null && ts > latestTimestamp) latestTimestamp = ts;

                        if (rId != null) {
                            // Buscar imagen en Config
                            for (Reaction r : Config.reactions) {
                                if (r.id.equals(rId)) {
                                    agregarStickerAmiTarjeta(r.image);
                                    break;
                                }
                            }
                        }
                    }

                    // 2. LÓGICA DE AUTO-GIRO (NOTIFICACIÓN)
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    long lastSeenTime = prefs.getLong("last_seen_reaction_time", 0);

                    // Si hay un sticker MÁS NUEVO que la última vez...
                    if (latestTimestamp > lastSeenTime) {

                        // a) Guardamos que ya lo vimos
                        prefs.edit().putLong("last_seen_reaction_time", latestTimestamp).apply();

                        // b) GIRAMOS LA TARJETA AUTOMÁTICAMENTE (Con delay)
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Solo girar si estamos viendo el frente
                            if (isFrontShowing && !isFinishing()) {
                                girarTarjeta();
                            }
                        }, 600);
                    }
                });
    }

    private void agregarStickerAmiTarjeta(String imageFile) {
        if (myReactionsLayer == null) return;

        ImageView sticker = new ImageView(this);

        // Tamaño del sticker
        int size = (int) (90 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

        int w = myReactionsLayer.getWidth();
        int h = myReactionsLayer.getHeight();
        if (w <= 0) w = 900;
        if (h <= 0) h = 600;

        Random random = new Random();

        // Márgenes aleatorios
        params.leftMargin = random.nextInt(Math.max(1, w - size - 20)) + 10;
        params.topMargin = random.nextInt(Math.max(1, h - size - 20)) + 10;
        sticker.setLayoutParams(params);
        sticker.setRotation(random.nextInt(60) - 30);

        // Ruta de la imagen
        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + "Widget/Reactions/";

        Glide.with(this).load(baseUrl + imageFile).into(sticker);

        myReactionsLayer.addView(sticker);

        // Animación de entrada suave
        sticker.setScaleX(0f);
        sticker.setScaleY(0f);
        sticker.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    // =================================================================
    // FIN LÓGICA STICKERS
    // =================================================================

    private void actualizarBotonPrincipal() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            btnShareHome.setText("Login to Sync");
            btnShareHome.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            btnShareHome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4285F4")));
            btnShareHome.setOnClickListener(v -> signIn());
        } else {
            btnShareHome.setText("Share Card");
            btnShareHome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#25D366")));
            btnShareHome.setOnClickListener(v -> verificarYCompartir());
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
                        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
                        prefs.edit().putString("user_uid", user.getUid()).apply();

                        descargarDatosDeNube(user);
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void descargarDatosDeNube(FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        String cloudName = documentSnapshot.getString("user_name");
                        if (cloudName != null) editor.putString("user_name", cloudName);

                        String cloudCode = documentSnapshot.getString("user_code"); // ID SWITCH
                        if (cloudCode != null) editor.putString("user_code", cloudCode);

                        String cloudThemeId = documentSnapshot.getString("selected_card_id");
                        if (cloudThemeId != null) {
                            editor.putString("saved_theme_id", cloudThemeId);
                            if(themeImageMap.containsKey(cloudThemeId)){
                                String url = ID_IMG_BASE + themeImageMap.get(cloudThemeId);
                                editor.putString("saved_image_url", url);
                            }
                        }

                        String cloudThemeColor = documentSnapshot.getString("saved_theme_color");
                        if(cloudThemeColor != null) editor.putString("saved_theme_color", cloudThemeColor);

                        String cloudStatus = documentSnapshot.getString("witty_status");
                        String cloudGame = documentSnapshot.getString("witty_game");
                        if (cloudStatus != null) editor.putString("witty_title", cloudStatus);
                        if (cloudGame != null) editor.putString("witty_game", cloudGame);

                        try {
                            Object friendsObj = documentSnapshot.get("friends_list");
                            if (friendsObj != null) {
                                Gson gson = new Gson();
                                String jsonFriends = gson.toJson(friendsObj); // Convertir a JSON String
                                SharedPreferences prefsFriends = getSharedPreferences("FriendsData", MODE_PRIVATE);
                                prefsFriends.edit().putString("friends_list", jsonFriends).apply();
                            }
                        } catch (Exception e) { e.printStackTrace(); }

                        editor.apply();

                        cargarDatosMiTarjeta();
                        cargarListaAmigos();
                        actualizarUIStatusLocal();
                        actualizarBotonPrincipal();
                    }
                });
    }

    private void iniciarCompartirConAnuncio() {
        if (isProcessing) return;
        iniciarCuentaRegresiva(btnShareHome, "Share Card", () -> {
            cargarAnuncioYEjecutarShare();
        });
    }

    private void iniciarCuentaRegresiva(TextView btn, String textoOriginal, Runnable alFinalizar) {
        isProcessing = true;
        new CountDownTimer(3100, 1000) {
            @Override public void onTick(long millis) {
                long seg = millis / 1000;
                if(seg > 0) btn.setText(String.valueOf(seg));
            }
            @Override public void onFinish() {
                btn.setText(textoOriginal);
                alFinalizar.run();
            }
        }.start();
    }

    private void cargarAnuncioYEjecutarShare() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Preparing Share...");
        pd.setCancelable(false);
        pd.show();

        RewardedAd.load(this, AD_UNIT_SHARE, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull RewardedAd ad) {
                mRewardedAd = ad;
                pd.dismiss();
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        mRewardedAd = null; isProcessing = false; ejecutarAccionShare();
                    }
                    @Override public void onAdFailedToShowFullScreenContent(AdError e) {
                        mRewardedAd = null; isProcessing = false; ejecutarAccionShare();
                    }
                });
                mRewardedAd.show(WalletHomeActivity.this, r -> {});
            }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError e) {
                pd.dismiss(); mRewardedAd = null; isProcessing = false; ejecutarAccionShare();
            }
        });
    }

    private void ejecutarAccionShare() {
        if (!isFrontShowing) {
            girarTarjeta();
            new android.os.Handler().postDelayed(this::prepararYCompartir, 700);
        } else {
            prepararYCompartir();
        }
    }

    private void prepararYCompartir() {
        if (isFinishing() || isDestroyed()) return;
        btnEditWallet.setVisibility(View.GONE);
        btnFlipWallet.setVisibility(View.GONE);
        qrContainerFront.setVisibility(View.VISIBLE);

        String qrData = "SWITCH:" + savedCode + ":" + savedName + ":" + savedThemeId + ":" + currentThemeColor + ":" + savedUid;
        String qrUrl = "https://quickchart.io/qr?text=" + Uri.encode(qrData) + "&size=400&dark=000000&light=ffffff&margin=2";

        Glide.with(this).load(qrUrl).into(new CustomTarget<Drawable>() {
            @Override public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                imgQrFront.setImageDrawable(resource);
                capturarYCompartir();
            }
            @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
        });
    }

    private void capturarYCompartir() {
        View holoEffectView = null;
        if (cardMyWallet.getChildCount() > 0) {
            View childZero = cardMyWallet.getChildAt(0);
            if (childZero instanceof ViewGroup) {
                ViewGroup frame = (ViewGroup) childZero;
                for (int i = 0; i < frame.getChildCount(); i++) {
                    View v = frame.getChildAt(i);
                    if (v instanceof HoloCardView) { holoEffectView = v; break; }
                }
            }
        }
        if (holoEffectView != null) holoEffectView.setVisibility(View.INVISIBLE);

        Bitmap bitmap = Bitmap.createBitmap(cardMyWallet.getWidth(), cardMyWallet.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cardMyWallet.draw(canvas);

        if (holoEffectView != null) holoEffectView.setVisibility(View.VISIBLE);
        btnEditWallet.setVisibility(View.VISIBLE);
        if (btnMenu != null) btnMenu.setVisibility(View.VISIBLE);
        btnFlipWallet.setVisibility(View.VISIBLE);
        qrContainerFront.setVisibility(View.GONE);

        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(cachePath + "/my_card.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            File imagePath = new File(getCacheDir(), "images");
            File newFile = new File(imagePath, "my_card.png");
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", newFile);
            if (contentUri != null) {
                String appLink = "https://play.google.com/store/apps/details?id=" + getPackageName();
                String shareText = "Add me on Switch! My Friend Code: " + savedCode + "\n\nCreate yours here: " + appLink;
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Share Card"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing", Toast.LENGTH_SHORT).show();
        }
    }

    private void descargarMapasTemas() {
        try {
            URL url = new URL(JSON_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);

            JSONObject json = new JSONObject(result.toString());
            JSONArray array = json.getJSONArray("id_themes");

            themeColorMap.clear();
            themeImageMap.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String id = o.getString("id");
                String colorBg = o.optString("color_bg", "#CCCCCC");
                String imageFile = o.getString("image");

                themeColorMap.put(id, colorBg);
                themeImageMap.put(id, imageFile);
            }

            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.updateThemeMaps(themeImageMap, themeColorMap);
                }
                cargarDatosMiTarjeta();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarListaAmigos() {
        SharedPreferences prefs = getSharedPreferences("FriendsData", MODE_PRIVATE);
        String json = prefs.getString("friends_list", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Friend>>(){}.getType();
        List<Friend> loadedList = gson.fromJson(json, type);

        friendList.clear();
        if (loadedList != null) {
            friendList.addAll(loadedList);
        }

        txtFriendCount.setText("Friend List " + friendList.size() + "/30");

        if (friendList.isEmpty()) {
            rvFriends.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            rvFriends.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void guardarListaAmigos() {
        SharedPreferences prefs = getSharedPreferences("FriendsData", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(friendList);
        prefs.edit().putString("friends_list", json).apply();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("friends_list", friendList);

            db.collection("users").document(user.getUid())
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error syncing friends", Toast.LENGTH_SHORT).show());
        }
    }

    private void eliminarAmigo(int position) {
        if (position >= 0 && position < friendList.size()) {
            friendList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, friendList.size());
            guardarListaAmigos();
            txtFriendCount.setText("Friend List " + friendList.size() + "/30");
            if (friendList.isEmpty()) {
                rvFriends.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void cargarDatosMiTarjeta() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);

        savedName = prefs.getString("user_name", "TU NOMBRE");
        savedCode = prefs.getString("user_code", "");
        savedThemeId = prefs.getString("saved_theme_id", "1");
        currentThemeColor = prefs.getString("saved_theme_color", "#ca3537");
        String artistName = prefs.getString("saved_artist_name", "Nintendo");

        savedUid = prefs.getString("user_uid", "");
        if (savedUid.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            savedUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        txtCardName.setText(savedName.isEmpty() ? "TU NOMBRE" : savedName);
        txtCardCode.setText(savedCode.isEmpty() ? "SW-XXXX-XXXX-XXXX" : savedCode);
        if(txtCardArtist != null) txtCardArtist.setText("Art by: " + artistName);

        boolean imagenCargada = false;

        if (themeImageMap.containsKey(savedThemeId)) {
            String imageFile = themeImageMap.get(savedThemeId);
            String fullUrl = ID_IMG_BASE + imageFile;
            Glide.with(this).load(fullUrl).into(imgCardBg);
            imagenCargada = true;

            if (themeColorMap.containsKey(savedThemeId)) {
                currentThemeColor = themeColorMap.get(savedThemeId);
            }
        }

        if (!imagenCargada) {
            String savedImageUrl = prefs.getString("saved_image_url", "");
            if (!savedImageUrl.isEmpty()) {
                Glide.with(this).load(savedImageUrl).into(imgCardBg);
            }
        }

        try {
            imgCardBg.setBackgroundColor(Color.parseColor(currentThemeColor));
        } catch (Exception e) {
            imgCardBg.setBackgroundColor(Color.parseColor("#ca3537"));
        }

        cargarQR();
    }

    private void cargarQR() {
        if (isFinishing() || isDestroyed()) return;
        if (savedCode.isEmpty()) {
            imgQrCode.setImageDrawable(null);
            txtScanLabel.setText("Save your data first!");
            return;
        }
        txtScanLabel.setText("SCAN ME");

        String qrData = "SWITCH:" + savedCode + ":" + savedName + ":" + savedThemeId + ":" + currentThemeColor + ":" + savedUid;
        String qrUrl = "https://quickchart.io/qr?text=" + Uri.encode(qrData) + "&size=400&dark=000000&light=ffffff&margin=2";

        Glide.with(this)
                .load(qrUrl)
                .apply(new RequestOptions().transform(new RoundedCorners(30)).fitCenter())
                .into(imgQrCode);
    }

    private void girarTarjeta() {
        if (isFlipping) return;
        isFlipping = true;

        float distance = 8000 * getResources().getDisplayMetrics().density;
        cardMyWallet.setCameraDistance(distance);

        final float startScale = 1.0f;
        final float endScale = 0.9f;

        ObjectAnimator flip1 = ObjectAnimator.ofFloat(cardMyWallet, "rotationY", 0f, 90f);
        flip1.setDuration(250);
        flip1.setInterpolator(new AccelerateDecelerateInterpolator());

        cardMyWallet.animate().scaleX(endScale).scaleY(endScale).setDuration(250).start();

        flip1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFrontShowing) {
                    containerFront.setVisibility(View.GONE);
                    containerBack.setVisibility(View.VISIBLE);
                    imgCardBg.setImageAlpha(0);
                    try {
                        imgCardBg.setBackgroundColor(Color.parseColor(currentThemeColor));
                    } catch (Exception e) {
                        imgCardBg.setBackgroundColor(Color.parseColor("#ca3537"));
                    }
                } else {
                    containerBack.setVisibility(View.GONE);
                    containerFront.setVisibility(View.VISIBLE);
                    imgCardBg.setImageAlpha(255);
                }

                cardMyWallet.setRotationY(-90f);
                ObjectAnimator flip2 = ObjectAnimator.ofFloat(cardMyWallet, "rotationY", -90f, 0f);
                flip2.setDuration(250);
                flip2.setInterpolator(new AccelerateDecelerateInterpolator());

                cardMyWallet.animate().scaleX(startScale).scaleY(startScale).setDuration(250).start();

                flip2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isFrontShowing = !isFrontShowing;
                        isFlipping = false;
                    }
                });
                flip2.start();
            }
        });
        flip1.start();
    }

    private void actualizarUIStatusLocal() {
        if (txtMyStatusTitle == null || txtMyStatusGame == null) return;
        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);

        String title = prefs.getString("witty_title", "");
        String game = prefs.getString("witty_game", "");

        if (title.isEmpty()) {
            txtMyStatusTitle.setText(getString(R.string.status_ready));
            txtMyStatusGame.setVisibility(View.GONE);
        } else {
            txtMyStatusTitle.setText(title);
            if (game.isEmpty()) {
                txtMyStatusGame.setVisibility(View.GONE);
            } else {
                txtMyStatusGame.setVisibility(View.VISIBLE);
                txtMyStatusGame.setText(game);
            }
        }
    }

    private void mostrarMenuWitty() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_witty_sheet, null);
        dialog.setContentView(sheetView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        String[] titles = getResources().getStringArray(R.array.witty_status_titles);
        String[] games = getResources().getStringArray(R.array.witty_status_games);

        List<WittyStatusAdapter.StatusItem> listaItems = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            listaItems.add(new WittyStatusAdapter.StatusItem(titles[i], games[i]));
        }

        RecyclerView rv = sheetView.findViewById(R.id.rvWittyStatus);
        rv.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
        String currentTitle = prefs.getString("witty_title", "");

        WittyStatusAdapter adapter = new WittyStatusAdapter(listaItems, currentTitle);
        rv.setAdapter(adapter);

        Button btnSave = sheetView.findViewById(R.id.btnSaveStatus);
        btnSave.setOnClickListener(v -> {
            WittyStatusAdapter.StatusItem selected = adapter.getSelectedItem();
            if (selected != null) {
                guardarStatus(selected.title, selected.game);
            } else {
                guardarStatus("", "");
            }
            dialog.dismiss();
        });

        TextView btnCancel = sheetView.findViewById(R.id.btnCancelStatus);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void guardarStatus(String title, String game) {
        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("witty_title", title)
                .putString("witty_game", game)
                .apply();

        actualizarUIStatusLocal();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("witty_status", title);
            update.put("witty_game", game);
            update.put("witty_timestamp", System.currentTimeMillis());

            db.collection("users").document(user.getUid())
                    .set(update, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.status_updated_msg), Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error syncing status", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("has_notification", false)
                .commit();
    }



    // ================================================================
    // SISTEMA DE MONEDAS: COMPARTIR TARJETA
    // ================================================================
    private void verificarYCompartir() {
        if (isProcessing) return;

        SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COSTO = 3;

        if (tickets >= COSTO) {
            mostrarDialogoMonedas("Share Card?", COSTO, tickets, () -> {
                // A) PAGÓ: Descontar y Compartir directo (sin espera)
                int nuevoSaldo = tickets - COSTO;
                prefs.edit().putInt("skip_tickets", nuevoSaldo).apply();
                actualizarMonedasNube(nuevoSaldo);
                ejecutarAccionShare(); // <--- COMPARTE DIRECTO
            }, () -> {
                // B) PREFIRIÓ ANUNCIO
                iniciarCompartirConAnuncio(); // <--- TU LÓGICA DE REWARDED AD
            });
        } else {
            iniciarCompartirConAnuncio();
        }
    }

    private void mostrarDialogoMonedas(String title, int cost, int balance, Runnable onPay, Runnable onAd) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_spend_coins, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));

        TextView tTitle = view.findViewById(R.id.txtDialogTitle);
        TextView tBal = view.findViewById(R.id.txtCurrentBalance);
        Button btnUse = view.findViewById(R.id.btnUseCoins);
        TextView btnWatch = view.findViewById(R.id.btnWatchAd);

        tTitle.setText(title);
        tBal.setText("Balance: " + balance);
        btnUse.setText("USE " + cost + " COINS");
        ((TextView)view.findViewById(R.id.txtDialogMessage)).setText("Share without waiting?");

        btnUse.setOnClickListener(v -> { dialog.dismiss(); onPay.run(); });
        btnWatch.setOnClickListener(v -> { dialog.dismiss(); onAd.run(); });
        dialog.show();
    }

    private void actualizarMonedasNube(int saldo) {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).update("coins", saldo);
        }
    }

}