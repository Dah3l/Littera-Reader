package com.littera.reader.reader

import android.content.Context
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi
import java.io.File

/**
 * Preferencias de lectura EPUB (tema, letra, fuente) compartidas por
 * todos los libros, guardadas como JSON con el serializador de Readium
 * en filesDir/epub-prefs.json.
 */
@OptIn(ExperimentalReadiumApi::class)
class EpubPrefsStore(context: Context) {

    private val file = File(context.filesDir, "epub-prefs.json")
    private val serializer = EpubPreferencesSerializer()

    fun load(): EpubPreferences = try {
        if (file.exists()) serializer.deserialize(file.readText())
        else EpubPreferences()
    } catch (e: Exception) {
        EpubPreferences()
    }

    fun save(preferences: EpubPreferences) {
        try {
            file.writeText(serializer.serialize(preferences))
        } catch (e: Exception) {
            // Preferencias sin guardar: se aplican igual en esta sesión.
        }
    }
}
