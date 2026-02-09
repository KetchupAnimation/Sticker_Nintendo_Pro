package com.ketchupstudios.Switchstickerapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color; // Importante para colores del canal
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BatteryService extends Service {

    private BroadcastReceiver powerReceiver;
    // ID único para el canal
    private static final String CHANNEL_ID = "BatteryWidgetChannel_v2"; // Cambié el ID levemente para forzar actualización
    private static final int NOTIFICATION_ID = 123;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Iniciar en Primer Plano (Lógica para Android 14+)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    NOTIFICATION_ID,
                    getNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(NOTIFICATION_ID, getNotification());
        }

        // 2. Registrar el receptor de batería
        registrarReceptor();
    }

    private void registrarReceptor() {
        IntentFilter filter = new IntentFilter();
        // Escuchar cambios de porcentaje
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);

        // Escuchar conexión/desconexión
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);

        powerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Actualizar widgets
                BatteryWidgetProvider.updateAllWidgets(context);
            }
        };

        registerReceiver(powerReceiver, filter);
        Log.d("BatteryService", "🔋 Servicio Real-Time Iniciado");
    }

    // --- AQUÍ ESTÁ LA MAGIA PARA OCULTAR EL ICONO Y EL GLOBO ---
    private Notification getNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_LOW: No hace sonido, no vibra, no salta en pantalla.
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Mantiene el widget de batería actualizado");
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET); // Oculto en bloqueo

            // ESTA LÍNEA ELIMINA EL "1" ROJO DEL ICONO DE LA APP
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Widget Activo")
                .setContentText("Actualizando batería...")
                .setSmallIcon(R.drawable.nintendo_switch_icon) // <--- TU ICONO DE SWITCH
                .setPriority(NotificationCompat.PRIORITY_LOW) // Prioridad baja para no molestar
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true) // Fija la notificación para que el sistema no mate el servicio
                .setShowWhen(false) // Oculta la hora para que se vea más limpio
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY: Si el sistema mata el servicio, intenta recrearlo
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (powerReceiver != null) {
            try {
                unregisterReceiver(powerReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Intentar revivir si nos destruyen
        Intent broadcastIntent = new Intent("com.ketchupstudios.Switchstickerapp.RESTART_SERVICE");
        sendBroadcast(broadcastIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}