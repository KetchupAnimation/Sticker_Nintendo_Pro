package com.ketchupstudios.Switchstickerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GachaUnboxActivity extends AppCompatActivity {

    private String packId;
    private StickerPack currentPack;
    private RecyclerView recycler;
    private TextView txtCounter;
    private LinearLayout buttonsContainer;
    private Button btnAddToWa;
    private GachaUnboxAdapter adapter; // <--- Referencia al adaptador
    public int unlockedCount = 0;
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

        TextView txtTitle = findViewById(R.id.txtUnboxTitle);
        TextView txtAuthor = findViewById(R.id.txtDetailAuthor);
        ImageView imgTray = findViewById(R.id.imgPackTray);
        txtCounter = findViewById(R.id.txtUnboxCounter);
        btnAddToWa = findViewById(R.id.btnAddToWa);
        buttonsContainer = findViewById(R.id.buttonsContainer);
        recycler = findViewById(R.id.recyclerGachaUnbox);
        Button btnSupport = findViewById(R.id.btnSupportArtist);

        if (currentPack != null) {
            txtTitle.setText(currentPack.name);
            txtAuthor.setText("x " + (currentPack.publisher != null ? currentPack.publisher : "UnTal3D"));

            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            String trayUrl = baseUrl + currentPack.identifier + "/" + currentPack.trayImageFile;
            Glide.with(this).load(trayUrl).into(imgTray);

            if (currentPack.artistLink != null && !currentPack.artistLink.isEmpty()) {
                btnSupport.setVisibility(View.VISIBLE);
                btnSupport.setOnClickListener(v -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(currentPack.artistLink));
                        startActivity(i);
                    } catch (Exception e) {}
                });
            } else {
                btnSupport.setVisibility(View.GONE);
            }

            recycler.setLayoutManager(new GridLayoutManager(this, 4));
            adapter = new GachaUnboxAdapter(this, currentPack, this);
            recycler.setAdapter(adapter);
        }

        btnAddToWa.setOnClickListener(v -> iniciarDescargaYEnvio());
    }

    public void onStickerUnlocked() {
        unlockedCount++;
        txtCounter.setText("Choose 3 to unlock: " + unlockedCount + "/3");
        if (unlockedCount >= 3) {
            txtCounter.setText("Pack Unlocked! 🎉");
            // Mostramos ambos botones de golpe
            buttonsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void iniciarDescargaYEnvio() {
        mostrarCargando();
        new Thread(() -> {
            try {
                // 1. EXTRAER SOLO LOS STICKERS TOCADOS POR EL USUARIO
                List<StickerPack.Sticker> unlockedStickers = new ArrayList<>();
                for (int i = 0; i < currentPack.stickers.size(); i++) {
                    if (adapter.isUnlocked[i]) {
                        unlockedStickers.add(currentPack.stickers.get(i));
                    }
                }

                // 2. CREAR UN MINI-PACK FALSO PARA ENGAÑAR A WHATSAPP
                StickerPack waPack = new StickerPack();
                waPack.identifier = currentPack.identifier;
                waPack.name = currentPack.name;
                waPack.publisher = currentPack.publisher;
                waPack.trayImageFile = currentPack.trayImageFile;
                waPack.stickers = unlockedStickers; // Solo van los 3 o más elegidos

                // 3. DECIRLE A LA APP QUE ESTE ES EL PACK ACTIVO
                Config.selectedPack = waPack;

                // 4. DESCARGAR SOLO LOS ARCHIVOS SELECCIONADOS
                File dir = new File(getFilesDir(), "stickers/" + currentPack.identifier);
                if (!dir.exists()) dir.mkdirs();
                String url = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + currentPack.identifier + "/";

                descargarArchivo(url + currentPack.trayImageFile, new File(dir, currentPack.trayImageFile));
                for (StickerPack.Sticker s : unlockedStickers) {
                    descargarArchivo(url + s.imageFile, new File(dir, s.imageFile));
                }

                runOnUiThread(() -> { cerrarCargando(); enviarIntentAWhatsApp(); });
            } catch (Exception e) {
                runOnUiThread(() -> { cerrarCargando(); CustomToast.makeText(this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show(); });
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
        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra("sticker_pack_id", currentPack.identifier);
        intent.putExtra("sticker_pack_authority", BuildConfig.APPLICATION_ID + ".provider");
        intent.putExtra("sticker_pack_name", currentPack.name);
        try { startActivityForResult(intent, 200); }
        catch (Exception e) { CustomToast.makeText(this, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show(); }
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


    // 👇 NUEVO: Método para mostrar la vista previa en grande 👇
    public void mostrarPreviewGrande(String urlImagen) {
        View v = getLayoutInflater().inflate(R.layout.dialog_sticker_preview, null);
        Glide.with(this).load(urlImagen).into((ImageView) v.findViewById(R.id.imgPreviewBig));

        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this).setView(v).create();
        v.setOnClickListener(view -> d.dismiss());

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setDimAmount(0.7f);
        }

        d.show();
        v.setScaleX(0.5f); v.setScaleY(0.5f); v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200)
                .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
    }
}