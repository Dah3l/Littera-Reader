package com.littera.reader.data

/** Formatos que Littera Reader sabe abrir. */
enum class BookFormat { EPUB, PDF, DOCX, TXT }

/**
 * Un libro de la biblioteca. El archivo vive en el almacenamiento interno
 * de la app (filesDir/books/<fileName>); el índice JSON guarda el resto.
 * [locator] guardará la posición exacta de lectura de cada motor
 * (Readium en EPUB, página en PDF, desplazamiento en texto).
 */
data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val format: BookFormat,
    val fileName: String,
    val addedAtMs: Long,
    val progress: Float = 0f,
    val locator: String? = null,
    /** Nombre del archivo de portada en filesDir/covers/, si el libro trae una. */
    val coverFileName: String? = null
)
