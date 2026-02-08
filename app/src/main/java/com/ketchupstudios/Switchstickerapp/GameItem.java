package com.ketchupstudios.Switchstickerapp;

public class GameItem {
    public String id;
    public String name;
    public String bannerUrl; // <--- NUEVO: Para guardar el banner del JSON

    public GameItem(String id, String name, String bannerUrl) {
        this.id = id;
        this.name = name;
        this.bannerUrl = bannerUrl;
    }
}