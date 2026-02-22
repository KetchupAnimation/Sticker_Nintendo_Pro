package com.ketchupstudios.Switchstickerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GachaUnboxActivity extends AppCompatActivity {

    private String packId;
    public StickerPack currentPack;
    private RecyclerView recycler;
    private TextView txtCounter;
    private Button btnAddToWa;

    public int sessionUnlockedCount = 0;
    public int maxSessionUnlocks = 3;
    public Set<String> unlockedStickersSet;
    private SharedPreferences prefs;
    private AlertDialog dialogCarga;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gacha_unbox);

        packId = getIntent().getStringExtra("PACK_ID");
        if (Config.packs != null) {
            for (StickerPack p : Config.packs) {
                if (p.identifier.equals(packId)) { currentPack = p; break; }
            }
        }

        prefs = getSharedPreferences("GachaUnlocks", MODE_PRIVATE);
        unlockedStickersSet = new HashSet<>(prefs.getStringSet("pack_" + packId, new HashSet<>()));

        TextView txtTitle = findViewById(R.id.txtUnboxTitle);
        TextView txtAuthor = findViewById(R.id.txtDetailAuthor);
        ImageView imgTray = findViewById(R.id.imgPackTray);
        txtCounter = findViewById(R.id.txtUnboxCounter);
        btnAddToWa = findViewById(R.id.btnAddToWa);
        Button btnSupport = findViewById(R.id.btnSupportArtist);
        recycler = findViewById(R.id.recyclerGachaUnbox);

        if (currentPack != null) {
            txtTitle.setText(currentPack.name);
            txtAuthor.setText("x " + (currentPack.publisher != null ? currentPack.publisher : "UnTal3D"));

            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            String trayUrl = baseUrl + currentPack.identifier + "/" + currentPack.trayImageFile;
            Glide.with(this).load(trayUrl).into(imgTray);

            // Mostrar botón de Soporte si hay link
            if (currentPack.artistLink != null && !currentPack.artistLink.isEmpty()) {
                btnSupport.setVisibility(View.VISIBLE);
                btnSupport.setOnClickListener(v -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(currentPack.artistLink));
                        startActivity(i);
                    } catch (Exception e) {}
                });
            }

            maxSessionUnlocks = Math.min(3, currentPack.stickers.size() - unlockedStickersSet.size());

            if (maxSessionUnlocks == 0) {
                txtCounter.setText("Pack Fully Unlocked! 🎉");
                btnAddToWa.setVisibility(View.VISIBLE);
            } else {
                txtCounter.setText("Choose " + maxSessionUnlocks + " to unlock: 0/" + maxSessionUnlocks);
            }

            recycler.setLayoutManager(new GridLayoutManager(this, 3));
            GachaUnboxAdapter adapter = new GachaUnboxAdapter(this, currentPack, this, unlockedStickersSet);
            recycler.setAdapter(adapter);
        }

        btnAddToWa.setOnClickListener(v -> iniciarDescargaYEnvio());
    }

    public void onStickerUnlocked(String imageFile) {
        sessionUnlockedCount++;
        unlockedStickersSet.add(imageFile);
        prefs.edit().putStringSet("pack_" + packId, unlockedStickersSet).apply();

        txtCounter.setText("Choose " + maxSessionUnlocks + " to unlock: " + sessionUnlockedCount + "/" + maxSessionUnlocks);

        if (sessionUnlockedCount >= maxSessionUnlocks) {
            if (unlockedStickersSet.size() == currentPack.stickers.size()) {
                txtCounter.setText("Pack Fully Unlocked! 🎉");
            } else {
                txtCounter.setText("Session Unlocked! 🎉");
            }
            btnAddToWa.setVisibility(View.VISIBLE);
        }
    }

    public void mostrarPreviewGrande(String urlImagen) {
        View v = getLayoutInflater().inflate(R.layout.dialog_sticker_preview, null);
        Glide.with(this).load(urlImagen).into((ImageView) v.findViewById(R.id.imgPreviewBig));

        AlertDialog d = new AlertDialog.Builder(this).setView(v).create();
        v.setOnClickListener(view -> d.dismiss());

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setDimAmount(0.7f);
        }

        d.show();
        v.setScaleX(0.5f); v.setScaleY(0.5f); v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200)
                .setInterpolator(new OvershootInterpolator()).start();
    }

    private void iniciarDescargaYEnvio() {
        mostrarCargando();
        new Thread(() -> {
            try {
                // 1. Filtrar los que el usuario ya ha ganado
                List<StickerPack.Sticker> unlockedList = new ArrayList<>();
                for (StickerPack.Sticker s : currentPack.stickers) {
                    if (unlockedStickersSet.contains(s.imageFile)) unlockedList.add(s);
                }

                // 2. Crear pack dinámico idéntico al original pero con menos stickers
                StickerPack waPack = new StickerPack();
                waPack.identifier = currentPack.identifier;
                waPack.name = currentPack.name;
                waPack.publisher = currentPack.publisher;
                waPack.trayImageFile = currentPack.trayImageFile;
                waPack.stickers = unlockedList;

                // 👇 EL HACK MAESTRO (CORREGIDO) 👇
                // Solo engañamos al selectedPack. Ya NO tocamos Config.packs para no borrar
                // el resto de tus siluetas de la memoria del celular.
                Config.selectedPack = waPack;

                // 3. Descargar los archivos físicos
                File dir = new File(getFilesDir(), "stickers/" + currentPack.identifier);
                if (!dir.exists()) dir.mkdirs();
                String url = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + currentPack.identifier + "/";

                descargarArchivo(url + currentPack.trayImageFile, new File(dir, currentPack.trayImageFile));
                for (StickerPack.Sticker s : unlockedList) {
                    descargarArchivo(url + s.imageFile, new File(dir, s.imageFile));
                }

                runOnUiThread(() -> { cerrarCargando(); enviarIntentAWhatsApp(); });
            } catch (Exception e) {
                runOnUiThread(() -> { cerrarCargando(); Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    private void descargarArchivo(String u, File d) throws Exception {
        InputStream in = new URL(u).openConnection().getInputStream();
        FileOutputStream out = new FileOutputStream(d);
        byte[] b = new byte[4096]; int c;
        while ((c = in.read(b)) != -1) out.write(b, 0, c);
        out.flush(); out.close(); in.close();
    }

    private void enviarIntentAWhatsApp() {
        Intent intent = new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra("sticker_pack_id", currentPack.identifier);
        intent.putExtra("sticker_pack_name", currentPack.name);

        // 👇 CORRECCIÓN 1: La autoridad exacta de tu Provider 👇
        intent.putExtra("sticker_pack_authority", "com.ketchupstudios.Switchstickerapp.stickercontentprovider");

        // 👇 CORRECCIÓN 2: Permiso obligatorio de lectura para WhatsApp 👇
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(intent, 200);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarCargando() {
        LinearLayout l = new LinearLayout(this); l.setPadding(50, 50, 50, 50); l.setGravity(Gravity.CENTER_VERTICAL); l.setBackgroundColor(Color.WHITE);
        ProgressBar p = new ProgressBar(this); TextView t = new TextView(this); t.setText("  Importing to WhatsApp..."); t.setTextColor(Color.BLACK);
        l.addView(p); l.addView(t);
        dialogCarga = new AlertDialog.Builder(this).setCancelable(false).setView(l).create();
        dialogCarga.show();
    }

    private void cerrarCargando() {
        if (!isFinishing() && !isDestroyed() && dialogCarga != null && dialogCarga.isShowing()) dialogCarga.dismiss();
    }
}