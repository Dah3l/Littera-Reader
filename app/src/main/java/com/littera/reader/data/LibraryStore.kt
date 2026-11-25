package com.littera.reader.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Un marcador dentro de un libro: posición Readium serializada + etiqueta. */
data class Bookmark(
    val locatorJson: String,
    val label: String,
    val createdAtMs: Long
)

/**
 * Biblioteca local: índice JSON en filesDir/library.json + los archivos
 * de los libros en filesDir/books/ + portadas en filesDir/covers/ +
 * marcadores en filesDir/bookmarks.json.
 *
 * Sin Room a propósito: el procesador de anotaciones de Room (KSP 2.3.x)
 * no tiene emparejamiento verificado con Kotlin 2.4.21, y un índice JSON
 * cubre de sobra una biblioteca personal (cientos de libros) sin arriesgar
 * el build. Si algún día hace falta Room, la migración es leer estos JSON.
 */
class LibraryStore(rootDir: File) {

    val booksDir: File = File(rootDir, "books").apply { mkdirs() }
    val coversDir: File = File(rootDir, "covers").apply { mkdirs() }
    private val indexFile = File(rootDir, "library.json")
    private val bookmarksFile = File(rootDir, "bookmarks.json")

    fun bookFile(book: Book): File = File(booksDir, book.fileName)

    fun coverFile(book: Book): File? =
        book.coverFileName?.let { File(coversDir, it) }

    // ---------- Libros ----------

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

    /** Inserta o reemplaza un libro en el índice (por id). */
    fun updateBook(book: Book) {
        val all = load().toMutableList()
        val i = all.indexOfFirst { it.id == book.id }
        if (i >= 0) all[i] = book else all.add(book)
        save(all)
    }

    // ---------- Marcadores ----------

    fun loadBookmarks(bookId: String): List<Bookmark> {
        val all = readBookmarksJson()
        val arr = all.optJSONArray(bookId) ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Bookmark(
                locatorJson = o.getString("locator"),
                label = o.optString("label", ""),
                createdAtMs = o.optLong("createdAt", 0L)
            )
        }
    }

    fun saveBookmarks(bookId: String, bookmarks: List<Bookmark>) {
        val all = readBookmarksJson()
        if (bookmarks.isEmpty()) {
            all.remove(bookId)
        } else {
            val arr = JSONArray()
            bookmarks.forEach { b ->
                arr.put(
                    JSONObject()
                        .put("locator", b.locatorJson)
                        .put("label", b.label)
                        .put("createdAt", b.createdAtMs)
                )
            }
            all.put(bookId, arr)
        }
        bookmarksFile.writeText(all.toString())
    }

    private fun readBookmarksJson(): JSONObject {
        if (!bookmarksFile.exists()) return JSONObject()
        return try {
            JSONObject(bookmarksFile.readText())
        } catch (e: Exception) {
            JSONObject()
        }
    }

    // ---------- JSON de Book ----------

    private fun Book.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("author", author)
        .put("format", format.name)
        .put("fileName", fileName)
        .put("addedAtMs", addedAtMs)
        .put("progress", progress.toDouble())
        .put("locator", locator ?: JSONObject.NULL)
        .put("coverFileName", coverFileName ?: JSONObject.NULL)

    private fun JSONObject.toBook(): Book = Book(
        id = getString("id"),
        title = getString("title"),
        author = optString("author", ""),
        format = BookFormat.valueOf(getString("format")),
        fileName = getString("fileName"),
        addedAtMs = optLong("addedAtMs", 0L),
        progress = optDouble("progress", 0.0).toFloat(),
        locator = if (isNull("locator")) null else optString("locator"),
        coverFileName = if (isNull("coverFileName")) null else optString("coverFileName")
    )
}
