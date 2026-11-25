# AGENTS.md — Littera Reader

Coordinación del proyecto. Leer antes de tocar código.

## Qué es

Lector de libros Android (Kotlin + Jetpack Compose), 100% offline, para publicar en Apklis. Formatos: EPUB, PDF, DOCX, TXT. TTS integrado desde la v1.

## Stack verificado (no cambiar sin verificar en Maven Central)

- Gradle 9.7.0, AGP 9.3.1 (Kotlin integrado: NO aplicar `org.jetbrains.kotlin.android`), KGP 2.4.21 fijado por classpath en el build raíz; plugin de Compose con la misma versión.
- compileSdk 37, targetSdk 34, minSdk 24.
- Compose BOM 2026.09.00, core-ktx 1.19.0, activity-compose 1.13.0, lifecycle 2.11.0, coroutines 1.8.1.
- EPUB: Readium Kotlin Toolkit 3.4.0 (fase 2) — pide compileSdk 37, Kotlin 2.4.20+, Gradle 9.7.0, minSdk 24: calza exacto.
- PDF: `PdfRenderer` de Android (sin dependencias; NO extrae texto → sin TTS en PDF en la v1).
- DOCX: parseo directo del XML del archivo (un DOCX es un ZIP); NO Apache POI (pesada y problemática en Android).
- Biblioteca: índice JSON (`LibraryStore`), sin Room — KSP 2.3.x no tiene emparejamiento verificado con Kotlin 2.4.21.

## Fases

1. **Esqueleto + biblioteca + importar** (en curso).
2. Lector EPUB con Readium: temas, letra, capítulos, progreso, marcadores + metadatos reales al importar.
3. Lectores PDF, DOCX y TXT.
4. TTS (EPUB/DOCX/TXT) con controles y reanudación.
5. Pulido, firma release (v1+v2, Apklis exige v1) y publicación.

## Reglas de trabajo

- Builds SOLO por GitHub Actions (sin Android Studio). El workflow `.github/workflows/android.yml` lo crea Dahel por la web de GitHub (la API no tiene permiso de workflows); usa `gradle/actions/setup-gradle` con Gradle 9.7.0, sin wrapper (el JAR del wrapper es binario y no se puede subir por la API).
- Pushes por la API de contenidos: **texto plano** (base64 corrompe), **de uno en uno** (en paralelo chocan con 409), verificando el SHA del blob tras cada push. Timeout de push = resultado desconocido: verificar SHA antes de reintentar.
- Archivos interdependientes: primero el callee, después el caller.
- Firma release: keystore por variables de entorno `LITTERA_KEYSTORE_*` en CI; nunca en el repo.
- Cada cambio se anota en CHANGELOG.md (sin cabecera de versión hasta preparar release).

## Registro

| Fecha | Agente | Cambio | Archivos |
|---|---|---|---|
| 2026-11-25 | Malen-IA | Fase 1: esqueleto del proyecto (Gradle/AGP/Compose), biblioteca con índice JSON, importación SAF (EPUB/PDF/DOCX/TXT), icono y tema papel/tinta | todo el repo |
