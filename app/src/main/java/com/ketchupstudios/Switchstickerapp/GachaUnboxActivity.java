package com.ketchupstudios.Switchstickerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GachaUnboxActivity extends AppCompatActivity {

    private String packId;
    private StickerPack currentPack;
    private RecyclerView recycler;
    private TextView txtCounter;
    private TextView txtTitle;
    private Button btnAddToWa;
    public int unlockedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gacha_unbox);

        packId = getIntent().getStringExtra("PACK_ID");

        if (Config.packs != null) {
            for (StickerPack p : Config.packs) {
                if (p.identifier.equals(packId)) {
                    currentPack = p;
                    break;
                }
            }
        }

        txtTitle = findViewById(R.id.txtUnboxTitle);
        txtCounter = findViewById(R.id.txtUnboxCounter);
        btnAddToWa = findViewById(R.id.btnAddToWa);
        recycler = findViewById(R.id.recyclerGachaUnbox);

        if (currentPack != null) {
            txtTitle.setText(currentPack.name); // Ponemos el nombre real del pack
            recycler.setLayoutManager(new GridLayoutManager(this, 4)); // 4 columnas se ve mejor para stickers

            // 👇 CONECTAMOS EL ADAPTADOR MÁGICO 👇
            GachaUnboxAdapter adapter = new GachaUnboxAdapter(this, currentPack, this);
            recycler.setAdapter(adapter);
        }

        // Lógica para enviar a WhatsApp (Se mostrará cuando desbloqueen 3)
        btnAddToWa.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
            intent.putExtra("sticker_pack_id", currentPack.identifier);
            intent.putExtra("sticker_pack_authority", BuildConfig.APPLICATION_ID + ".provider");
            intent.putExtra("sticker_pack_name", currentPack.name);
            try {
                startActivityForResult(intent, 200);
            } catch (Exception e) {
                CustomToast.makeText(this, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método que el Adaptador llamará cada vez que toquen un sticker gris
    public void onStickerUnlocked() {
        unlockedCount++;
        txtCounter.setText("Choose 3 to unlock: " + unlockedCount + "/3");

        if (unlockedCount >= 3) {
            txtCounter.setText("Pack Unlocked! 🎉");
            txtCounter.setTextColor(android.graphics.Color.parseColor("#25D366"));
            btnAddToWa.setVisibility(View.VISIBLE); // ¡Mostramos el botón!
        }
    }
}