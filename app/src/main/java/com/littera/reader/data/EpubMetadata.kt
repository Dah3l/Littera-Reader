package com.littera.reader.data

import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/** Metadatos reales extraídos del propio EPUB al importar. */
data class EpubMetadata(
    val title: String?,
    val author: String?,
    val coverBytes: ByteArray?,
    val coverExt: String?
)

/**
 * Lector mínimo de metadatos EPUB, sin dependencias: un EPUB es un ZIP;
 * META-INF/container.xml apunta al OPF, y el OPF trae título, autor y la
 * referencia a la portada (EPUB 3: item con properties="cover-image";
 * EPUB 2: <meta name="cover" content="id"/>). Solo se usa al importar,
 * para la ficha del libro; la lectura la hace Readium.
 */
object EpubMetadataParser {

    fun parse(file: File): EpubMetadata? {
        return try {
            ZipFile(file).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml")
                    ?: return null
                val containerXml = zip.getInputStream(containerEntry)
                    .bufferedReader().readText()
                val opfPath = findRootFilePath(containerXml) ?: return null
                val opfEntry = zip.getEntry(opfPath) ?: return null
                val opfXml = zip.getInputStream(opfEntry)
                    .bufferedReader().readText()
                val opf = parseOpf(opfXml)

                var coverBytes: ByteArray? = null
                var coverExt: String? = null
                val coverHref = opf.coverHref
                if (coverHref != null) {
                    val entryName = resolvePath(opfPath, coverHref)
                    val coverEntry = zip.getEntry(entryName)
                    if (coverEntry != null) {
                        coverBytes = zip.getInputStream(coverEntry).readBytes()
                        coverExt = entryName.substringAfterLast('.', "jpg").lowercase()
                    }
                }
                EpubMetadata(opf.title, opf.author, coverBytes, coverExt)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun newParser(xml: String): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xml))
        return parser
    }

    private fun findRootFilePath(containerXml: String): String? {
        val parser = newParser(containerXml)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
            }
            event = parser.next()
        }
        return null
    }

    private data class OpfData(
        val title: String?,
        val author: String?,
        val coverHref: String?
    )

    private fun parseOpf(opfXml: String): OpfData {
        val parser = newParser(opfXml)
        var title: String? = null
        var author: String? = null
        var coverHref: String? = null
        var epub2CoverId: String? = null
        val idToHref = mutableMapOf<String, String>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> if (title == null) {
                        title = parser.nextText().trim().ifBlank { null }
                    } else {
                        parser.nextText()
                    }
                    "creator" -> if (author == null) {
                        author = parser.nextText().trim().ifBlank { null }
                    } else {
                        parser.nextText()
                    }
                    "meta" -> {
                        if (parser.getAttributeValue(null, "name") == "cover") {
                            epub2CoverId = parser.getAttributeValue(null, "content")
                        }
                    }
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        val properties = parser.getAttributeValue(null, "properties")
                        if (id != null && href != null) idToHref[id] = href
                        if (href != null && properties != null &&
                            properties.split(' ').contains("cover-image")
                        ) {
                            coverHref = href
                        }
                    }
                }
            }
            event = parser.next()
        }
        if (coverHref == null && epub2CoverId != null) {
            coverHref = idToHref[epub2CoverId]
        }
        return OpfData(title, author, coverHref)
    }

    /** Resuelve un href del OPF contra la carpeta del OPF dentro del ZIP. */
    private fun resolvePath(opfPath: String, href: String): String {
        val clean = Uri.decode(href.substringBefore('#'))
        if (clean.startsWith("/")) return clean.trimStart('/')
        val dir = opfPath.substringBeforeLast('/', "")
        val joined = if (dir.isEmpty()) clean else "$dir/$clean"
        val segments = mutableListOf<String>()
        for (part in joined.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments.add(part)
            }
        }
        return segments.joinToString("/")
    }
}
