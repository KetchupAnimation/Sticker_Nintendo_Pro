package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.ArrayList;
import java.util.List;

public class HoloController implements SensorEventListener {

    private static HoloController instance;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float currentX, currentY;

    // Lista de vistas que quieren recibir el efecto
    private List<HoloCardView> listeners = new ArrayList<>();

    public static HoloController getInstance(Context context) {
        if (instance == null) {
            instance = new HoloController(context.getApplicationContext());
        }
        return instance;
    }

    private HoloController(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    // --- LOGICA DE REGISTRO ---
    public void register(HoloCardView view) {
        if (!listeners.contains(view)) {
            listeners.add(view);
            // Si es la primera vista que se une, encendemos el sensor
            if (listeners.size() == 1) {
                startSensor();
            }
        }
        // Actualizamos la vista recién llegada con el último valor conocido
        view.updateHolo(currentX, currentY);
    }

    public void unregister(HoloCardView view) {
        listeners.remove(view);
        // Si ya no hay vistas (ej: cerramos la app o cambiamos de pantalla), apagamos el sensor
        if (listeners.isEmpty()) {
            stopSensor();
        }
    }

    private void startSensor() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI); // DELAY_UI es suficiente y gasta menos batería que GAME
        }
    }

    private void stopSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Suavizado (Lerp) calculado UNA SOLA VEZ para todos
            float targetX = -event.values[0];
            float targetY = event.values[1];

            currentX += (targetX - currentX) * 0.1f;
            currentY += (targetY - currentY) * 0.1f;

            // Notificamos a todas las vistas activas
            for (HoloCardView view : listeners) {
                view.updateHolo(currentX, currentY);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}