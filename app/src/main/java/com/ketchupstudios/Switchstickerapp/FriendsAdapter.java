package com.ketchupstudios.Switchstickerapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private SoundPool soundPool;
    private int soundIdNoti;
    private boolean soundLoaded = false;
    private int currentSelectedPosition = 0;

    private List<Friend> friends;
    private Context context;
    private Map<String, String> themeImages;
    private Map<String, String> themeColors;
    private OnFriendActionListener actionListener;

    public interface OnFriendActionListener {
        void onDelete(int position);
        void onDataUpdated();
        void onAutoScrollRequest(int position);
        void onNewNotification();
    }

    public FriendsAdapter(List<Friend> friends, Map<String, String> themeImages, Map<String, String> themeColors, Context context, OnFriendActionListener listener) {
        this.friends = friends;
        this.themeImages = themeImages;
        this.themeColors = themeColors;
        this.context = context;
        this.actionListener = listener;
    }

    public void updateThemeMaps(Map<String, String> newImages, Map<String, String> newColors) {
        this.themeImages = newImages;
        this.themeColors = newColors;
        notifyDataSetChanged();
    }

    public void setCurrentSelectedPosition(int position) {
        this.currentSelectedPosition = position;
    }

    private void reproducirSonidoNoti() {
        if (soundPool != null && soundLoaded) {
            soundPool.play(soundIdNoti, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);

        if (soundPool == null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build();
            soundIdNoti = soundPool.load(context, R.raw.noti, 1);
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status == 0) soundLoaded = true;
            });
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() { return friends.size(); }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private boolean isValidContextForGlide(Context context) {
        if (context == null) return false;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        return true;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtCode, txtNoGames;
        ImageView btnCopy, btnDelete, imgBg;
        View containerFront, containerBack, actionButtons;
        ImageView game1, game2, game3;
        View gamesContainer;
        LinearLayout statusContainer;
        TextView txtStatusTitle, txtStatusGame;
        FrameLayout frontGameContainer;
        ImageView imgFavGameFront;
        ImageView imgFavGameBorder;

        androidx.cardview.widget.CardView reactionsPanel;
        RecyclerView rvReactions;
        FrameLayout reactionsLayer;

        boolean isFrontShowing = true;
        boolean isFlipping = false;
        int currentColor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtFriendName);
            txtCode = itemView.findViewById(R.id.txtFriendCode);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            imgBg = itemView.findViewById(R.id.imgFriendBg);
            containerFront = itemView.findViewById(R.id.containerFront);
            containerBack = itemView.findViewById(R.id.containerBack);
            actionButtons = itemView.findViewById(R.id.actionButtons);
            game1 = itemView.findViewById(R.id.game1);
            game2 = itemView.findViewById(R.id.game2);
            game3 = itemView.findViewById(R.id.game3);
            gamesContainer = itemView.findViewById(R.id.gamesContainer);
            txtNoGames = itemView.findViewById(R.id.txtNoGames);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            txtStatusTitle = itemView.findViewById(R.id.txtFriendStatusTitle);
            txtStatusGame = itemView.findViewById(R.id.txtFriendStatusGame);
            frontGameContainer = itemView.findViewById(R.id.frontGameContainer);
            imgFavGameFront = itemView.findViewById(R.id.imgFavGameFront);
            imgFavGameBorder = itemView.findViewById(R.id.imgFavGameBorder);

            reactionsPanel = itemView.findViewById(R.id.reactionsPanel);
            rvReactions = itemView.findViewById(R.id.rvReactions);
            reactionsLayer = itemView.findViewById(R.id.reactionsLayer);

            if (rvReactions != null) {
                // Configuración Horizontal de 2 filas
                GridLayoutManager lm = new GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false);
                rvReactions.setLayoutManager(lm);
                rvReactions.setHasFixedSize(true);
            }
        }

        void bind(Friend friend) {
            resetCardState();
            txtName.setText(friend.name);
            txtCode.setText(friend.code);
            aplicarApariencia(friend);
            actualizarMiniJuegoFrontal(friend.game1);
            actualizarTraseraLogica(friend);

            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Switch Code", friend.code);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show();
            });

            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDelete(getAdapterPosition());
            });

            itemView.setOnClickListener(v -> flipCard(friend, false));
            obtenerDatosEnTiempoReal(friend);
        }

        private void resetCardState() {
            isFrontShowing = true;
            isFlipping = false;
            containerFront.setVisibility(View.VISIBLE);
            containerBack.setVisibility(View.GONE);
            if(actionButtons != null) actionButtons.setVisibility(View.VISIBLE);

            // Ocultamos el panel y limpiamos los stickers viejos
            if(reactionsPanel != null) reactionsPanel.setVisibility(View.GONE);
            if(reactionsLayer != null) reactionsLayer.removeAllViews();

            itemView.setRotationY(0f);
            itemView.setScaleX(1f);
            itemView.setScaleY(1f);
            imgBg.setImageAlpha(255);
        }

        private void aplicarApariencia(Friend friend) {
            String colorHex = friend.colorHex;
            String themeId = friend.themeId;
            if (colorHex == null || colorHex.isEmpty()) {
                if (themeColors != null && themeColors.containsKey(themeId)) {
                    colorHex = themeColors.get(themeId);
                } else { colorHex = "#ca3537"; }
            }
            try { this.currentColor = Color.parseColor(colorHex); } catch (Exception e) { this.currentColor = Color.parseColor("#ca3537"); }
            imgBg.setBackgroundColor(currentColor);
            String bgUrl = "";
            String baseUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/Widget/ID/";
            if (themeImages != null && themeImages.containsKey(themeId)) {
                bgUrl = baseUrl + themeImages.get(themeId);
                if (isValidContextForGlide(context)) {
                    Glide.with(context).load(bgUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(imgBg);
                }
            } else { imgBg.setImageDrawable(null); }
        }

        private void actualizarMiniJuegoFrontal(String gameId) {
            if (frontGameContainer != null) {
                if (gameId != null && !gameId.isEmpty()) {
                    frontGameContainer.setVisibility(View.VISIBLE);
                    String gameUrl = "https://api.nlib.cc/nx/" + gameId + "/icon";
                    if (isValidContextForGlide(context)) {
                        Glide.with(context).load(gameUrl).into(imgFavGameFront);
                    }
                } else {
                    frontGameContainer.setVisibility(View.INVISIBLE);
                    imgFavGameFront.setImageDrawable(null);
                }
            }
        }

        private void actualizarTraseraLogica(Friend friend) {
            boolean tieneEstado = friend.statusTitle != null && !friend.statusTitle.isEmpty();
            if (tieneEstado) {
                if (gamesContainer != null) gamesContainer.setVisibility(View.GONE);
                if (txtNoGames != null) txtNoGames.setVisibility(View.GONE);
                if (statusContainer != null) {
                    statusContainer.setVisibility(View.VISIBLE);
                    txtStatusTitle.setText(friend.statusTitle);
                    if (friend.statusGame != null && !friend.statusGame.isEmpty()) {
                        txtStatusGame.setVisibility(View.VISIBLE);
                        txtStatusGame.setText(friend.statusGame);
                    } else { txtStatusGame.setVisibility(View.GONE); }
                }
            } else {
                if (statusContainer != null) statusContainer.setVisibility(View.GONE);
                boolean hayJuegos = (friend.game1 != null && !friend.game1.isEmpty()) || (friend.game2 != null && !friend.game2.isEmpty()) || (friend.game3 != null && !friend.game3.isEmpty());
                if (hayJuegos) {
                    if(gamesContainer != null) gamesContainer.setVisibility(View.VISIBLE);
                    if(txtNoGames != null) txtNoGames.setVisibility(View.GONE);
                    loadGameIcon(game1, friend.game1);
                    loadGameIcon(game2, friend.game2);
                    loadGameIcon(game3, friend.game3);
                } else {
                    if(gamesContainer != null) gamesContainer.setVisibility(View.GONE);
                    if(txtNoGames != null) txtNoGames.setVisibility(View.VISIBLE);
                }
            }
        }

        private void loadGameIcon(ImageView img, String gameId) {
            if (img == null) return;
            if (gameId != null && !gameId.isEmpty()) {
                String url = "https://api.nlib.cc/nx/" + gameId + "/icon";
                if (isValidContextForGlide(context)) {
                    Glide.with(context).load(url).into(img);
                }
            } else { img.setImageDrawable(null); }
        }

        // ====================================================================
        // LÓGICA DE REACCIONES (STICKER BOMBING)
        // ====================================================================

        void verificarSiPuedeReaccionar(Friend friend) {
            if (reactionsPanel == null) return;

            String myUid = FirebaseAuth.getInstance().getUid();
            if (myUid == null || friend.uid == null || friend.uid.isEmpty()) return;

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(friend.uid)
                    .collection("received_reactions")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!isValidContextForGlide(context)) return;

                        // 1. REGLA DE CANTIDAD: MÁXIMO 30 STICKERS
                        // Si ya tiene 30 o más, no dejamos enviar más.
                        if (querySnapshot.size() >= 30) {
                            return;
                        }

                        // 2. REGLA DE TIEMPO: 1 POR SEMANA
                        boolean puedeReaccionar = true;
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            // Buscamos si YO ya envié uno
                            if (doc.getId().equals(myUid)) {
                                Long timestamp = doc.getLong("timestamp");
                                if (timestamp != null) {
                                    long diff = System.currentTimeMillis() - timestamp;
                                    // 7 días en milisegundos
                                    if (diff < 7L * 24 * 60 * 60 * 1000) {
                                        puedeReaccionar = false;
                                    }
                                }
                                break;
                            }
                        }

                        if (puedeReaccionar) {
                            mostrarPanelReacciones(friend);
                        }
                    });
        }

        void mostrarPanelReacciones(Friend friend) {
            reactionsPanel.setVisibility(View.VISIBLE);
            reactionsPanel.setAlpha(0f);
            reactionsPanel.setTranslationY(-50f);

            reactionsPanel.animate()
                    .alpha(1f)
                    .translationY(20f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator())
                    .start();

            ReactionAdapter ra = new ReactionAdapter(context, Config.reactions, reaction -> {
                enviarReaccion(friend, reaction);
            });
            rvReactions.setAdapter(ra);
        }

        void ocultarPanelReaccionesAnimado() {
            if (reactionsPanel == null || reactionsPanel.getVisibility() != View.VISIBLE) return;

            reactionsPanel.animate()
                    .translationY(-50f)
                    .setDuration(150) // Rápido para evitar ghosting
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        reactionsPanel.setVisibility(View.GONE);
                        reactionsPanel.setAlpha(0f);
                    })
                    .start();
        }

        void enviarReaccion(Friend friend, Reaction reaction) {
            if (reaction == null || reaction.id == null) return;

            String myUid = FirebaseAuth.getInstance().getUid();
            if (myUid == null) return;

            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("reactionId", reaction.id);
            logData.put("senderId", myUid);

            FirebaseFirestore.getInstance()
                    .collection("users").document(friend.uid)
                    .collection("received_reactions").document(myUid)
                    .set(logData);

            // PEGAR EL STICKER VISUALMENTE AL INSTANTE
            agregarStickerVisual(reaction.image);

            ocultarPanelReaccionesAnimado();
            Toast.makeText(context, context.getString(R.string.toast_sticker_sent_cooldown), Toast.LENGTH_LONG).show();
        }

        // --- CARGAR STICKERS GUARDADOS AL GIRAR ---
        void cargarReaccionesDesdeFirebase(Friend friend) {
            if (friend.uid == null || reactionsLayer == null) return;

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(friend.uid)
                    .collection("received_reactions")
                    .limit(30)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!isValidContextForGlide(context)) return;
                        reactionsLayer.removeAllViews(); // Limpiar para no duplicar

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String rId = doc.getString("reactionId");
                            if (rId != null) {
                                // Buscar la imagen correspondiente en Config
                                for (Reaction r : Config.reactions) {
                                    if (r.id.equals(rId)) {
                                        agregarStickerVisual(r.image);
                                        break;
                                    }
                                }
                            }
                        }
                    });
        }

        // --- DIBUJAR EL STICKER CON EFECTO POP ---
        void agregarStickerVisual(String imageFile) {
            if (reactionsLayer == null) return;

            ImageView sticker = new ImageView(context);
            int size = (int) (90 * context.getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

            int w = reactionsLayer.getWidth();
            int h = reactionsLayer.getHeight();
            if (w <= 0) w = 800; // Medidas de seguridad
            if (h <= 0) h = 500;

            Random random = new Random();
            params.leftMargin = random.nextInt(Math.max(1, w - size));
            params.topMargin = random.nextInt(Math.max(1, h - size));
            sticker.setLayoutParams(params);

            // Rotación aleatoria
            sticker.setRotation(random.nextInt(60) - 30);

            // CORRECCIÓN DE RUTA
            String baseUrl = Config.STICKER_JSON_URL.substring(0, Config.STICKER_JSON_URL.lastIndexOf("/") + 1) + "Widget/Reactions/";
            Glide.with(context).load(baseUrl + imageFile).into(sticker);

            reactionsLayer.addView(sticker);

            // ANIMACIÓN
            sticker.setScaleX(0f);
            sticker.setScaleY(0f);
            sticker.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(2.0f))
                    .withEndAction(() -> sticker.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                    .start();
        }

        // ====================================================================
        // ANIMACIÓN FLIP
        // ====================================================================

        void flipCard(Friend friend, boolean isAutoFlip) {
            if (isFlipping) return;
            isFlipping = true;
            if (isAutoFlip) friend.hasAutoFlipped = true;

            float distance = 8000 * itemView.getContext().getResources().getDisplayMetrics().density;
            itemView.setCameraDistance(distance);
            final float startScale = 1.0f;
            final float endScale = 0.9f;

            if (!isFrontShowing) {
                ocultarPanelReaccionesAnimado();
            }

            ObjectAnimator flip1 = ObjectAnimator.ofFloat(itemView, "rotationY", 0f, 90f);
            flip1.setDuration(250);
            flip1.setInterpolator(new AccelerateDecelerateInterpolator());
            itemView.animate().scaleX(endScale).scaleY(endScale).setDuration(250).start();

            flip1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isFrontShowing) {
                        // ATRÁS
                        containerFront.setVisibility(View.GONE);
                        containerBack.setVisibility(View.VISIBLE);
                        if(actionButtons != null) actionButtons.setVisibility(View.GONE);
                        imgBg.setImageDrawable(null);
                        imgBg.setBackgroundColor(currentColor);

                        if (!isAutoFlip) obtenerDatosEnTiempoReal(friend);

                        // --- AQUÍ CARGAMOS LOS STICKERS ---
                        cargarReaccionesDesdeFirebase(friend);

                        verificarSiPuedeReaccionar(friend);

                    } else {
                        // FRENTE
                        containerBack.setVisibility(View.GONE);
                        containerFront.setVisibility(View.VISIBLE);
                        if(actionButtons != null) actionButtons.setVisibility(View.VISIBLE);
                        aplicarApariencia(friend);
                        ocultarPanelReaccionesAnimado();

                        // Limpiamos la memoria visual al volver al frente
                        if(reactionsLayer != null) reactionsLayer.removeAllViews();
                    }
                    itemView.setRotationY(-90f);
                    ObjectAnimator flip2 = ObjectAnimator.ofFloat(itemView, "rotationY", -90f, 0f);
                    flip2.setDuration(250);
                    flip2.setInterpolator(new AccelerateDecelerateInterpolator());
                    itemView.animate().scaleX(startScale).scaleY(startScale).setDuration(250).start();
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

        void obtenerDatosEnTiempoReal(Friend friend) {
            if (friend.uid == null || friend.uid.isEmpty()) return;
            if (!isValidContextForGlide(context)) return;

            FirebaseFirestore.getInstance().collection("users").document(friend.uid).get()
                    .addOnSuccessListener(doc -> {
                        if (!isValidContextForGlide(context)) return;

                        if (doc.exists()) {
                            boolean huboCambios = false;
                            // --- CÓDIGO ANTERIOR QUE FALLABA ---
                            /*
                            List<String> games = (List<String>) doc.get("favorite_games");
                            String g1 = "", g2 = "", g3 = "";
                            if (games != null) {
                                if (games.size() > 0) g1 = games.get(0);
                                if (games.size() > 1) g2 = games.get(1);
                                if (games.size() > 2) g3 = games.get(2);
                            }
                            */
                            // --- CÓDIGO CORREGIDO Y SEGURO ---
                            // Leemos como lista genérica (?) para aceptar cualquier cosa que venga
                            List<?> rawGames = (List<?>) doc.get("favorite_games");
                            String g1 = "", g2 = "", g3 = "";

                            if (rawGames != null) {
                                // Verificamos "instanceof String" para asegurarnos que sea texto antes de convertirlo
                                if (rawGames.size() > 0 && rawGames.get(0) instanceof String) {
                                    g1 = (String) rawGames.get(0);
                                }
                                if (rawGames.size() > 1 && rawGames.get(1) instanceof String) {
                                    g2 = (String) rawGames.get(1);
                                }
                                if (rawGames.size() > 2 && rawGames.get(2) instanceof String) {
                                    g3 = (String) rawGames.get(2);
                                }
                            }

                            // Ahora seguimos con la comparación segura...
                            if (!sonIguales(friend.game1, g1) || !sonIguales(friend.game2, g2) || !sonIguales(friend.game3, g3)) {
                                friend.game1 = g1;
                                friend.game2 = g2;
                                friend.game3 = g3;
                                huboCambios = true;
                            }

                            if (!sonIguales(friend.game1, g1) || !sonIguales(friend.game2, g2) || !sonIguales(friend.game3, g3)) {
                                friend.game1 = g1; friend.game2 = g2; friend.game3 = g3; huboCambios = true;
                            }
                            String status = doc.getString("witty_status");
                            String sGame = doc.getString("witty_game");
                            if (!sonIguales(friend.statusTitle, status) || !sonIguales(friend.statusGame, sGame)) {
                                friend.statusTitle = status; friend.statusGame = sGame; huboCambios = true;
                            }
                            String newName = doc.getString("user_name");
                            String newCode = doc.getString("user_code");
                            if (newName != null && !newName.equals(friend.name)) { friend.name = newName; txtName.setText(newName); huboCambios = true; }
                            if (newCode != null && !newCode.equals(friend.code)) { friend.code = newCode; txtCode.setText(newCode); huboCambios = true; }
                            String newColor = doc.getString("saved_theme_color");
                            String newThemeId = doc.getString("saved_theme_id");
                            if (newColor != null && !newColor.equals(friend.colorHex)) { friend.colorHex = newColor; huboCambios = true; }
                            if (newThemeId != null && !newThemeId.equals(friend.themeId)) { friend.themeId = newThemeId; huboCambios = true; }

                            if (huboCambios) {
                                actualizarMiniJuegoFrontal(friend.game1);
                                actualizarTraseraLogica(friend);
                                if (!isFrontShowing) { try { currentColor = Color.parseColor(friend.colorHex); imgBg.setBackgroundColor(currentColor); } catch (Exception e) { } }
                                if (actionListener != null) actionListener.onDataUpdated();
                            }

                            boolean hasStatus = friend.statusTitle != null && !friend.statusTitle.isEmpty();

                            if (hasStatus) {
                                String statusSignature = friend.statusTitle + "_" + (friend.statusGame != null ? friend.statusGame : "");
                                SharedPreferences prefs = context.getSharedPreferences("StatusSeenPrefs", Context.MODE_PRIVATE);
                                String lastSeenSignature = prefs.getString("seen_" + friend.uid, "");
                                boolean isNewStatus = !statusSignature.equals(lastSeenSignature);

                                if (isNewStatus) {
                                    prefs.edit().putString("seen_" + friend.uid, statusSignature).apply();
                                    if (actionListener != null) actionListener.onNewNotification();
                                    if (actionListener != null) actionListener.onAutoScrollRequest(getAdapterPosition());
                                }

                                if (isFrontShowing && !friend.hasAutoFlipped) {
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (isFrontShowing && !friend.hasAutoFlipped && isValidContextForGlide(context)) {
                                            if (isNewStatus && getAdapterPosition() == currentSelectedPosition) {
                                                reproducirSonidoNoti();
                                            }
                                            flipCard(friend, true);
                                        }
                                    }, 800);
                                }
                            }
                        }
                    });
        }

        private boolean sonIguales(String a, String b) {
            if (a == null) a = "";
            if (b == null) b = "";
            return a.equals(b);
        }
    }
}