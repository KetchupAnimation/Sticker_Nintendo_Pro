package com.ketchupstudios.Switchstickerapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class StickerContentProvider extends ContentProvider {
    // La dirección exacta (escrita a mano para evitar errores)
    public static final String AUTHORITY_URI = "com.ketchupstudios.Switchstickerapp.stickercontentprovider";
    private static final String TAG = "StickerProvider";

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // Regla 1: Piden solo "metadata"
        MATCHER.addURI(AUTHORITY_URI, "metadata", 1);

        // --- 🚨 LA REGLA NUEVA QUE FALTABA 🚨 ---
        // Regla 1b: Piden "metadata/NOMBRE_DEL_PACK" (Esto es lo que WhatsApp hacía)
        MATCHER.addURI(AUTHORITY_URI, "metadata/*", 1);
        // ----------------------------------------

        // Regla 2: Piden stickers
        MATCHER.addURI(AUTHORITY_URI, "stickers/*", 2);
        MATCHER.addURI(AUTHORITY_URI, "stickers/*/*", 3);
    }

    @Override
    public boolean onCreate() {
        Log.e(TAG, "¡ESTOY VIVO! El Provider se ha creado correctamente.");
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int code = MATCHER.match(uri);
        Log.d(TAG, "WhatsApp pide datos (Query): " + uri.toString() + " Code: " + code);

        // Si es código 1 (metadata o metadata/nombre), devolvemos los datos del pack
        if (code == 1) {
            return getPackMetadata(uri);
        } else if (code == 2 || code == 3) {
            return getStickersForPack(uri);
        }

        Log.e(TAG, "❌ ERROR: Dirección desconocida (Code -1): " + uri.toString());
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "vnd.android.cursor.dir/vnd.com.whatsapp.provider.sticker";
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        Log.d(TAG, "Abriendo archivo: " + uri.toString());
        List<String> pathSegments = uri.getPathSegments();

        // Lógica flexible para encontrar el archivo
        String fileName = pathSegments.get(pathSegments.size() - 1);
        String packName = Config.selectedPack != null ? Config.selectedPack.identifier : "";

        // Intentamos deducir el pack de la URL si es posible
        if (pathSegments.size() >= 3 && !pathSegments.get(pathSegments.size()-2).equals("stickers")) {
            packName = pathSegments.get(pathSegments.size() - 2);
        }

        File file = new File(getContext().getFilesDir(), "stickers/" + packName + "/" + fileName);
        if (!file.exists()) {
            // Intento desesperado: buscar en raíz de stickers
            File fallback = new File(getContext().getFilesDir(), "stickers/" + fileName);
            if(fallback.exists()) return ParcelFileDescriptor.open(fallback, ParcelFileDescriptor.MODE_READ_ONLY);

            Log.e(TAG, "❌ Archivo no encontrado: " + file.getAbsolutePath());
            throw new FileNotFoundException("No existe: " + fileName);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private Cursor getPackMetadata(Uri uri) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                "sticker_pack_identifier", "sticker_pack_name", "sticker_pack_publisher",
                "sticker_pack_icon", "android_play_store_link", "ios_app_download_link", "publisher_email",
                "publisher_website", "privacy_policy_website", "license_agreement_website",
                "image_data_version", "avoid_cache"
        });
        if (Config.selectedPack != null) {
            cursor.addRow(new Object[]{
                    Config.selectedPack.identifier,
                    Config.selectedPack.name,
                    Config.selectedPack.publisher,
                    Config.selectedPack.trayImageFile,
                    "", "", "", "", "", "", "1", 0
            });
        }
        return cursor;
    }

    private Cursor getStickersForPack(Uri uri) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"sticker_file_name", "sticker_emoji"});
        if (Config.selectedPack != null) {
            for (StickerPack.Sticker s : Config.selectedPack.stickers) {
                cursor.addRow(new Object[]{s.imageFile, "😀"});
            }
        }
        return cursor;
    }

    // Métodos vacíos obligatorios
    @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { return null; }
    @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
}