package com.ketchupstudios.Switchstickerapp;

public class Friend {
    public String name;
    public String code;
    public String themeId;
    public String colorHex;
    public String uid;
    public String game1;
    public String game2 = "";
    public String game3 = "";

    // --- NUEVOS CAMPOS PARA WITTY STATUS ---
    public String statusTitle = "";
    public String statusGame = "";

    // Variable de control para que gire solo 1 vez automáticamente
    public boolean hasAutoFlipped = false;

    // Constructor vacío para Gson
    public Friend() {}

    public Friend(String name, String code, String themeId, String colorHex, String uid) {
        this.name = name;
        this.code = code;
        this.themeId = themeId;
        this.colorHex = colorHex;
        this.uid = uid;
        this.game1 = "";
        this.game2 = "";
        this.game3 = "";
    }
}