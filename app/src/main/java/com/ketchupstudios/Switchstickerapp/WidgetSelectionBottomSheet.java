package com.ketchupstudios.Switchstickerapp;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class WidgetSelectionBottomSheet extends BottomSheetDialogFragment {

    private static final String AD_UNIT_ID = "ca-app-pub-9087203932210009/2214350595";

    private InterstitialAd mInterstitialAd;
    private OnWidgetSelectedListener listener;
    private Class<?> pendingWidgetClass;

    private boolean isAdLoading = false;

    // NUEVA VARIABLE: ¿El usuario ya pulsó y está esperando?
    private boolean isUserWaiting = false;

    public interface OnWidgetSelectedListener {
        void onWidgetSelected(Class<?> widgetClass);
    }

    public void setListener(OnWidgetSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        cargarAnuncio();
    }

    private void cargarAnuncio() {
        isAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(requireContext(), AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        isAdLoading = false;
                        Log.i("AdMob", "Anuncio cargado.");

                        configurarCallbacksAnuncio();

                        // --- AUTOMATIZACIÓN ---
                        // Si el usuario ya había pulsado y estaba esperando... ¡MOSTRAR AHORA!
                        if (isUserWaiting) {
                            mInterstitialAd.show(requireActivity());
                            isUserWaiting = false; // Ya no espera, ya se mostró
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.i("AdMob", "Fallo carga: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                        isAdLoading = false;

                        // --- AUTOMATIZACIÓN (FALLO) ---
                        // Si el usuario estaba esperando y falló... ¡AGREGAR DIRECTO!
                        if (isUserWaiting) {
                            ejecutarAccionPendiente();
                            isUserWaiting = false;
                        }
                    }
                });
    }

    private void configurarCallbacksAnuncio() {
        if (mInterstitialAd == null) return;

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
            @Override
            public void onAdDismissedFullScreenContent() {
                // El usuario cerró el anuncio -> Éxito
                ejecutarAccionPendiente();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                mInterstitialAd = null; // Consumimos el anuncio
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Falló al abrirse -> No castigamos al usuario, procedemos
                ejecutarAccionPendiente();
            }
        });
    }

    private void intentarAgregarWidget(Class<?> widgetClass) {
        this.pendingWidgetClass = widgetClass;

        if (mInterstitialAd != null) {
            // CASO 1: Anuncio listo -> Mostrar inmediato
            mInterstitialAd.show(requireActivity());

        } else if (isAdLoading) {
            // CASO 2: Cargando -> Avisar y activar MODO ESPERA AUTOMÁTICA
            isUserWaiting = true;
            Toast.makeText(getContext(), "Cargando anuncio...", Toast.LENGTH_SHORT).show();
            // No hacemos nada más aquí, el "onAdLoaded" se encargará de lanzarlo solo.

        } else {
            // CASO 3: No hay anuncio y no está cargando (Error previo) -> Pasar directo
            ejecutarAccionPendiente();
        }
    }

    private void ejecutarAccionPendiente() {
        if (listener != null && pendingWidgetClass != null) {
            listener.onWidgetSelected(pendingWidgetClass);
            dismiss();
        }
    }

    // --- CÓDIGO VISUAL (SIN CAMBIOS) ---
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                View decorView = dialog.getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_widgets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerWidgets);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // TUS OPCIONES
        List<WidgetOption> options = new ArrayList<>();
        options.add(new WidgetOption("Clima Grande (4x2)", R.drawable.preview_4x2, WidgetProvider4x2.class));
        options.add(new WidgetOption("Clima Cuadrado (2x2)", R.drawable.preview_2x2, WidgetProvider2x2.class));
        options.add(new WidgetOption("Clima Barra (4x1)", R.drawable.preview_4x1, WidgetProvider4x1.class));
        options.add(new WidgetOption("Calendario Grande (4x2)", R.drawable.preview_calendar_4x2, WidgetProviderCalendar4x2.class));
        options.add(new WidgetOption("Calendario Cuadrado (2x2)", R.drawable.preview_calendar_2x2, WidgetProviderCalendar2x2.class));
        options.add(new WidgetOption("Híbrido (4x2)", R.drawable.preview_noticiasreloj_4x2, HybridWidget.class));
        options.add(new WidgetOption("Solo Noticias (4x2)", R.drawable.preview_noticias_4x2, NewsWidget.class));
        options.add(new WidgetOption("Solo Reloj (4x2)", R.drawable.preview_reloj_4x2, ClockWidget.class));

        WidgetAdapter adapter = new WidgetAdapter(options, item -> {
            intentarAgregarWidget(item.widgetClass);
        });

        recyclerView.setAdapter(adapter);
    }

    private static class WidgetAdapter extends RecyclerView.Adapter<WidgetAdapter.ViewHolder> {
        private final List<WidgetOption> items;
        private final OnItemClickListener listener;
        interface OnItemClickListener { void onClick(WidgetOption item); }
        WidgetAdapter(List<WidgetOption> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_widget_option, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WidgetOption item = items.get(position);
            holder.title.setText(item.title);
            holder.image.setImageResource(item.previewImageResId);
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }
        @Override
        public int getItemCount() { return items.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            ImageView image;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.txtTitle);
                image = v.findViewById(R.id.imgPreview);
            }
        }
    }
}