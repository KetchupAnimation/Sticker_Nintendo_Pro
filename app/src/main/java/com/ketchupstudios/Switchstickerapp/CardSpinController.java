package com.ketchupstudios.Switchstickerapp;

import android.view.MotionEvent;
import android.view.View;

public class CardSpinController implements View.OnTouchListener {

    private float startX;
    private float startRotationY;
    private final View cardView;
    private final OnSpinChangeListener listener;

    // Sensibilidad del giro (1.0 = gira a la misma velocidad del dedo. Un poco menos lo hace sentir "pesado" y premium)
    private static final float SPIN_SENSITIVITY = 0.6f;

    // Interfaz para avisarle a MainActivity cuándo la carta se dio la vuelta
    public interface OnSpinChangeListener {
        void onRotationChanged(float currentRotationY);
    }

    public CardSpinController(View cardView, OnSpinChangeListener listener) {
        this.cardView = cardView;
        this.listener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // El usuario tocó la carta: guardamos en qué punto de la pantalla empezó
                startX = event.getRawX();
                startRotationY = cardView.getRotationY();
                return true; // Importante devolver true para poder seguir leyendo el movimiento

            case MotionEvent.ACTION_MOVE:
                // El usuario está deslizando el dedo: calculamos cuántos píxeles se movió
                float deltaX = event.getRawX() - startX;

                // Calculamos la nueva rotación
                float newRotation = startRotationY + (deltaX * SPIN_SENSITIVITY);
                cardView.setRotationY(newRotation);

                // Le avisamos a la actividad principal para que oculte/muestre el dorso
                if (listener != null) {
                    listener.onRotationChanged(newRotation);
                }
                return true;
        }
        return false;
    }
}