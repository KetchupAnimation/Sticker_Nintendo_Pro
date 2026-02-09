package com.ketchupstudios.Switchstickerapp;

public class PremiumItem {
    public static final int TYPE_STICKER = 1;
    public static final int TYPE_WALLPAPER = 2;
    public static final int TYPE_EMPTY = 3; // <--- TIPO PARA HUECOS INVISIBLES

    public int type;
    public StickerPack stickerPack;
    public Config.Wallpaper wallpaper;

    // Constructor Sticker
    public PremiumItem(StickerPack pack) {
        this.type = TYPE_STICKER;
        this.stickerPack = pack;
    }

    // Constructor Wallpaper
    public PremiumItem(Config.Wallpaper wall) {
        this.type = TYPE_WALLPAPER;
        this.wallpaper = wall;
    }

    // Constructor Vacío
    public PremiumItem(int type) {
        this.type = type;
    }
}