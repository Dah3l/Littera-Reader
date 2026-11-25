package com.littera.reader.reader

import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Sesión de lectura en curso: la publicación abierta y todo lo que
 * [ReaderFragment] necesita para crear el Navigator. Vive en memoria
 * (una Publication no es serializable); si el proceso muere, la sesión
 * se pierde y el lector se cierra solo (patrón de la app de prueba de
 * Readium): el usuario reabre el libro y sigue donde iba, porque la
 * posición se guarda en el índice de la biblioteca.
 */
@OptIn(ExperimentalReadiumApi::class)
object ReaderSession {

    var bookId: String? = null
        private set
    var publication: Publication? = null
        private set
    var navigatorFactory: EpubNavigatorFactory? = null
        private set
    var initialLocator: Locator? = null
        private set
    var initialPreferences: EpubPreferences = EpubPreferences()
        private set

    fun start(
        bookId: String,
        publication: Publication,
        initialLocator: Locator?,
        initialPreferences: EpubPreferences
    ) {
        clear()
        this.bookId = bookId
        this.publication = publication
        this.navigatorFactory = EpubNavigatorFactory(publication)
        this.initialLocator = initialLocator
        this.initialPreferences = initialPreferences
    }

    fun clear() {
        publication?.close()
        bookId = null
        publication = null
        navigatorFactory = null
        initialLocator = null
    }
}
