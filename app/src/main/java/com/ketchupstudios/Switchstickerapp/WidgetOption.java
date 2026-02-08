package com.ketchupstudios.Switchstickerapp;

public class WidgetOption {
    String title;
    int previewImageResId;
    Class<?> widgetClass;

    public WidgetOption(String title, int previewImageResId, Class<?> widgetClass) {
        this.title = title;
        this.previewImageResId = previewImageResId;
        this.widgetClass = widgetClass;
    }
}