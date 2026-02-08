plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ketchupstudios.Switchstickerapp"
    compileSdk {
        version = release(35)
    }

    defaultConfig {
        applicationId = "com.ketchupstudios.Switchstickerapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 88
        versionName = "14.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true       // Activa ProGuard (Seguridad)
            isShrinkResources = true     // Elimina archivos inútiles (Menor peso)
           // isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // 1. IMÁGENES
    implementation("com.github.bumptech.glide:glide:4.15.1")

    // 2. PUBLICIDAD
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.google.ads.mediation:unity:4.12.0.0")

    // 3. BASE DE ANDROID (Versiones directas para evitar errores de 'libs')
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // <--- ESTA ERA LA QUE FALLABA

    // 4. FIREBASE (Esto solucionará lo rojo en Messaging y Analytics)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // --- AGREGA ESTAS NUEVAS ---
    implementation ("com.google.firebase:firebase-auth")       // Para Login
    implementation ("com.google.firebase:firebase-firestore")  // Base de datos
    implementation ("com.google.android.gms:play-services-auth:20.7.0") // Google Login

    // 5. OTRAS UTILIDADES
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.palette:palette:1.0.0")

    // 6. TESTS
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Escáner de QR (Cámara)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
// Para guardar la lista de amigos (JSON)
    implementation("com.google.code.gson:gson:2.10.1")
}