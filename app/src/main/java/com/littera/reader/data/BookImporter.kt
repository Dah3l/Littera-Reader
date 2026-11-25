package com.littera.reader.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

/**
 * Importa libros desde el selector de archivos del sistema (SAF):
 * copia el archivo al almacenamiento interno y crea su ficha.
 * En la fase 1 el título sale del nombre del archivo; la extracción de
 * metadatos reales (título/autor/portada del EPUB) llega con Readium
 * en la fase 2.
 */
object BookImporter {

    fun formatOf(fileName: String): BookFormat? = when {
        fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
        fileName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
        fileName.endsWith(".docx", ignoreCase = true) -> BookFormat.DOCX
        fileName.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
        else -> null
    }

    private fun displayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
            }
        } catch (e: Exception) {
            // Sin nombre legible: se usa el último segmento de la URI.
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    /** Copia el libro y devuelve su ficha, o null si no es un formato válido. */
    fun import(context: Context, store: LibraryStore, uri: Uri): Book? {
        val name = displayName(context, uri) ?: return null
        val format = formatOf(name) ?: return null
        val id = UUID.randomUUID().toString()
        val ext = name.substringAfterLast('.', "").lowercase()
        val fileName = "$id.$ext"
        val dest = File(store.booksDir, fileName)
        val copied = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } != null
        } catch (e: Exception) {
            false
        }
        if (!copied) {
            dest.delete()
            return null
        }
        val title = name.substringBeforeLast('.').replace('_', ' ').trim()
        return Book(
            id = id,
            title = title.ifBlank { name },
            format = format,
            fileName = fileName,
            addedAtMs = System.currentTimeMillis()
        )
    }
}
