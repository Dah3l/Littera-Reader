// Archivo de construcción raíz del proyecto Littera Reader
// Stack (2026-11-25): AGP 9.3.1 con Kotlin integrado (built-in Kotlin):
// el plugin org.jetbrains.kotlin.android NO se aplica, es incompatible
// con el DSL nuevo de AGP 9. La versión de KGP se fija con el classpath
// de buildscript y el plugin de Compose lleva la MISMA versión.
// Combo verificado en AuralPlayer: KGP 2.4.21 + Gradle 9.7.0.
// Readium 3.4.0 (fase 2, EPUB) pide exactamente esto: compileSdk 37,
// Kotlin 2.4.20+, Gradle 9.7.0, minSdk 24.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
