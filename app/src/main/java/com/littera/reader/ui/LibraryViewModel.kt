package com.littera.reader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.littera.reader.data.Book
import com.littera.reader.data.BookFormat
import com.littera.reader.data.BookImporter
import com.littera.reader.data.LibraryStore
import com.littera.reader.reader.EpubPrefsStore
import com.littera.reader.reader.ReaderSession
import com.littera.reader.reader.ReadiumService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import java.io.File

@OptIn(ExperimentalReadiumApi::class)
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LibraryStore(app.filesDir)

    private val readiumService by lazy { ReadiumService(getApplication()) }
    private val prefsStore by lazy { EpubPrefsStore(getApplication()) }

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    /** Id del libro que se está abriendo (para el indicador de carga). */
    private val _openingBookId = MutableStateFlow<String?>(null)
    val openingBookId: StateFlow<String?> = _openingBookId.asStateFlow()

    /** Id del libro listo en ReaderSession: la pantalla lanza el lector. */
    private val _openReady = MutableStateFlow<String?>(null)
    val openReady: StateFlow<String?> = _openReady.asStateFlow()

    private val _openError = MutableStateFlow<String?>(null)
    val openError: StateFlow<String?> = _openError.asStateFlow()

    init {
        _books.value = store.load()
    }

    fun refresh() {
        _books.value = store.load()
    }

    fun coverFile(book: Book): File? = store.coverFile(book)

    fun importBooks(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val added = withContext(Dispatchers.IO) {
                uris.mapNotNull { BookImporter.import(context, store, it) }
            }
            if (added.isNotEmpty()) {
                val updated = (added + _books.value).sortedByDescending { it.addedAtMs }
                _books.value = updated
                withContext(Dispatchers.IO) { store.save(updated) }
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.bookFile(book).delete()
                store.coverFile(book)?.delete()
                store.saveBookmarks(book.id, emptyList())
            }
            val updated = _books.value.filterNot { it.id == book.id }
            _books.value = updated
            withContext(Dispatchers.IO) { store.save(updated) }
        }
    }

    /**
     * Abre un EPUB con Readium y deja la sesión lista en [ReaderSession];
     * al terminar, [_openReady] trae el id y la pantalla lanza el lector.
     */
    fun openBook(book: Book) {
        if (book.format != BookFormat.EPUB) return
        if (_openingBookId.value != null) return
        viewModelScope.launch {
            _openingBookId.value = book.id
            val publication = withContext(Dispatchers.IO) {
                readiumService.open(store.bookFile(book))
            }
            _openingBookId.value = null
            if (publication == null) {
                _openError.value =
                    "No se pudo abrir «${book.title}». El archivo puede estar dañado o no ser un EPUB válido."
                return@launch
            }
            val initialLocator = book.locator?.let { json ->
                try {
                    Locator.fromJSON(JSONObject(json))
                } catch (e: Exception) {
                    null
                }
            }
            ReaderSession.start(
                bookId = book.id,
                publication = publication,
                initialLocator = initialLocator,
                initialPreferences = prefsStore.load()
            )
            _openReady.value = book.id
        }
    }

    fun consumeOpenReady() {
        _openReady.value = null
    }

    fun consumeOpenError() {
        _openError.value = null
    }
}
