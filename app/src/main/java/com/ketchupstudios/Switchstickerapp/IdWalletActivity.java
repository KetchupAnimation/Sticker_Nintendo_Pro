package com.ketchupstudios.Switchstickerapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

public class IdWalletActivity extends AppCompatActivity {

    private CardView cardPreview;
    private ImageView imgCardBg, btnFlip;
    private TextView txtCardName, txtCardCode, txtCardArtist;
    private RelativeLayout containerFront, containerBack;
    private TextInputEditText inputName, inputCode;
    private RecyclerView rvThemes;
    private Button btnSave;
    private ImageView btnFriendsList;
    private ImageView imgFavGameFront;

    private int slotSeleccionado = 0;
    private String[] myGameIds = {"", "", ""};
    private static final int RC_PICK_GAME = 9002;
    private String[] myGameTitles = {"", "", ""};
    private String[] myGameImages = {"", "", ""};

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private ImageView gameSlot1, gameSlot2, gameSlot3;
    private Button btnLogin;

    private static List<IdTheme> themeList = new ArrayList<>();

    // Variables Preview
    private String currentThemeUrl = "";
    private String currentTextColor = "#FFFFFF";
    private String currentThemeId = "";
    private String currentThemeColor = "#ca3537";
    private String currentArtistName = "Nintendo";

    // Bandera para saber si hay cambios sin guardar
    private boolean hasUnsavedChanges = false;

    private boolean isFrontShowing = true;
    private boolean isFlipping = false;

    private InterstitialAd mInterstitialAd;
    private static final String AD_UNIT_SAVE = "ca-app-pub-9087203932210009/2214350595";

    private TextView lblTop3;

    private static final String JSON_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
    private static final String ID_IMG_BASE = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/ID/";

    public static class IdTheme {
        String id, imageFile, textColor, colorBg, artistCredits;
        boolean isNew;

        public IdTheme(String i, String im, String t, String c, boolean n, String ac) {
            id = i; imageFile = im; textColor = t; colorBg = c; isNew = n; artistCredits = ac;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_wallet);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        lblTop3 = findViewById(R.id.lblTop3);
        imgFavGameFront = findViewById(R.id.imgFavGameFront);
        cardPreview = findViewById(R.id.cardPreview);
        imgCardBg = findViewById(R.id.imgCardBg);

        containerFront = findViewById(R.id.containerFront);
        containerBack = findViewById(R.id.containerBack);

        txtCardName = findViewById(R.id.txtCardName);
        txtCardCode = findViewById(R.id.txtCardCode);
        txtCardArtist = findViewById(R.id.txtCardArtist);

        inputName = findViewById(R.id.inputName);
        inputCode = findViewById(R.id.inputCode);
        rvThemes = findViewById(R.id.rvIdThemes);
        btnSave = findViewById(R.id.btnSave);

        btnFriendsList = findViewById(R.id.btnFriendsList);
        btnFlip = findViewById(R.id.btnFlip);

        gameSlot1 = findViewById(R.id.gameSlot1);
        gameSlot2 = findViewById(R.id.gameSlot2);
        gameSlot3 = findViewById(R.id.gameSlot3);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> signIn());

        View.OnClickListener slotListener = v -> {
            if (v.getId() == R.id.gameSlot1) abrirBuscador(1);
            else if (v.getId() == R.id.gameSlot2) abrirBuscador(2);
            else if (v.getId() == R.id.gameSlot3) abrirBuscador(3);
        };
        gameSlot1.setOnClickListener(slotListener);
        gameSlot2.setOnClickListener(slotListener);
        gameSlot3.setOnClickListener(slotListener);

        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        MobileAds.initialize(this, initializationStatus -> {});

        // 1. CARGAR DATOS
        recargarDatosLocales();

        // 2. TEXTOS
        inputName.setText(getSharedPreferences("IdWalletPrefs", MODE_PRIVATE).getString("user_name", ""));
        inputCode.setText(getSharedPreferences("IdWalletPrefs", MODE_PRIVATE).getString("user_code", ""));

        actualizarVistaPrevia();
        checkUserStatus();
        actualizarMiniJuegoFrontal();

        findViewById(R.id.cardContainer).setOnClickListener(v -> girarTarjeta());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { actualizarVistaPrevia(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        inputName.addTextChangedListener(watcher);
        inputCode.addTextChangedListener(watcher);

        btnSave.setOnClickListener(v -> verificarYGuardar());

        rvThemes.setLayoutManager(new GridLayoutManager(this, 3));
        new Thread(this::descargarTemas).start();

        btnFriendsList.setOnClickListener(v -> {
            startActivity(new Intent(IdWalletActivity.this, FriendsActivity.class));
            overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Solo recargamos de disco si NO tenemos cambios sin guardar (para no borrar el preview de juegos)
        if (!hasUnsavedChanges) {
            recargarDatosLocales();
        }
        actualizarVistaPrevia();

        if(mAuth.getCurrentUser() != null && currentThemeUrl.isEmpty() && !hasUnsavedChanges){
            descargarDatosDeNube(mAuth.getCurrentUser());
        }
    }

    private void recargarDatosLocales() {
        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);

        currentThemeId = prefs.getString("selected_card_id", "1");
        if(currentThemeId == null || currentThemeId.isEmpty()) {
            currentThemeId = prefs.getString("saved_theme_id", "1");
        }

        currentThemeColor = prefs.getString("saved_theme_color", "#ca3537");
        currentArtistName = prefs.getString("saved_artist_name", "Nintendo");
        currentThemeUrl = prefs.getString("saved_image_url", "");

        // --- REEMPLAZA EL BLOQUE DE CARGA DE JUEGOS POR ESTO ---
        myGameIds[0] = prefs.getString("game_0_id", "");
        myGameTitles[0] = prefs.getString("game_0_title", "");
        myGameImages[0] = prefs.getString("game_0_image", "");

        myGameIds[1] = prefs.getString("game_1_id", "");
        myGameTitles[1] = prefs.getString("game_1_title", "");
        myGameImages[1] = prefs.getString("game_1_image", "");

        myGameIds[2] = prefs.getString("game_2_id", "");
        myGameTitles[2] = prefs.getString("game_2_title", "");
        myGameImages[2] = prefs.getString("game_2_image", "");
        // -------------------------------------------------------

        aplicarCambiosVisuales();

        if (!myGameIds[0].isEmpty()) actualizarSlot(gameSlot1, myGameIds[0]);
        else gameSlot1.setImageDrawable(null);

        if (!myGameIds[1].isEmpty()) actualizarSlot(gameSlot2, myGameIds[1]);
        else gameSlot2.setImageDrawable(null);

        if (!myGameIds[2].isEmpty()) actualizarSlot(gameSlot3, myGameIds[2]);
        else gameSlot3.setImageDrawable(null);
    }

    private void aplicarCambiosVisuales() {
        try {
            int color = Color.parseColor(currentThemeColor);
            imgCardBg.setBackgroundColor(color);
            containerBack.setBackgroundColor(color);
        } catch (Exception e) {
            imgCardBg.setBackgroundColor(Color.parseColor("#ca3537"));
        }

        if (!currentThemeUrl.isEmpty() && isFrontShowing) {
            Glide.with(this).load(currentThemeUrl).into(imgCardBg);
        } else if (!isFrontShowing) {
            imgCardBg.setImageDrawable(null);
        }
    }

    private void girarTarjeta() {
        if (isFlipping) return;
        isFlipping = true;

        float distance = 8000 * getResources().getDisplayMetrics().density;
        cardPreview.setCameraDistance(distance);

        final float startScale = 1.0f;
        final float endScale = 0.9f;

        ObjectAnimator flip1 = ObjectAnimator.ofFloat(cardPreview, "rotationY", 0f, 90f);
        flip1.setDuration(250);
        flip1.setInterpolator(new AccelerateDecelerateInterpolator());

        cardPreview.animate().scaleX(endScale).scaleY(endScale).setDuration(250).start();

        flip1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFrontShowing) {
                    containerFront.setVisibility(View.GONE);
                    containerBack.setVisibility(View.VISIBLE);
                    imgCardBg.setImageAlpha(0);
                    try {
                        imgCardBg.setBackgroundColor(Color.parseColor(currentThemeColor));
                    } catch (Exception e) {}
                    actualizarMiniJuegoFrontal();
                } else {
                    containerBack.setVisibility(View.GONE);
                    containerFront.setVisibility(View.VISIBLE);
                    imgCardBg.setImageAlpha(255);
                    if (!currentThemeUrl.isEmpty()) {
                        Glide.with(IdWalletActivity.this).load(currentThemeUrl).into(imgCardBg);
                    }
                }

                cardPreview.setRotationY(-90f);
                ObjectAnimator flip2 = ObjectAnimator.ofFloat(cardPreview, "rotationY", -90f, 0f);
                flip2.setDuration(250);
                flip2.setInterpolator(new AccelerateDecelerateInterpolator());

                cardPreview.animate().scaleX(startScale).scaleY(startScale).setDuration(250).start();

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

    private void descargarTemas() {
        try {
            URL url = new URL(JSON_URL + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            JSONObject json = new JSONObject(result.toString());
            JSONArray array = json.getJSONArray("id_themes");
            List<IdTheme> priorityList = new ArrayList<>();
            List<IdTheme> normalList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                IdTheme theme = new IdTheme(
                        o.getString("id"),
                        o.getString("image"),
                        o.getString("text_color"),
                        o.optString("color_bg", "#CCCCCC"),
                        o.optBoolean("is_new", false),
                        o.optString("artist", "")
                );
                if (theme.isNew) priorityList.add(theme); else normalList.add(theme);
            }

            synchronized(themeList) {
                themeList.clear();
                themeList.addAll(priorityList);
                themeList.addAll(normalList);
            }

            runOnUiThread(() -> {
                rvThemes.setAdapter(new IdThemeAdapter(themeList, theme -> aplicarTema(theme)));

                SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
                String prefsId = prefs.getString("selected_card_id", "");
                if(prefsId.isEmpty()) prefsId = prefs.getString("saved_theme_id", "");

                if (prefsId != null && !prefsId.isEmpty()) {
                    for (IdTheme t : themeList) {
                        if (t.id.equals(prefsId)) {
                            aplicarTema(t);
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void aplicarTema(IdTheme theme) {
        if (isFlipping) return;

        // SOLO ACTUALIZAR VARIABLES EN MEMORIA (PREVIEW)
        currentThemeId = theme.id;
        currentThemeUrl = ID_IMG_BASE + theme.imageFile;
        currentTextColor = theme.textColor;
        currentThemeColor = theme.colorBg;
        currentArtistName = (theme.artistCredits != null && !theme.artistCredits.isEmpty()) ? theme.artistCredits : "";

        hasUnsavedChanges = true; // Marcar que hay cambios

        cardPreview.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> cardPreview.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();

        aplicarCambiosVisuales();

        try {
            int textColor = Color.parseColor(currentTextColor);
            txtCardName.setTextColor(textColor);
            txtCardCode.setTextColor(textColor);
            txtCardArtist.setTextColor(textColor);
        } catch (Exception e) {}

        if (!currentArtistName.isEmpty()) {
            txtCardArtist.setVisibility(View.VISIBLE);
            txtCardArtist.setText("Art by: " + currentArtistName);
        } else {
            txtCardArtist.setVisibility(View.GONE);
        }
    }

    private void cargarAnuncioYGuardar() {
        btnSave.setEnabled(false);
        btnSave.setText("Loading Ad...");

        InterstitialAd.load(this, AD_UNIT_SAVE, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitialAd = ad;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        ejecutarAccionSave();
                    }
                    @Override public void onAdFailedToShowFullScreenContent(AdError e) {
                        mInterstitialAd = null;
                        ejecutarAccionSave();
                    }
                });
                mInterstitialAd.show(IdWalletActivity.this);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError e) {
                mInterstitialAd = null;
                ejecutarAccionSave();
            }
        });
    }

    private void ejecutarAccionSave() {
        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String finalName = inputName.getText().toString();
        String finalCode = inputCode.getText().toString();

        editor.putString("user_name", finalName);
        editor.putString("user_code", finalCode);

        editor.putString("selected_card_id", currentThemeId);
        editor.putString("saved_theme_id", currentThemeId);
        editor.putString("saved_theme_color", currentThemeColor);
        editor.putString("saved_image_url", currentThemeUrl);
        editor.putString("saved_artist_name", currentArtistName);

        // --- GUARDADO LOCAL DETALLADO ---
        for (int i = 0; i < 3; i++) {
            editor.putString("game_" + i + "_id", myGameIds[i]);
            editor.putString("game_" + i + "_title", myGameTitles[i]);
            editor.putString("game_" + i + "_image", myGameImages[i]);
        }
        // --------------------------------

        if (mAuth.getCurrentUser() != null) {
            editor.putString("user_uid", mAuth.getCurrentUser().getUid());
        }
        editor.apply();

        // SUBIR A NUBE
        if (mAuth.getCurrentUser() != null) {
            Map<String, Object> cloudData = new HashMap<>();
            cloudData.put("user_name", finalName);
            cloudData.put("user_code", finalCode);
            cloudData.put("selected_card_id", currentThemeId);
            cloudData.put("saved_theme_id", currentThemeId);
            cloudData.put("saved_theme_color", currentThemeColor);
            // --- CONSTRUIR LISTA DE MAPAS PARA LA NUBE ---
            List<Map<String, String>> gamesList = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                if (myGameIds[i] != null && !myGameIds[i].isEmpty()) {
                    Map<String, String> g = new HashMap<>();
                    g.put("id", myGameIds[i]);
                    g.put("title", myGameTitles[i] != null ? myGameTitles[i] : "");
                    g.put("image", myGameImages[i] != null ? myGameImages[i] : "");
                    gamesList.add(g);
                }
            }
            cloudData.put("favorite_games", gamesList);
            // ---------------------------------------------

            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .set(cloudData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Perfil actualizado"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Error subiendo", e));
        }

        hasUnsavedChanges = false; // Resetear bandera al guardar

        ocultarTeclado();
        btnSave.setText("Save Data");
        btnSave.setEnabled(true);
        Toast.makeText(this, "Data Saved & Synced!", Toast.LENGTH_SHORT).show();
    }

    private void actualizarVistaPrevia() {
        String name = inputName.getText().toString();
        String code = inputCode.getText().toString();
        txtCardName.setText(name.isEmpty() ? "TU NOMBRE" : name);
        txtCardCode.setText(code.isEmpty() ? "SW-0000-0000-0000" : code);
        if (!currentArtistName.isEmpty()) txtCardArtist.setText("Art by: " + currentArtistName);
    }

    private void ocultarTeclado() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            btnLogin.setVisibility(View.GONE);
            if (lblTop3 != null) lblTop3.setVisibility(View.VISIBLE);
            findViewById(R.id.gamesContainer).setVisibility(View.VISIBLE);
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            if (lblTop3 != null) lblTop3.setVisibility(View.GONE);
            findViewById(R.id.gamesContainer).setVisibility(View.GONE);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void abrirBuscador(int slot) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Login first to edit favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        slotSeleccionado = slot;
        Intent intent = new Intent(this, GameSearchActivity.class);
        startActivityForResult(intent, RC_PICK_GAME);
        overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Login Error", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == RC_PICK_GAME && resultCode == RESULT_OK && data != null) {
            String gameId = data.getStringExtra("GAME_ID");
            // --- CAPTURAR DATOS EXTRA ---
            String gameTitle = data.getStringExtra("GAME_TITLE");
            String gameImage = data.getStringExtra("GAME_IMAGE");

            // ACTUALIZAR SOLO VISUALMENTE (PREVIEW)
            int index = slotSeleccionado - 1;
            myGameIds[index] = gameId;
            myGameTitles[index] = gameTitle;
            myGameImages[index] = gameImage;
            // ----------------------------
            hasUnsavedChanges = true;

            ImageView imgDestino = null;
            if (slotSeleccionado == 1) imgDestino = gameSlot1;
            else if (slotSeleccionado == 2) imgDestino = gameSlot2;
            else if (slotSeleccionado == 3) imgDestino = gameSlot3;

            if (imgDestino != null) actualizarSlot(imgDestino, gameId);
            if (slotSeleccionado == 1) actualizarMiniJuegoFrontal();

            // ELIMINAMOS EL GUARDADO AUTOMÁTICO AQUÍ
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        descargarDatosDeNube(user);
                        checkUserStatus();
                    }
                });
    }

    private void descargarDatosDeNube(FirebaseUser user) {
        if (user == null) return;
        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cloudName = documentSnapshot.getString("user_name");
                        String cloudCode = documentSnapshot.getString("user_code");
                        String cloudThemeId = documentSnapshot.getString("selected_card_id");
                        String cloudThemeColor = documentSnapshot.getString("saved_theme_color");
                        Object rawGames = documentSnapshot.get("favorite_games");


                        SharedPreferences prefs = getSharedPreferences("IdWalletPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        if (cloudName != null) {
                            editor.putString("user_name", cloudName);
                            inputName.setText(cloudName);
                        }
                        if (cloudCode != null) {
                            editor.putString("user_code", cloudCode);
                            inputCode.setText(cloudCode);
                        }

                        if (cloudThemeId != null) {
                            editor.putString("selected_card_id", cloudThemeId);
                            editor.putString("saved_theme_id", cloudThemeId);
                        }

                        if (cloudThemeColor != null) editor.putString("saved_theme_color", cloudThemeColor);

                        // --- GUARDAR JUEGOS (ANTI-CRASH) ---
                        if (rawGames instanceof List) {
                            List<?> list = (List<?>) rawGames;
                            for (int i = 0; i < 3; i++) {
                                if (i < list.size()) {
                                    Object item = list.get(i);

                                    if (item instanceof Map) {
                                        // NUEVO
                                        Map<String, String> g = (Map<String, String>) item;
                                        editor.putString("game_" + i + "_id", g.get("id"));
                                        editor.putString("game_" + i + "_title", g.get("title"));
                                        editor.putString("game_" + i + "_image", g.get("image"));
                                    } else if (item instanceof String) {
                                        // VIEJO
                                        editor.putString("game_" + i + "_id", (String) item);
                                        // No guardamos título para obligar a actualizar después
                                    }
                                } else {
                                    // Limpiar vacíos
                                    editor.remove("game_" + i + "_id");
                                    editor.remove("game_" + i + "_title");
                                    editor.remove("game_" + i + "_image");
                                }
                            }
                        }
                        // -----------------------------------
                        editor.apply();

                        try {
                            Object friendsObj = documentSnapshot.get("friends_list");
                            if (friendsObj != null) {
                                Gson gson = new Gson();
                                String jsonFriends = gson.toJson(friendsObj);
                                SharedPreferences prefsFriends = getSharedPreferences("FriendsData", MODE_PRIVATE);
                                prefsFriends.edit().putString("friends_list", jsonFriends).apply();
                            }
                        } catch (Exception e) { e.printStackTrace(); }

                        recargarDatosLocales();
                        actualizarVistaPrevia();

                        if (cloudThemeId != null && !themeList.isEmpty()) {
                            for (IdTheme t : themeList) {
                                if (t.id.equals(cloudThemeId)) {
                                    aplicarTema(t);
                                    hasUnsavedChanges = false; // Como vino de nube, no cuenta como cambio manual
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private void actualizarSlot(ImageView img, String gameId) {
        if (gameId.isEmpty()) return;
        String url = "https://api.nlib.cc/nx/" + gameId + "/icon";
        Glide.with(this).load(url).placeholder(android.R.drawable.ic_menu_gallery).into(img);
    }

    // No lo usamos directamente en onActivityResult, solo en guardar
    private void guardarJuegosEnNube() { }

    private void actualizarMiniJuegoFrontal() {
        if (imgFavGameFront == null) return;
        String gameId1 = myGameIds[0];
        if (gameId1 != null && !gameId1.isEmpty()) {
            String url = "https://api.nlib.cc/nx/" + gameId1 + "/icon";
            Glide.with(this).load(url).into(imgFavGameFront);
            ((View)imgFavGameFront.getParent()).setVisibility(View.VISIBLE);
        } else {
            ((View)imgFavGameFront.getParent()).setVisibility(View.INVISIBLE);
        }
    }




    // ================================================================
    // SISTEMA DE MONEDAS: GUARDAR DATOS
    // ================================================================
    private void verificarYGuardar() {
        // 1. Revisar saldo
        SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int tickets = prefs.getInt("skip_tickets", 0);
        final int COSTO = 3;

        // 2. ¿Tiene suficientes monedas?
        if (tickets >= COSTO) {
            mostrarDialogoMonedas("Save Data?", COSTO, tickets, () -> {
                // A) PAGÓ: Descontar y Guardar
                int nuevoSaldo = tickets - COSTO;
                prefs.edit().putInt("skip_tickets", nuevoSaldo).apply();
                actualizarMonedasNube(nuevoSaldo);
                ejecutarAccionSave(); // <--- TU MÉTODO DE GUARDAR
            }, () -> {
                // B) NO PAGÓ: Ver Anuncio
                cargarAnuncioYGuardar(); // <--- TU MÉTODO DE ANUNCIO
            });
        } else {
            // 3. No tiene saldo: Anuncio directo
            cargarAnuncioYGuardar();
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
        ((TextView)view.findViewById(R.id.txtDialogMessage)).setText("Save changes instantly?");

        btnUse.setOnClickListener(v -> {
            dialog.dismiss();

            // --- INICIO SONIDO Y VIBRACIÓN ---
            try {
                MediaPlayer mp = MediaPlayer.create(this, R.raw.coin);
                if (mp != null) {
                    mp.setOnCompletionListener(MediaPlayer::release);
                    mp.start();
                }
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            // ---------------------------------

            onPay.run();
        });
        btnWatch.setOnClickListener(v -> { dialog.dismiss(); onAd.run(); });
        dialog.show();
    }

    private void actualizarMonedasNube(int saldo) {
        if (mAuth.getCurrentUser() != null) {
            // 1. Guardamos bandera de "pendiente" por seguridad antes de intentar
            getSharedPreferences("UserRewards", MODE_PRIVATE).edit().putBoolean("needs_sync", true).apply();

            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update("coins", saldo)
                    .addOnSuccessListener(aVoid -> {
                        // 2. Si hubo éxito, quitamos la bandera
                        getSharedPreferences("UserRewards", MODE_PRIVATE).edit().putBoolean("needs_sync", false).apply();
                    })
                    .addOnFailureListener(e -> {
                        // 3. Si falla, la bandera se queda en true para que MainActivity reintente luego
                    });
        } else {
            // Si no hay usuario, marcamos para subir luego
            getSharedPreferences("UserRewards", MODE_PRIVATE).edit().putBoolean("needs_sync", true).apply();
        }
    }

}