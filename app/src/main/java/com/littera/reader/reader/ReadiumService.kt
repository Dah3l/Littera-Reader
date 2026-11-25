package com.littera.reader.reader

import android.content.Context
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

/**
 * Puerta de entrada a Readium: abre un archivo local como [Publication].
 * Sin fábrica PDF (pdfFactory = null): el PDF de Littera va por
 * PdfRenderer en la fase 3, no por Readium.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReadiumService(context: Context) {

    private val httpClient = DefaultHttpClient()

    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

    private val opener = PublicationOpener(
        DefaultPublicationParser(
            context,
            assetRetriever = assetRetriever,
            httpClient = httpClient,
            pdfFactory = null
        )
    )

    /** Abre el archivo, o null si Readium no puede con él. */
    suspend fun open(file: File): Publication? {
        val asset = assetRetriever.retrieve(file.toUrl(isDirectory = false))
            .getOrElse { return null }
        return opener.open(asset, allowUserInteraction = false)
            .getOrElse { return null }
    }
}
