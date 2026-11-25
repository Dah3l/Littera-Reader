package com.littera.reader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.littera.reader.data.Book
import com.littera.reader.data.BookImporter
import com.littera.reader.data.LibraryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LibraryStore(app.filesDir)

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    init {
        _books.value = store.load()
    }

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
            withContext(Dispatchers.IO) { store.bookFile(book).delete() }
            val updated = _books.value.filterNot { it.id == book.id }
            _books.value = updated
            withContext(Dispatchers.IO) { store.save(updated) }
        }
    }
}
