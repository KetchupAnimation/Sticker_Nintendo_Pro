package com.ketchupstudios.Switchstickerapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class WidgetProvider4x2 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Al actualizar (o agregar), lanzamos el trabajador INMEDIATAMENTE.
        // Quitamos el freno de "lastUpdate" aquí para asegurar que la primera carga no falle.
        iniciarWorker(context);
    }

    private void iniciarWorker(Context context) {
        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();

        // Usamos REPLACE para que si hay una petición vieja atascada, la nueva (con la lógica rápida) tome el control.
        WorkManager.getInstance(context).enqueueUniqueWork(
                "WIDGET_UPDATE_WORK_GLOBAL",
                ExistingWorkPolicy.REPLACE,
                updateRequest
        );
    }

    @Override
    public void onEnabled(Context context) {
        // Esto se ejecuta cuando se agrega el primer widget de este tipo.
        iniciarWorker(context);
    }
}