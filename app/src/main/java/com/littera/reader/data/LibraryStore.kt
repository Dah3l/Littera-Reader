package com.littera.reader.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Biblioteca local: índice JSON en filesDir/library.json + los archivos
 * de los libros en filesDir/books/.
 *
 * Fase 1 sin Room a propósito: el procesador de anotaciones de Room
 * (KSP 2.3.x) no tiene emparejamiento verificado con Kotlin 2.4.21, y un
 * índice JSON cubre de sobra una biblioteca personal (cientos de libros)
 * sin arriesgar el build. Si algún día hace falta Room, la migración es
 * leer este mismo JSON una vez.
 */
class LibraryStore(rootDir: File) {

    val booksDir: File = File(rootDir, "books").apply { mkdirs() }
    private val indexFile = File(rootDir, "library.json")

    fun bookFile(book: Book): File = File(booksDir, book.fileName)

    fun load(): List<Book> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length())
                .map { i -> arr.getJSONObject(i).toBook() }
                .sortedByDescending { it.addedAtMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(books: List<Book>) {
        val arr = JSONArray()
        books.forEach { arr.put(it.toJson()) }
        indexFile.writeText(arr.toString())
    }

    private fun Book.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("author", author)
        .put("format", format.name)
        .put("fileName", fileName)
        .put("addedAtMs", addedAtMs)
        .put("progress", progress.toDouble())
        .put("locator", locator ?: JSONObject.NULL)

    private fun JSONObject.toBook(): Book = Book(
        id = getString("id"),
        title = getString("title"),
        author = optString("author", ""),
        format = BookFormat.valueOf(getString("format")),
        fileName = getString("fileName"),
        addedAtMs = optLong("addedAtMs", 0L),
        progress = optDouble("progress", 0.0).toFloat(),
        locator = if (isNull("locator")) null else optString("locator")
    )
}
