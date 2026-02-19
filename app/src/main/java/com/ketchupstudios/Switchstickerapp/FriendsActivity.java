package com.ketchupstudios.Switchstickerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

// IMPORTS FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView rvFriends;
    private View emptyState, btnScan;
    private TextView titleFriends;
    private FriendsAdapter adapter;
    private List<Friend> friendList = new ArrayList<>();
    private Map<String, String> themeImageMap = new HashMap<>();
    private Map<String, String> themeColorMap = new HashMap<>();

    private static final int MAX_FRIENDS = 30;

    // FIREBASE
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        setContentView(R.layout.activity_friends);

        // INICIALIZAR FIREBASE
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvFriends = findViewById(R.id.rvFriends);
        emptyState = findViewById(R.id.emptyState);
        btnScan = findViewById(R.id.btnScanQr);
        titleFriends = findViewById(R.id.titleFriends);

        cargarAmigosLocal(); // Carga inicial del celular

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
// --- REEMPLAZAR LA INICIALIZACIÓN DEL ADAPTER ---
        adapter = new FriendsAdapter(friendList, themeImageMap, themeColorMap, this, new FriendsAdapter.OnFriendActionListener() {
            @Override
            public void onDelete(int position) {
                confirmarBorrado(position);
            }

            @Override
            public void onDataUpdated() {
                guardarAmigosLocal();
                subirAmigosANube();
            }

            @Override
            public void onAutoScrollRequest(int position) {
                if (position >= 0 && position < friendList.size()) {
                    rvFriends.post(() -> rvFriends.smoothScrollToPosition(position));
                }
            }

            // --- ESTO ES LO NUEVO QUE FALTABA ---
            @Override
            public void onNewNotification() {
                // Guardamos en la memoria que hay una novedad
                getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("has_notification", true)
                        .apply();

                // Cuando el usuario vuelva al MainActivity, el onResume leerá este 'true' y prenderá la campana.
            }
        });

        rvFriends.setAdapter(adapter);

        actualizarUI();

        btnScan.setOnClickListener(v -> {
            if (friendList.size() >= MAX_FRIENDS) {
                CustomToast.makeText(this, "List is full (Max 30)", Toast.LENGTH_SHORT).show();
            } else {
                iniciarEscaner();
            }
        });

        new Thread(this::descargarMapaTemas).start();

        if (getIntent().getBooleanExtra("AUTO_SCAN", false)) {
            if (friendList.size() < MAX_FRIENDS) {
                rvFriends.postDelayed(this::iniciarEscaner, 300);
            } else {
                CustomToast.makeText(this, "List is full (Max 30)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- NUEVO: SUBIR A FIREBASE ---
    private void subirAmigosANube() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            // Firestore acepta listas de objetos personalizados si son simples
            data.put("friends_list", friendList);

            db.collection("users").document(user.getUid())
                    .set(data, SetOptions.merge()) // Merge para no borrar tus otros datos (juegos, nombre)
                    .addOnFailureListener(e -> CustomToast.makeText(this, "Sync Error", Toast.LENGTH_SHORT).show());
        }
    }

    private void descargarMapaTemas() {
        try {
            String jsonUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/battery_themes.json";
            URL url = new URL(jsonUrl + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);

            JSONObject json = new JSONObject(result.toString());
            JSONArray array = json.getJSONArray("id_themes");

            Map<String, String> newImageMap = new HashMap<>();
            Map<String, String> newColorMap = new HashMap<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                newImageMap.put(o.getString("id"), o.getString("image"));
                newColorMap.put(o.getString("id"), o.optString("color_bg", "#ca3537"));
            }

            runOnUiThread(() -> {
                themeImageMap = newImageMap;
                themeColorMap = newColorMap;
                if (adapter != null) adapter.updateThemeMaps(themeImageMap, themeColorMap);
            });

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void iniciarEscaner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a Friend Card QR");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                procesarCodigoQR(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void procesarCodigoQR(String qrContent) {
        try {
            if (qrContent.startsWith("SWITCH:")) {
                String[] parts = qrContent.split(":");
                if (parts.length < 2) return;

                String code = parts[1].trim();
                String name = (parts.length > 2) ? parts[2] : "New Friend";
                String themeId = (parts.length > 3) ? parts[3] : "default";
                String colorHex = (parts.length > 4) ? parts[4] : "#ca3537";

                String uid = "";
                if (parts.length > 5) {
                    uid = parts[5];
                }

                for (Friend f : friendList) {
                    if (f.code != null && f.code.trim().equalsIgnoreCase(code)) {
                        CustomToast.makeText(this, "Friend already added!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                try {
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.open);
                    if (mp != null) {
                        mp.start();
                        mp.setOnCompletionListener(MediaPlayer::release);
                    }
                } catch (Exception e) { e.printStackTrace(); }

                Friend newFriend = new Friend(name, code, themeId, colorHex, uid);
                friendList.add(newFriend);

                Collections.sort(friendList, (f1, f2) -> f1.name.compareToIgnoreCase(f2.name));

                // GUARDAR LOCAL Y NUBE (AQUÍ FALTABA LA NUBE)
                guardarAmigosLocal();
                subirAmigosANube();

                adapter.notifyDataSetChanged();
                actualizarUI();
                CustomToast.makeText(this, "Friend Added!", Toast.LENGTH_SHORT).show();

                // Animación
                int newPosition = friendList.indexOf(newFriend);
                if (newPosition != -1) {
                    rvFriends.scrollToPosition(newPosition);
                    rvFriends.postDelayed(() -> {
                        RecyclerView.ViewHolder holder = rvFriends.findViewHolderForAdapterPosition(newPosition);
                        if (holder != null && holder.itemView != null) {
                            View itemView = holder.itemView;
                            itemView.setScaleX(0f); itemView.setScaleY(0f); itemView.setAlpha(0f);
                            itemView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(600).setInterpolator(new OvershootInterpolator(1.5f)).start();
                        }
                    }, 100);
                }

            } else {
                CustomToast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            CustomToast.makeText(this, "Error reading QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarBorrado(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Friend")
                .setMessage("Are you sure you want to remove this friend?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    friendList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, friendList.size());

                    // GUARDAR Y SUBIR AL BORRAR
                    guardarAmigosLocal();
                    subirAmigosANube();

                    actualizarUI();
                    CustomToast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cargarAmigosLocal() {
        SharedPreferences prefs = getSharedPreferences("FriendsData", MODE_PRIVATE);
        String json = prefs.getString("friends_list", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Friend>>() {}.getType();
            friendList = gson.fromJson(json, type);
        }
        if (friendList == null) {
            friendList = new ArrayList<>();
        }
        Collections.sort(friendList, (f1, f2) -> f1.name.compareToIgnoreCase(f2.name));
    }

    private void guardarAmigosLocal() {
        SharedPreferences prefs = getSharedPreferences("FriendsData", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(friendList);
        prefs.edit().putString("friends_list", json).apply();
    }

    private void actualizarUI() {
        int count = friendList.size();
        titleFriends.setText("Friend List " + count + "/" + MAX_FRIENDS);

        if (count == 0) {
            emptyState.setVisibility(View.VISIBLE);
            rvFriends.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvFriends.setVisibility(View.VISIBLE);
        }

        if (count >= MAX_FRIENDS) {
            btnScan.setVisibility(View.GONE);
        } else {
            btnScan.setVisibility(View.VISIBLE);
        }
    }
}