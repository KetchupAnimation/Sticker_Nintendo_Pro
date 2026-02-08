package com.ketchupstudios.Switchstickerapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class HoloCardView extends View {

    private Paint shinePaint;
    private float holoX, holoY;
    // Colores ajustados para el efecto holográfico (Rainbow sutil)
    private int[] shineColors = {
            0x20FFFFFF, // Blanco transparente
            0x40FF00FF, // Magenta suave
            0x4000FFFF, // Azul suave
            0x20FFFFFF  // Blanco transparente
    };

    public HoloCardView(Context context) {
        super(context);
        init();
    }

    public HoloCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        shinePaint = new Paint();
        // SRC_ATOP dibuja solo donde ya hay pixeles (útil si el padre tiene forma),
        // pero como esto es un overlay rectangular, SRC_OVER está bien.
        shinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        shinePaint.setAntiAlias(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Conectarse al cerebro único
        HoloController.getInstance(getContext()).register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Desconectarse para no gastar recursos
        HoloController.getInstance(getContext()).unregister(this);
    }

    // Método llamado por el Controlador
    public void updateHolo(float x, float y) {
        this.holoX = x;
        this.holoY = y;
        invalidate(); // Redibujar solo este frame
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Evitar división por cero o tamaños inválidos
        if (width == 0 || height == 0) return;

        // Cálculos de movimiento basados en los datos del controlador
        float translateX = (holoX * width) / 10;
        float translateY = (holoY * height) / 10;

        // Gradiente dinámico
        Shader shader = new LinearGradient(
                0 - translateX, 0 + translateY,
                width - translateX, height + translateY,
                shineColors,
                null,
                Shader.TileMode.CLAMP);

        shinePaint.setShader(shader);
        canvas.drawRect(0, 0, width, height, shinePaint);
    }
}