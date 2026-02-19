package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CustomToast {

    // Este método imita exactamente a CustomToast.makeText
    public static Toast makeText(Context context, String message, int duration) {
        // 1. Crear el Toast vacío
        Toast toast = new Toast(context);
        toast.setDuration(duration);

        // 2. Inflar nuestro diseño personalizado
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.layout_custom_toast, null);

        // 3. Poner el texto
        TextView text = layout.findViewById(R.id.txtToastMessage);
        text.setText(message);

        // 4. Asignar la vista al Toast
        toast.setView(layout);

        return toast;
    }
}