package com.littera.reader.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.littera.reader.data.Bookmark
import com.littera.reader.data.LibraryStore
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado del lector EPUB para una sesión: preferencias, marcadores,
 * progreso y el puente con el Navigator (que vive en [ReaderFragment]).
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LibraryStore(app.filesDir)
    private val prefsStore = EpubPrefsStore(app)

    val bookId: String? = ReaderSession.bookId

    val bookTitle: String =
        store.load().firstOrNull { it.id == bookId }?.title ?: "Libro"

    val tableOfContents: List<Link> =
        ReaderSession.publication?.tableOfContents ?: emptyList()

    /** Lo asigna ReaderFragment al crear el Navigator. */
    var navigator: EpubNavigatorFragment? = null

    private val _preferences = MutableStateFlow(ReaderSession.initialPreferences)
    val preferences: StateFlow<EpubPreferences> = _preferences.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _progress = MutableStateFlow(0.0)
    val progress: StateFlow<Double> = _progress.asStateFlow()

    private var lastLocatorJson: String? = null

    init {
        val id = bookId
        if (id != null) {
            _bookmarks.value = store.loadBookmarks(id)
            store.load().firstOrNull { it.id == id }
                ?.let { _progress.value = it.progress.toDouble() }
        }
    }

    /** Guarda la posición exacta y el % leído en el índice de la biblioteca. */
    fun saveProgression(locator: Locator) {
        locator.locations.totalProgression?.let { _progress.value = it }
        val json = locator.toJSON().toString()
        if (json == lastLocatorJson) return
        lastLocatorJson = json
        val id = bookId ?: return
        val current = store.load().firstOrNull { it.id == id } ?: return
        store.updateBook(
            current.copy(
                locator = json,
                progress = locator.locations.totalProgression?.toFloat()
                    ?: current.progress
            )
        )
    }

    fun updatePreferences(transform: (EpubPreferences) -> EpubPreferences) {
        val updated = transform(_preferences.value)
        _preferences.value = updated
        navigator?.submitPreferences(updated)
        prefsStore.save(updated)
    }

    fun addBookmark() {
        val nav = navigator ?: return
        val id = bookId ?: return
        val locator = nav.currentLocator.value
        val percent = ((locator.locations.totalProgression ?: 0.0) * 100).toInt()
        val label = locator.text.highlight?.trim()?.take(80)?.ifBlank { null }
            ?: "Posición $percent%"
        val bookmark = Bookmark(
            locatorJson = locator.toJSON().toString(),
            label = label,
            createdAtMs = System.currentTimeMillis()
        )
        val updated = listOf(bookmark) + _bookmarks.value
        _bookmarks.value = updated
        store.saveBookmarks(id, updated)
    }

    fun deleteBookmark(bookmark: Bookmark) {
        val id = bookId ?: return
        val updated = _bookmarks.value.filterNot { it === bookmark }
        _bookmarks.value = updated
        store.saveBookmarks(id, updated)
    }

    fun goTo(link: Link) {
        navigator?.go(link)
    }

    fun goTo(bookmark: Bookmark) {
        val locator = try {
            Locator.fromJSON(JSONObject(bookmark.locatorJson))
        } catch (e: Exception) {
            null
        } ?: return
        navigator?.go(locator)
    }
}
