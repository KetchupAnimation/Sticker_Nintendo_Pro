package com.ketchupstudios.Switchstickerapp;

import java.util.ArrayList;
import java.util.List;

public class Config {
    // 1. URL DEL JSON
    public static final String STICKER_JSON_URL = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/refs/heads/main/contents.json";

    // 2. IDS DE PUBLICIDAD
    public static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-9087203932210009/2214350595";
    public static final String ADMOB_BANNER_ID = "ca-app-pub-9087203932210009/5970596927";

    public static StickerPack selectedPack;
    public static News newsData = null;

    public static final String BASE_URL_ASSETS = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/";
    public static final String GIFT_CLOSED = "regalo1.png";
    public static final String GIFT_OPEN = "regalo2.png";

    // --- CLASES DE DATOS ---

    public static class ApiResponse {
        public List<StickerPack> stickers;
        public List<Wallpaper> wallpapers;
        public List<Banner> banners;
        public Promo promo;
    }

    public static class Promo {
        public boolean enabled;
        public String image;
        public String title;
        public String subtitle;
        public String link;
        public int current;
        public int goal;
        public int start;
    }

    public static class News {
        public boolean enabled;
        public String versionId;
        public int minVersionCode;
        public String text;
        public String iconImage;
        public String bgImage;
        public String closeImage;
        public String overlayImage;
    }

    public static class Banner {
        public String imageFile;
        public String linkUrl;
        public Banner(String imageFile, String linkUrl) {
            this.imageFile = imageFile;
            this.linkUrl = linkUrl;
        }
    }

    public static class Wallpaper {
        public String identifier;
        public String name;
        public String imageFile;
        public String publisher;
        public boolean isNew;
        public boolean isPremium;
        public String artistLink;
        public String colorBg;
        public boolean isHidden = false;// <-- Añade esta línea
        public String rewardDay = "";

        // NUEVO CAMPO PARA EVENTOS LIMITADOS
        public boolean isLimitedTime = false;
        public boolean isExpired = false;


        public java.util.List<String> tags = new java.util.ArrayList<>();

        public Wallpaper(String identifier, String name, String imageFile, String publisher, boolean isNew, boolean isPremium, String artistLink) {
            this.identifier = identifier;
            this.name = name;
            this.imageFile = imageFile;
            this.publisher = publisher;
            this.isNew = isNew;
            this.isPremium = isPremium;
            this.artistLink = artistLink;
        }
    }

    public static class BatteryTheme {
        public String id;
        public String name;
        public String folder;
        public String colorBg;
        public String textColor;
        public boolean isNew;
        public String artistLink;

        // NUEVO CAMPO PARA EVENTOS LIMITADOS
        public boolean isLimitedTime = false;
        public boolean isExpired = false;

        public BatteryTheme(String id, String name, String folder, String colorBg, String textColor, boolean isNew, String artistLink) {
            this.id = id;
            this.name = name;
            this.folder = folder;
            this.colorBg = colorBg;
            this.textColor = textColor;
            this.isNew = isNew;
            this.artistLink = artistLink;
        }
    }

    // --- NUEVO: CLASE PARA PARSEAR EL JSON DE EVENTOS ---
    public static class LimitedEvent {
        public String date;
        public String type; // "wallpaper", "battery", "widget"
        public org.json.JSONObject data; // Datos crudos para convertir luego

        public LimitedEvent(String date, String type, org.json.JSONObject data) {
            this.date = date;
            this.type = type;
            this.data = data;
        }
    }

    // LISTAS GLOBALES
    public static List<Banner> banners = new ArrayList<>();
    public static List<Wallpaper> wallpapers = new ArrayList<>();
    public static ArrayList<StickerPack> packs = new ArrayList<>();
    public static List<BatteryTheme> batteryThemes = new ArrayList<>();

    // NUEVA LISTA TEMPORAL
    public static List<LimitedEvent> limitedEvents = new ArrayList<>();
    // --- AQUÍ LA PONEMOS ---
    public static java.util.List<Reaction> reactions = new java.util.ArrayList<>();
}