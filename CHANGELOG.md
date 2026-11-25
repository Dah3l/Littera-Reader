# Changelog — Littera Reader

## En desarrollo — Fase 2: lector EPUB (Readium)

- Lector EPUB completo con Readium Kotlin Toolkit 3.4.0: paginación, temas Claro/Oscuro/Sepia, tamaño de letra y fuente (Original/Serif/Sans), índice de capítulos, barra de progreso.
- La posición exacta de lectura se guarda sola y el libro se reabre donde iba; la biblioteca muestra el % leído.
- Marcadores: añadir con un toque, saltar a ellos y eliminarlos.
- Al importar un EPUB se extraen su título, autor y portada reales, y la biblioteca muestra las portadas.
- Preferencias de lectura compartidas entre libros y persistentes (JSON de Readium).

## En desarrollo — Fase 1: esqueleto y biblioteca

- Proyecto Android creado: Kotlin + Jetpack Compose + Material 3 (AGP 9.3.1, Gradle 9.7.0, compileSdk 37, minSdk 24).
- Biblioteca local con índice JSON: importar libros (EPUB, PDF, DOCX, TXT) desde el selector de archivos, copiarlos al almacenamiento interno, listarlos y eliminarlos.
- Tema visual "papel y tinta" (claro/oscuro según el sistema) e icono de libro abierto.
- Firma release preparada con v1+v2 (requisito de Apklis), keystore por variables de entorno en CI.
