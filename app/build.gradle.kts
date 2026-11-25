// Configuración del módulo de aplicación Littera Reader
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    // Sin org.jetbrains.kotlin.android: AGP 9 trae Kotlin integrado.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.littera.reader"
    // compileSdk 37: lo exige Compose (BOM 2026.09.00) + AGP 9, y es el
    // mismo que pide Readium 3.4.0 para la fase 2.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.littera.reader"
        // minSdk 24: mínimo que pide Readium 3.4.0; cubre teléfonos viejos.
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            // Solo en CI: la keystore llega decodificada a una ruta temporal.
            // Sin estas variables de entorno (build local), esta config queda vacía.
            val ksPath = System.getenv("LITTERA_KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("LITTERA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LITTERA_KEY_ALIAS")
                keyPassword = System.getenv("LITTERA_KEY_PASSWORD")
            }
            // Apklis exige firma v1 (JAR) para validar la llave: con minSdk 24
            // Gradle la omite por defecto, así que se fuerza explícitamente.
            // (Nombres nuevos de AGP 9: enableV1Signing/enableV2Signing.)
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            // Paquete aparte (com.littera.reader.debug): la build de prueba
            // convive con la release instalada sin tocar sus datos.
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Littera DEBUG")
        }
        release {
            // Firma con la keystore oficial cuando está disponible (CI)
            if (System.getenv("LITTERA_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Desugaring: OBLIGATORIO para Readium (usa APIs nuevas de Java
        // en minSdk viejos). Sabor NIO, el mismo verificado en AuralPlayer.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        // AGP 9 trae resValues apagado por defecto: el buildType debug
        // usa resValue para el nombre "Littera DEBUG", así que se enciende.
        resValues = true
    }
}

// Opciones del compilador Kotlin (DSL nuevo de AGP 9): bytecode JVM 17 +
// opt-in global de Material3 experimental.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    // ---- Jetpack Compose + Material 3 ----
    // BOM 2026.09.00: el mismo verificado en AuralPlayer.
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---- Core KTX ----
    implementation("androidx.core:core-ktx:1.19.0")

    // ---- Actividad y ciclo de vida ----
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0")

    // ---- Coroutines ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ---- Desugaring (ver compileOptions) ----
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    // ---- Readium Kotlin Toolkit 3.4.0 (lector EPUB, fase 2) ----
    // shared: modelo Publication/Locator · streamer: abrir el EPUB ·
    // navigator: el lector visual (EpubNavigatorFragment).
    implementation("org.readium.kotlin-toolkit:readium-shared:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.4.0")

    // Fragment: Readium lo usa como dependencia interna y NO llega al
    // classpath de compilación de la app; el lector (FragmentActivity,
    // Fragment, FragmentContainerView) lo necesita declarado aquí.
    // Misma versión que usa Readium 3.4.0.
    implementation("androidx.fragment:fragment-ktx:1.9.0")
}
