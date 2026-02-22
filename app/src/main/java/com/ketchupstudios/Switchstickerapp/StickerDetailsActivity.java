package com.ketchupstudios.Switchstickerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class StickerDetailsActivity extends AppCompatActivity {

    private InterstitialAd mInterstitialAd;
    private AlertDialog dialogCarga;
    private static final String AUTHORITY = "com.ketchupstudios.Switchstickerapp.stickercontentprovider";

    private int unlockedCount = 0;
    private boolean isEventExpired = false;

    // 👇 NUEVO: Variables para el control de Gacha 👇
    private boolean isGachaPack = false;
    private Set<String> unlockedGachaStickers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_details);

        TextView txtTitle = findViewById(R.id.txtDetailTitle);
        TextView txtAuthor = findViewById(R.id.txtDetailAuthor);
        ImageView imgPackTray = findViewById(R.id.imgPackTray);

        Button btnAdd = findViewById(R.id.btnAddToWhatsapp);
        Button btnSupport = findViewById(R.id.btnSupportArtist);
        ImageView btnShare = findViewById(R.id.btnShareApp);

        if (Config.selectedPack != null) {

            // 👇 1. DETECTAR SI ES UN PACK DEL GACHA 👇
            isGachaPack = Config.selectedPack.status != null && Config.selectedPack.status.equalsIgnoreCase("gacha");

            if (isGachaPack) {
                // Recuperar la memoria local de este pack específico
                unlockedGachaStickers = getSharedPreferences("GachaUnlocks", MODE_PRIVATE)
                        .getStringSet("pack_" + Config.selectedPack.identifier, new HashSet<>());

                // Actualizar botón
                btnAdd.setText("UNLOCKED (" + unlockedGachaStickers.size() + ")");

            } else if (Config.selectedPack.isEvent && Config.selectedPack.eventStartDate != null) {
                calcularStickersDesbloqueados();
                if (!isEventExpired && unlockedCount < Config.selectedPack.stickers.size()) {
                    btnAdd.setText("DAY (" + unlockedCount + ") + 1 COIN");
                }
            } else {
                unlockedCount = Config.selectedPack.stickers.size();
            }

            txtTitle.setText(Config.selectedPack.name);
            txtAuthor.setText("x " + Config.selectedPack.publisher);

            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            String trayUrl = baseUrl + Config.selectedPack.identifier + "/" + Config.selectedPack.trayImageFile;

            Glide.with(this).load(trayUrl).transform(new CenterCrop(), new RoundedCorners(16)).into(imgPackTray);

            btnShare.setOnClickListener(v -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    String shareMessage = "Check out these stickers by " + Config.selectedPack.name + "!\n";
                    shareMessage = shareMessage + "*Download here:* https://play.google.com/store/apps/details?id=" + getPackageName();
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share..."));
                } catch(Exception e) {}
            });

            String link = Config.selectedPack.artistLink;
            if (link != null && !link.isEmpty()) {
                btnSupport.setVisibility(View.VISIBLE);
                btnSupport.setOnClickListener(v -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(link));
                        startActivity(i);
                    } catch (Exception e) {
                        CustomToast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                btnSupport.setVisibility(View.GONE);
            }
        }

        RecyclerView rvGrid = findViewById(R.id.rvStickersGrid);
        rvGrid.setLayoutManager(new GridLayoutManager(this, 3));

        if (Config.selectedPack != null && Config.selectedPack.stickers != null) {
            rvGrid.setAdapter(new StickerGridAdapter(Config.selectedPack.stickers));
        }

        btnAdd.setOnClickListener(v -> {
            if (isGachaPack && unlockedGachaStickers.size() < 3) {
                CustomToast.makeText(this, "WhatsApp needs at least 3 unlocked stickers.", Toast.LENGTH_LONG).show();
            } else if (Config.selectedPack.isEvent && !isEventExpired && unlockedCount < 3) {
                CustomToast.makeText(this, "WhatsApp needs at least 3 unlocked stickers.", Toast.LENGTH_LONG).show();
            } else {
                iniciarCargaDeAnuncio(btnAdd);
            }
        });

        checkUpdateNote();
        checkMilestoneReward();
    }

    private void calcularStickersDesbloqueados() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(Config.selectedPack.eventStartDate);
            Date today = new Date();
            long diff = today.getTime() - startDate.getTime();
            long daysPassed = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;

            if (daysPassed < 1) { unlockedCount = 0; }
            else {
                if (daysPassed > Config.selectedPack.stickers.size()) {
                    isEventExpired = true;
                    unlockedCount = Config.selectedPack.stickers.size();
                } else {
                    unlockedCount = (int) daysPassed;
                    isEventExpired = false;
                }
            }
        } catch (Exception e) {
            unlockedCount = Config.selectedPack.stickers.size();
            isEventExpired = true;
        }
    }

    private class StickerGridAdapter extends RecyclerView.Adapter<StickerGridAdapter.ViewHolder> {
        private List<StickerPack.Sticker> stickerList;

        public StickerGridAdapter(List<StickerPack.Sticker> stickerList) { this.stickerList = stickerList; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_single, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StickerPack.Sticker sticker = stickerList.get(position);
            int diaActual = position + 1;
            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
            Glide.with(holder.itemView.getContext()).load(baseUrl + Config.selectedPack.identifier + "/" + sticker.imageFile).into(holder.image);

            // 👇 2. EL PINTADO INTELIGENTE (GACHA VS NORMAL) 👇
            if (isGachaPack) {
                holder.layoutRewardBadge.setVisibility(View.GONE);

                if (unlockedGachaStickers.contains(sticker.imageFile)) {
                    // DESBLOQUEADO (Color Completo)
                    holder.image.clearColorFilter();
                    holder.image.setAlpha(1.0f);
                    holder.itemView.setOnClickListener(v -> mostrarPreviewGrande(sticker));
                } else {
                    // BLOQUEADO (Silueta Gris)
                    holder.image.setColorFilter(Color.parseColor("#444444"), PorterDuff.Mode.SRC_IN);
                    holder.image.setAlpha(0.6f);
                    holder.itemView.setOnClickListener(v -> mostrarPopupGachaBloqueado());
                }
            }
            else if (Config.selectedPack.isEvent && !isEventExpired) {
                // LÓGICA ORIGINAL DEL EVENTO...
                SharedPreferences milestonePrefs = getSharedPreferences("EventMilestones", MODE_PRIVATE);
                boolean yaReclamado = milestonePrefs.getBoolean("claimed_" + Config.selectedPack.identifier + "_" + diaActual, false);

                if (diaActual % 5 == 0) {
                    holder.layoutRewardBadge.setVisibility(View.VISIBLE);
                    if (yaReclamado) {
                        holder.layoutRewardBadge.setBackgroundResource(R.drawable.bg_badge_red);
                        holder.itemView.setOnClickListener(v -> mostrarPopupRegaloWallpaper(diaActual, true));
                    } else if (diaActual < unlockedCount) {
                        holder.layoutRewardBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#424242")));
                        holder.itemView.setOnClickListener(v -> mostrarPopupPerdido());
                    } else if (diaActual == unlockedCount) {
                        holder.layoutRewardBadge.setBackgroundResource(R.drawable.bg_badge_red);
                        holder.itemView.setOnClickListener(v -> mostrarPopupRegaloWallpaper(diaActual, false));
                    } else {
                        holder.layoutRewardBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#212121")));
                        holder.itemView.setOnClickListener(v -> CustomToast.makeText(StickerDetailsActivity.this, "Locked until Day " + diaActual, Toast.LENGTH_SHORT).show());
                    }
                } else {
                    holder.layoutRewardBadge.setVisibility(View.GONE);
                }

                if (position >= unlockedCount) {
                    holder.image.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                    holder.image.setAlpha(0.3f);
                    if (diaActual % 5 != 0) holder.itemView.setOnClickListener(v -> CustomToast.makeText(StickerDetailsActivity.this, "Wait " + (diaActual - unlockedCount) + " days", Toast.LENGTH_SHORT).show());
                } else {
                    holder.image.clearColorFilter();
                    holder.image.setAlpha(1.0f);
                    if (diaActual % 5 != 0) holder.itemView.setOnClickListener(v -> mostrarPreviewGrande(sticker));
                }
            } else {
                holder.layoutRewardBadge.setVisibility(View.GONE);
                holder.image.clearColorFilter();
                holder.image.setAlpha(1.0f);
                holder.itemView.setOnClickListener(v -> mostrarPreviewGrande(sticker));
            }
        }
        @Override public int getItemCount() { return stickerList.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image; View layoutRewardBadge;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.sticker_image);
                layoutRewardBadge = itemView.findViewById(R.id.layoutRewardBadge);
            }
        }
    }

    // 👇 3. POPUP DE SILUETA DE GACHA 👇
    private void mostrarPopupGachaBloqueado() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_missed_event, null);
        dialog.setContentView(view);
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Magia para cambiar los textos sin conocer los IDs exactos de tu XML
        modificarTextosParaGacha((ViewGroup) view);

        View btnClose = view.findViewById(R.id.btnGotItMissed);
        if(btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void modificarTextosParaGacha(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            // Evitamos tocar el texto del botón por si quieres que diga "Got it"
            if (v instanceof TextView && v.getId() != R.id.btnGotItMissed) {
                TextView tv = (TextView) v;
                // Si el texto es corto, seguro es el título
                if (tv.getText().length() < 20) {
                    tv.setText("LOCKED STICKER");
                } else { // Si es largo, es la descripción
                    tv.setText("Play the Gacha roulette on the Home screen to reveal this exclusive sticker!");
                }
            } else if (v instanceof ViewGroup) {
                modificarTextosParaGacha((ViewGroup) v);
            }
        }
    }

    private void mostrarPopupPerdido() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        android.view.ViewGroup wrapper = new android.widget.RelativeLayout(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_missed_event, wrapper, false);
        android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) view.getLayoutParams();
        params.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
        wrapper.addView(view);

        dialog.setContentView(wrapper);
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View btnClose = view.findViewById(R.id.btnGotItMissed);
        if(btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void mostrarPopupRegaloWallpaper(int dia, boolean yaReclamado) {
        // Lógica original del regalo...
        android.app.Dialog dialog = new android.app.Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_update_note, null);
        dialog.setContentView(view);
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView imgNote = view.findViewById(R.id.imgUpdateNote);
        Button btnAction = view.findViewById(R.id.btnGotIt);
        View holoView = view.findViewById(R.id.holoEffectReward);
        if (holoView != null) holoView.setVisibility(View.VISIBLE);

        String wallpaperId = "";
        Config.Wallpaper wallPremio = null;
        String currentPackId = Config.selectedPack.identifier.toLowerCase();

        for (Config.Wallpaper w : Config.wallpapers) {
            if (w.rewardDay.equals(String.valueOf(dia)) && w.tags != null && w.tags.contains(currentPackId)) {
                wallPremio = w; wallpaperId = w.imageFile; break;
            }
        }
        if (wallpaperId.isEmpty()) {
            for (Config.Wallpaper w : Config.wallpapers) {
                if (w.rewardDay.equals(String.valueOf(dia))) { wallPremio = w; wallpaperId = w.imageFile; break; }
            }
            if (wallpaperId.isEmpty()) wallpaperId = "wall_29.png";
        }

        if (wallPremio != null && wallPremio.colorBg != null) {
            try { imgNote.setBackgroundColor(Color.parseColor(wallPremio.colorBg)); }
            catch (Exception e) { imgNote.setBackgroundColor(Color.parseColor("#ce3a38")); }
        }

        String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1);
        Glide.with(this).load(baseUrl + "wallpappers/" + wallpaperId).into(imgNote);

        if (yaReclamado) {
            btnAction.setText("Already in favorites! ❤️");
            btnAction.setOnClickListener(v -> dialog.dismiss());
        } else {
            btnAction.setText("Claim Reward!");
            final String finalId = wallpaperId;
            btnAction.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
                int tickets = prefs.getInt("skip_tickets", 0);
                if (tickets >= 3) {
                    dialog.hide();
                    mostrarDialogoGastarMonedas("Unlock Reward?", 3, tickets, () -> {
                        prefs.edit().putInt("skip_tickets", tickets - 3).apply();
                        CustomToast.makeText(this, "Redeemed! Reward Unlocked 🔓", Toast.LENGTH_SHORT).show();
                        reclamarRecompensaAhora(finalId, dia, dialog);
                    }, () -> {
                        dialog.show();
                        cargarAnuncioParaRecompensa(btnAction, finalId, dia, dialog);
                    });
                } else {
                    cargarAnuncioParaRecompensa(btnAction, finalId, dia, dialog);
                }
            });
        }
        dialog.setCancelable(true);
        view.setScaleX(0.5f); view.setScaleY(0.5f); view.setAlpha(0f);
        dialog.setOnShowListener(d -> view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(500).setInterpolator(new OvershootInterpolator()).start());
        dialog.show();
    }

    private void checkMilestoneReward() {
        if (Config.selectedPack == null) return;
        if (Config.selectedPack.isEvent && !isEventExpired) {
            SharedPreferences milestonePrefs = getSharedPreferences("EventMilestones", MODE_PRIVATE);
            int currentMilestone = (unlockedCount / 5) * 5;
            if (currentMilestone > 0 && currentMilestone == unlockedCount) {
                if (!milestonePrefs.getBoolean("claimed_" + Config.selectedPack.identifier + "_" + currentMilestone, false)) {
                    mostrarPopupRegaloWallpaper(currentMilestone, false);
                }
            }
        }
    }

    private void iniciarCargaDeAnuncio(Button btn) {
        btn.setEnabled(false);
        btn.setText("Loading Ad...");
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-9087203932210009/2214350595", adRequest, new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        if (Config.selectedPack.isEvent) darTicketDeRecompensa();
                        restaurarBoton(btn); iniciarDescargaYEnvio();
                    }
                    @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                        mInterstitialAd = null;
                        if (Config.selectedPack.isEvent) darTicketDeRecompensa();
                        restaurarBoton(btn); iniciarDescargaYEnvio();
                    }
                });
                mInterstitialAd.show(StickerDetailsActivity.this);
            }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null; restaurarBoton(btn); iniciarDescargaYEnvio();
            }
        });
    }

    private void darTicketDeRecompensa() {
        SharedPreferences prefs = getSharedPreferences("UserRewards", MODE_PRIVATE);
        int t = prefs.getInt("skip_tickets", 0) + 1;
        prefs.edit().putInt("skip_tickets", t).apply();
        CustomToast.makeText(this, "¡You win 1 coin! (Total: " + t + ")", Toast.LENGTH_SHORT).show();
    }

    private void restaurarBoton(Button btn) {
        if (isGachaPack) {
            btn.setText("DAY (" + unlockedGachaStickers.size() + ")");
        } else if (Config.selectedPack.isEvent && !isEventExpired && unlockedCount < Config.selectedPack.stickers.size()) {
            btn.setText("DAY (" + unlockedCount + ") + 1 COIN");
        } else {
            btn.setText("Add to WhatsApp");
        }
        btn.setEnabled(true);
        btn.setAlpha(1.0f);
    }

    private void checkUpdateNote() {
        if (Config.selectedPack != null && Config.selectedPack.updateNoteImage != null && !Config.selectedPack.updateNoteImage.isEmpty()) {
            SharedPreferences p = getSharedPreferences("UpdateNotesPrefs", MODE_PRIVATE);
            if (!p.getBoolean("seen_" + Config.selectedPack.identifier + "_" + Config.selectedPack.updateNoteImage, false)) {
                showUpdateNoteDialog(Config.selectedPack.updateNoteImage, Config.selectedPack.identifier, p);
            }
        }
    }

    private void showUpdateNoteDialog(String img, String id, SharedPreferences p) {
        android.app.Dialog d = new android.app.Dialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_update_note, null);
        d.setContentView(v);
        if(d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Glide.with(this).load(Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + Config.selectedPack.identifier + "/" + img).into((ImageView) v.findViewById(R.id.imgUpdateNote));
        v.findViewById(R.id.btnGotIt).setOnClickListener(view -> { d.dismiss(); p.edit().putBoolean("seen_" + id + "_" + img, true).apply(); });
        d.setCancelable(false);
        v.setScaleX(0.5f); v.setScaleY(0.5f); v.setAlpha(0f);
        d.setOnShowListener(dialog -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400).setInterpolator(new OvershootInterpolator()).start());
        d.show();
    }

    // 👇 4. HACK MAESTRO DE DESCARGA PARA WHATSAPP 👇
    private void iniciarDescargaYEnvio() {
        mostrarCargando();
        new Thread(() -> {
            try {
                if (isGachaPack) {
                    List<StickerPack.Sticker> unlockedList = new ArrayList<>();
                    for (StickerPack.Sticker s : Config.selectedPack.stickers) {
                        if (unlockedGachaStickers.contains(s.imageFile)) {
                            unlockedList.add(s);
                        }
                    }
                    StickerPack waPack = new StickerPack();
                    waPack.identifier = Config.selectedPack.identifier;
                    waPack.name = Config.selectedPack.name;
                    waPack.publisher = Config.selectedPack.publisher;
                    waPack.trayImageFile = Config.selectedPack.trayImageFile;
                    waPack.stickers = unlockedList;

                    Config.selectedPack = waPack; // Engañamos a WhatsApp
                }

                descargarArchivos();
                runOnUiThread(() -> { cerrarCargando(); enviarIntentAWhatsApp(); });
            } catch (Exception e) {
                runOnUiThread(() -> { cerrarCargando(); CustomToast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    private void descargarArchivos() throws Exception {
        File dir = new File(getFilesDir(), "stickers/" + Config.selectedPack.identifier);
        if (!dir.exists()) dir.mkdirs();
        String url = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + Config.selectedPack.identifier + "/";
        descargarArchivo(url + Config.selectedPack.trayImageFile, new File(dir, Config.selectedPack.trayImageFile));

        int limit;
        if (isGachaPack) {
            limit = Config.selectedPack.stickers.size(); // Ya está filtrado por el hack
        } else if (Config.selectedPack.isEvent) {
            limit = unlockedCount;
        } else {
            limit = Config.selectedPack.stickers.size();
        }

        for (int i = 0; i < limit; i++) {
            descargarArchivo(url + Config.selectedPack.stickers.get(i).imageFile, new File(dir, Config.selectedPack.stickers.get(i).imageFile));
        }
    }

    private void descargarArchivo(String u, File d) throws Exception {
        InputStream in = new URL(u).openConnection().getInputStream();
        FileOutputStream out = new FileOutputStream(d);
        byte[] b = new byte[4096]; int c;
        while ((c = in.read(b)) != -1) out.write(b, 0, c);
        out.flush(); out.close(); in.close();
    }

    private void enviarIntentAWhatsApp() {
        Intent i = new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        i.putExtra("sticker_pack_id", Config.selectedPack.identifier);
        i.putExtra("sticker_pack_name", Config.selectedPack.name);
        i.putExtra("sticker_pack_authority", AUTHORITY);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(i, 1); } catch (Exception e) { CustomToast.makeText(this, "WhatsApp not installed", Toast.LENGTH_LONG).show(); }
    }

    private void mostrarCargando() {
        LinearLayout l = new LinearLayout(this); l.setPadding(50, 50, 50, 50); l.setGravity(Gravity.CENTER_VERTICAL); l.setBackgroundColor(Color.WHITE);
        ProgressBar p = new ProgressBar(this); TextView t = new TextView(this); t.setText("  Importing to WhatsApp..."); t.setTextColor(Color.BLACK);
        l.addView(p); l.addView(t);
        dialogCarga = new AlertDialog.Builder(this).setCancelable(false).setView(l).create();
        dialogCarga.show();
    }

    private void cerrarCargando() {
        if (!isFinishing() && !isDestroyed() && dialogCarga != null && dialogCarga.isShowing()) {
            try { dialogCarga.dismiss(); } catch (IllegalArgumentException e) { Log.e("StickerDetails", "Error al cerrar diálogo: " + e.getMessage()); }
        }
    }

    public void mostrarPreviewGrande(StickerPack.Sticker s) {
        View v = getLayoutInflater().inflate(R.layout.dialog_sticker_preview, null);
        Glide.with(this).load(Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + Config.selectedPack.identifier + "/" + s.imageFile).into((ImageView) v.findViewById(R.id.imgPreviewBig));
        AlertDialog d = new AlertDialog.Builder(this).setView(v).create();
        v.setOnClickListener(view -> d.dismiss());
        if (d.getWindow() != null) { d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); d.getWindow().setDimAmount(0.7f); }
        d.show();
        v.setScaleX(0.5f); v.setScaleY(0.5f); v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
    }

    private void cargarAnuncioParaRecompensa(Button btn, String wallId, int dia, android.app.Dialog dialogPadre) {
        btn.setEnabled(false); btn.setText("Loading Ad...");
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, Config.ADMOB_INTERSTITIAL_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() { mInterstitialAd = null; reclamarRecompensaAhora(wallId, dia, dialogPadre); }
                    @Override public void onAdFailedToShowFullScreenContent(AdError adError) { mInterstitialAd = null; reclamarRecompensaAhora(wallId, dia, dialogPadre); }
                });
                mInterstitialAd.show(StickerDetailsActivity.this);
            }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null; CustomToast.makeText(StickerDetailsActivity.this, "Ad failed, giving reward anyway...", Toast.LENGTH_SHORT).show(); reclamarRecompensaAhora(wallId, dia, dialogPadre);
            }
        });
    }

    private void reclamarRecompensaAhora(String finalId, int dia, android.app.Dialog dialog) {
        SharedPreferences favPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favs = new HashSet<>(favPrefs.getStringSet("fav_wallpapers_ids", new HashSet<>()));
        favs.add(finalId);
        favPrefs.edit().putStringSet("fav_wallpapers_ids", favs).apply();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) FirebaseFirestore.getInstance().collection("users").document(user.getUid()).update("fav_wallpapers", FieldValue.arrayUnion(finalId));

        getSharedPreferences("EventMilestones", MODE_PRIVATE).edit().putBoolean("claimed_" + Config.selectedPack.identifier + "_" + dia, true).apply();

        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        RecyclerView rvGrid = findViewById(R.id.rvStickersGrid);
        if (rvGrid.getAdapter() != null) rvGrid.getAdapter().notifyDataSetChanged();
        CustomToast.makeText(this, "Reward added to favorites!", Toast.LENGTH_LONG).show();
    }

    private void mostrarDialogoGastarMonedas(String title, int cost, int balance, Runnable onUseCoins, Runnable onWatchAd) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_spend_coins, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView txtTitle = view.findViewById(R.id.txtDialogTitle);
        TextView txtMsg = view.findViewById(R.id.txtDialogMessage);
        TextView txtBal = view.findViewById(R.id.txtCurrentBalance);
        Button btnUse = view.findViewById(R.id.btnUseCoins);
        TextView btnAd = view.findViewById(R.id.btnWatchAd);

        txtTitle.setText(title);
        txtMsg.setText("Use " + cost + " coins to unlock this item instantly without ads.");
        txtBal.setText("Balance: " + balance + " coins");
        btnUse.setText("USE " + cost + " COINS");

        btnUse.setOnClickListener(v -> { dialog.dismiss(); if (onUseCoins != null) onUseCoins.run(); });
        btnAd.setOnClickListener(v -> { dialog.dismiss(); if (onWatchAd != null) onWatchAd.run(); });
        dialog.show();
    }
}