package com.ketchupstudios.Switchstickerapp;

import java.util.ArrayList;
import java.util.List;

public class GachaItem {
    public String id;
    public String type; // Guardará "wallpaper", "widget", "sticker_pack" o "extra_sticker"
    public String title;
    public String image;
    public String rarity;
    public String colorBg;
    public String publisher;
    public String artistLink;
    public String pack_identifier;
    public List<String> tags = new ArrayList<>();

    // 👇 NUEVO: Emojis obligatorios para WhatsApp 👇
    public List<String> emojis = new ArrayList<>();

    // Constructor vacío necesario para algunas librerías
    public GachaItem() {}
}