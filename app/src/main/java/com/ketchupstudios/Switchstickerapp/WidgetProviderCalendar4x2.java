package com.ketchupstudios.Switchstickerapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingWorkPolicy;

public class WidgetProviderCalendar4x2 extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Llamamos al Worker del calendario
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CalendarUpdateWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork("CALENDAR_WORK_4x2", ExistingWorkPolicy.REPLACE, request);
    }
}