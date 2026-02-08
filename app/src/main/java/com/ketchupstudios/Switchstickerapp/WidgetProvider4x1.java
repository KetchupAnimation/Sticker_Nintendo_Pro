package com.ketchupstudios.Switchstickerapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class WidgetProvider4x1 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        iniciarWorker(context);
    }

    private void iniciarWorker(Context context) {
        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "WIDGET_UPDATE_WORK_GLOBAL",
                ExistingWorkPolicy.REPLACE,
                updateRequest
        );
    }

    @Override
    public void onEnabled(Context context) {
        iniciarWorker(context);
    }
}