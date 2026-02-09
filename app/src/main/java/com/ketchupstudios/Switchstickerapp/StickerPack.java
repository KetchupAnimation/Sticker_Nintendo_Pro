package com.ketchupstudios.Switchstickerapp;

import java.util.ArrayList;
import java.util.List;

public class StickerPack {
    // --- DATOS DEL PACK (LA CAJA COMPLETA) ---
    public String identifier;
    public String name;
    public String publisher;
    public String trayImageFile;
    public String status;      // "new", "updated", etc.
    public boolean isPremium;  // true = candado, false = gratis
    public String updateNoteImage;
    public String artistLink;

    // --- NUEVAS VARIABLES PARA EL EVENTO DIARIO (MINIJUEGO) ---
    public boolean isEvent = false;       // Identifica si es un calendario/evento
    public String eventStartDate = "";    // Fecha inicio YYYY-MM-DD
    public String eventBanner = "";       // Imagen del banner (dentro de la carpeta del pack)
    public int totalDays = 0;             // Cuántos días dura el evento
    // ---------------------------------------------------------

    public List<Sticker> stickers;

    // --- ESTA ES LA ÚNICA LISTA DE TAGS QUE NECESITAS ---
    public List<String> tags = new ArrayList<>();

    public StickerPack() {
        stickers = new ArrayList<>();
    }

    // Getters para facilitar la lectura
    public String getName() { return name; }
    public String getIdentifier() { return identifier; }
    public String getTrayImageFile() { return trayImageFile; }

    // --- DATOS DE CADA STICKER (LAS ESTAMPAS INDIVIDUALES) ---
    public static class Sticker {
        public String imageFile;
        public List<String> emojis; // WhatsApp pide emojis

        public Sticker() {
            emojis = new ArrayList<>();
        }
    }
}