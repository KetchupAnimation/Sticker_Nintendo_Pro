# ============================================================
# 1. REGLAS GENERALES DE ANDROID Y OPTIMIZACIÓN
# ============================================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Mantener clases referenciadas desde XML (Layouts y Menús)
-keep public class * extends android.view.View
-keep public class * extends android.view.ViewGroup
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity

# ============================================================
# 2. LIBRERÍAS DE TERCEROS (CRÍTICO)
# ============================================================

# --- GSON (Vital para leer JSONs) ---
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }

# --- GLIDE (Carga de Imágenes) ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# --- FIREBASE & GOOGLE SERVICES (Vital para Login) ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class androidx.work.** { *; }
-keepnames class com.google.android.gms.common.internal.ReflectedParcelable { public *; }

# --- ADMOB (Publicidad) ---
-keep class com.google.ads.** { *; }
-keep public class com.google.android.gms.ads.** { public *; }

# --- UNITY ADS (Evita errores de compilación) ---
-dontwarn com.unity3d.ads.**
-dontwarn com.unity3d.services.**
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-keep class com.google.ads.mediation.unity.** { *; }

# --- ESCÁNER QR (ZXing & JourneyApps) ---
# Evita crash al abrir la cámara o escanear
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.barcodescanner.**
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.journeyapps.barcodescanner.CaptureActivity { *; }

# ============================================================
# 3. TUS MODELOS DE DATOS (EVITA CRASH EN LOGIN Y LISTAS)
# ============================================================
# Protegemos estas clases para que Firebase y Gson puedan leer/escribir
# los nombres de las variables sin que ProGuard los cambie a "a", "b", etc.

# Configuración Global y Eventos
-keep class com.ketchupstudios.Switchstickerapp.Config { *; }
-keep class com.ketchupstudios.Switchstickerapp.Config$* { *; }

# Usuario y Amigos (CRÍTICO PARA LOGIN GOOGLE)
-keep class com.ketchupstudios.Switchstickerapp.Friend { *; }
-keep class com.ketchupstudios.Switchstickerapp.Friend$* { *; }

# Stickers y Wallpapers
-keep class com.ketchupstudios.Switchstickerapp.StickerPack { *; }
-keep class com.ketchupstudios.Switchstickerapp.StickerPack$* { *; }
-keep class com.ketchupstudios.Switchstickerapp.PremiumItem { *; }

# Temas de Batería
-keep class com.ketchupstudios.Switchstickerapp.BatteryWidgetProvider { *; }
-keep class com.ketchupstudios.Switchstickerapp.WidgetUpdateWorker { *; }

# ============================================================
# 4. TUS ACTIVIDADES Y ADAPTADORES (UI)
# ============================================================

# Wallet y Edición de ID (Evita cierres al abrir Wallet)
-keep class com.ketchupstudios.Switchstickerapp.IdWalletActivity { *; }
-keep class com.ketchupstudios.Switchstickerapp.IdWalletActivity$* { *; }
-keep class com.ketchupstudios.Switchstickerapp.WalletHomeActivity { *; }
-keep class com.ketchupstudios.Switchstickerapp.WalletHomeActivity$* { *; }

# Lista de Amigos y Scanner (Evita cierres al ver lista)
-keep class com.ketchupstudios.Switchstickerapp.FriendsActivity { *; }
-keep class com.ketchupstudios.Switchstickerapp.FriendsActivity$* { *; }
-keep class com.ketchupstudios.Switchstickerapp.FriendsAdapter { *; }
-keep class com.ketchupstudios.Switchstickerapp.FriendsAdapter$* { *; }

# Componentes Visuales Personalizados
-keep class com.ketchupstudios.Switchstickerapp.HoloCardView { *; }
-keep class com.ketchupstudios.Switchstickerapp.*Adapter$ViewHolder { *; }

# ============================================================
# 5. SOLUCIÓN DEFINITIVA CRASH APK (COPIA ESTO AL FINAL)
# ============================================================

# 1. Protege la clase Reaction (Que es un archivo aparte, por eso fallaba con $)
-keep class com.ketchupstudios.Switchstickerapp.Reaction { *; }

# 2. Protege las clases internas de Config (Como LimitedEvent)
-keep class com.ketchupstudios.Switchstickerapp.Config$* { *; }

# 3. Protege HoloCardView y HoloController
-keep class com.ketchupstudios.Switchstickerapp.HoloCardView { *; }
-keep class com.ketchupstudios.Switchstickerapp.HoloController { *; }

# 4. REGLA MAESTRA PARA VISTAS EN XML (ESTA ES LA CLAVE)
# Esto le dice a Android: "Si alguna clase extiende de View, NO BORRES sus constructores"
# Esto es vital para que HoloCardView funcione en el layout.
-keepclassmembers class * extends android.view.View {
   <init>(android.content.Context);
   <init>(android.content.Context, android.util.AttributeSet);
   <init>(android.content.Context, android.util.AttributeSet, int);
}

# 5. Protege TODOS tus modelos para evitar fallos con GSON/JSON
# Si alguna variable se llama "imageFile" en el JSON, debe llamarse igual en el APK.
-keepclassmembers class com.ketchupstudios.Switchstickerapp.** {
    <fields>;
}