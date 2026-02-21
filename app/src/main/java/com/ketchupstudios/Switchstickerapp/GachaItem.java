package com.ketchupstudios.Switchstickerapp;

import java.util.ArrayList;
import java.util.List;

public class GachaItem {
    public String id;
    public String type; // Guardará "wallpaper" o "widget"
    public String title;
    public String image;
    public String rarity;
    public String colorBg;
    public String publisher;
    public String artistLink;
    public List<String> tags = new ArrayList<>();

    // Constructor vacío necesario para algunas librerías
    public GachaItem() {}
}