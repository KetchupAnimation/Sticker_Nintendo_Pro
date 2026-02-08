package com.ketchupstudios.Switchstickerapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log; // IMPORTANTE: Para ver los errores
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

public class FriendCardDialog extends Dialog {

    private String friendName, friendCode, friendThemeUrl, friendColor, friendUid;
    private static final String ID_IMG_BASE = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/ID/";

    private boolean isFrontShowing = true;
    private boolean isFlipping = false;

    // Vistas
    private View cardFlipContainer;
    private ImageView imgCardBg;
    private RelativeLayout containerFront, containerBack;
    private TextView txtName, txtCode, txtNoGames;
    private ImageView game1, game2, game3;

    public FriendCardDialog(@NonNull Context context, String name, String code, String themeImage, String color, String uid) {
        super(context);
        this.friendName = name;
        this.friendCode = code;
        this.friendThemeUrl = ID_IMG_BASE + themeImage;
        this.friendColor = color;
        this.friendUid = uid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.item_friend);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        inicializarVistas();
        cargarDatosIniciales();

        // DEPURACIÓN: Verificamos si llega el UID
        Log.e("FRIEND_DEBUG", "Intentando conectar con UID: " + friendUid);

        if (friendUid != null && !friendUid.isEmpty()) {
            escucharCambiosEnTiempoReal(friendUid);
        } else {
            Log.e("FRIEND_DEBUG", "ERROR FATAL: El UID llegó vacío. No se pueden buscar actualizaciones.");
        }
    }

    private void inicializarVistas() {
        cardFlipContainer = findViewById(R.id.cardFlipContainer);
        imgCardBg = findViewById(R.id.imgFriendBg);
        containerFront = findViewById(R.id.containerFront);
        containerBack = findViewById(R.id.containerBack);
        txtName = findViewById(R.id.txtFriendName);
        txtCode = findViewById(R.id.txtFriendCode);
        txtNoGames = findViewById(R.id.txtNoGames);
        game1 = findViewById(R.id.game1);
        game2 = findViewById(R.id.game2);
        game3 = findViewById(R.id.game3);

        View actionButtons = findViewById(R.id.actionButtons);
        if (actionButtons != null) actionButtons.setVisibility(View.GONE);

        if (cardFlipContainer != null) {
            cardFlipContainer.setOnClickListener(v -> girarTarjeta());
        }
    }

    private void cargarDatosIniciales() {
        if (txtName != null) txtName.setText(friendName);
        if (txtCode != null) txtCode.setText(friendCode);
        if (imgCardBg != null) {
            Glide.with(getContext()).load(friendThemeUrl).into(imgCardBg);
        }
    }

    private void escucharCambiosEnTiempoReal(String uid) {
        Log.e("FRIEND_DEBUG", "Iniciando Listener en: users/" + uid);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("FRIEND_DEBUG", "Error de conexión: " + error.getMessage());
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            Log.e("FRIEND_DEBUG", "¡DATOS ENCONTRADOS! Procesando...");

                            // 1. Verificamos los nombres exactos de los campos
                            // INTENTA LEER LOS CAMPOS COMUNES
                            String newName = snapshot.getString("user_name");
                            // Si retorna null, intentamos con "name" (por si acaso)
                            if (newName == null) newName = snapshot.getString("name");

                            String newCode = snapshot.getString("user_code");
                            if (newCode == null) newCode = snapshot.getString("friend_code");

                            Log.e("FRIEND_DEBUG", "Nombre obtenido: " + newName);
                            Log.e("FRIEND_DEBUG", "Código obtenido: " + newCode);

                            if (newName != null) txtName.setText(newName);
                            if (newCode != null) txtCode.setText(newCode);

                            // 2. Juegos
                            List<String> games = (List<String>) snapshot.get("favorite_games");
                            actualizarJuegos(games);
                        } else {
                            Log.e("FRIEND_DEBUG", "El documento NO EXISTE en Firebase. Revisa el UID.");
                        }
                    }
                });
    }

    private void actualizarJuegos(List<String> games) {
        String g1 = "", g2 = "", g3 = "";

        if (games != null) {
            Log.e("FRIEND_DEBUG", "Juegos encontrados: " + games.size());
            if (games.size() > 0) g1 = games.get(0);
            if (games.size() > 1) g2 = games.get(1);
            if (games.size() > 2) g3 = games.get(2);
        } else {
            Log.e("FRIEND_DEBUG", "La lista de juegos es NULL");
        }

        cargarImagenJuego(game1, g1);
        cargarImagenJuego(game2, g2);
        cargarImagenJuego(game3, g3);

        boolean hayJuegos = (!g1.isEmpty() || !g2.isEmpty() || !g3.isEmpty());
        if (txtNoGames != null) {
            txtNoGames.setVisibility(hayJuegos ? View.GONE : View.VISIBLE);
        }
    }

    private void cargarImagenJuego(ImageView img, String gameId) {
        if (img == null) return;
        if (gameId != null && !gameId.isEmpty()) {
            String url = "https://api.nlib.cc/nx/" + gameId + "/icon";
            Glide.with(getContext()).load(url).into(img);
        } else {
            img.setImageDrawable(null);
        }
    }

    private void girarTarjeta() {
        if (isFlipping) return;
        isFlipping = true;

        final float startScale = 1.0f;
        final float endScale = 0.9f;

        ObjectAnimator flip1 = ObjectAnimator.ofFloat(cardFlipContainer, "rotationY", 0f, 90f);
        flip1.setDuration(200);
        flip1.setInterpolator(new AccelerateDecelerateInterpolator());
        cardFlipContainer.animate().scaleX(endScale).scaleY(endScale).setDuration(200).start();

        flip1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFrontShowing) {
                    containerFront.setVisibility(View.GONE);
                    containerBack.setVisibility(View.VISIBLE);
                } else {
                    containerBack.setVisibility(View.GONE);
                    containerFront.setVisibility(View.VISIBLE);
                }
                isFrontShowing = !isFrontShowing;
                cardFlipContainer.setRotationY(-90f);
                ObjectAnimator flip2 = ObjectAnimator.ofFloat(cardFlipContainer, "rotationY", -90f, 0f);
                flip2.setDuration(200);
                flip2.setInterpolator(new AccelerateDecelerateInterpolator());
                cardFlipContainer.animate().scaleX(startScale).scaleY(startScale).setDuration(200).start();
                flip2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isFlipping = false;
                    }
                });
                flip2.start();
            }
        });
        flip1.start();
    }
}