package com.littera.reader.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import kotlin.math.roundToInt

/**
 * Barra del lector EPUB (Compose sobre el Fragment de Readium):
 * título, capítulos, marcadores y apariencia (tema, letra, fuente),
 * con la barra de progreso de lectura debajo.
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderChrome(viewModel: ReaderViewModel, onClose: () -> Unit) {
    val preferences by viewModel.preferences.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val progress by viewModel.progress.collectAsState()

    var showToc by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    viewModel.bookTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = { showToc = true }) {
                    Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Capítulos")
                }
                IconButton(onClick = { viewModel.addBookmark() }) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Añadir marcador")
                }
                IconButton(onClick = { showBookmarks = true }) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = "Marcadores")
                }
                IconButton(onClick = { showAppearance = true }) {
                    Icon(Icons.Rounded.FormatSize, contentDescription = "Apariencia")
                }
            }
        )
        LinearProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showToc) {
        AlertDialog(
            onDismissRequest = { showToc = false },
            title = { Text("Capítulos") },
            text = {
                val flat = remember(viewModel.tableOfContents) {
                    flattenToc(viewModel.tableOfContents)
                }
                if (flat.isEmpty()) {
                    Text("Este libro no trae índice de capítulos.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(flat) { (link, depth) ->
                            ListItem(
                                headlineContent = {
                                    Text(link.title ?: "Sin título")
                                },
                                modifier = Modifier
                                    .clickable {
                                        viewModel.goTo(link)
                                        showToc = false
                                    }
                                    .padding(start = (depth * 16).dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToc = false }) { Text("Cerrar") }
            }
        )
    }

    if (showAppearance) {
        AlertDialog(
            onDismissRequest = { showAppearance = false },
            title = { Text("Apariencia") },
            text = {
                Column {
                    Text("Tema")
                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                        FilterChip(
                            selected = preferences.theme == Theme.LIGHT,
                            onClick = {
                                viewModel.updatePreferences { it.copy(theme = Theme.LIGHT) }
                            },
                            label = { Text("Claro") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = preferences.theme == Theme.DARK,
                            onClick = {
                                viewModel.updatePreferences { it.copy(theme = Theme.DARK) }
                            },
                            label = { Text("Oscuro") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = preferences.theme == Theme.SEPIA,
                            onClick = {
                                viewModel.updatePreferences { it.copy(theme = Theme.SEPIA) }
                            },
                            label = { Text("Sepia") }
                        )
                    }
                    Text("Tamaño de letra")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.updatePreferences {
                                    it.copy(fontSize = stepFontSize(it.fontSize ?: 1.0, -0.1))
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Letra más pequeña")
                        }
                        Text("${(((preferences.fontSize ?: 1.0) * 100).roundToInt())}%")
                        IconButton(
                            onClick = {
                                viewModel.updatePreferences {
                                    it.copy(fontSize = stepFontSize(it.fontSize ?: 1.0, 0.1))
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Letra más grande")
                        }
                    }
                    Text("Fuente")
                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                        FilterChip(
                            selected = preferences.fontFamily == null,
                            onClick = {
                                viewModel.updatePreferences { it.copy(fontFamily = null) }
                            },
                            label = { Text("Original") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = preferences.fontFamily == FontFamily.SERIF,
                            onClick = {
                                viewModel.updatePreferences { it.copy(fontFamily = FontFamily.SERIF) }
                            },
                            label = { Text("Serif") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = preferences.fontFamily == FontFamily.SANS_SERIF,
                            onClick = {
                                viewModel.updatePreferences { it.copy(fontFamily = FontFamily.SANS_SERIF) }
                            },
                            label = { Text("Sans") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearance = false }) { Text("Cerrar") }
            }
        )
    }

    if (showBookmarks) {
        AlertDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text("Marcadores") },
            text = {
                if (bookmarks.isEmpty()) {
                    Text("Todavía no tienes marcadores en este libro. Añade uno con el botón de marcador de la barra.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(bookmarks, key = { it.createdAtMs }) { bookmark ->
                            ListItem(
                                headlineContent = {
                                    Text(bookmark.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.deleteBookmark(bookmark) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Eliminar marcador")
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.goTo(bookmark)
                                    showBookmarks = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) { Text("Cerrar") }
            }
        )
    }
}

private fun stepFontSize(current: Double, delta: Double): Double {
    val stepped = ((current + delta) * 10).roundToInt() / 10.0
    return stepped.coerceIn(0.7, 1.8)
}

private fun flattenToc(links: List<Link>, depth: Int = 0): List<Pair<Link, Int>> =
    links.flatMap { link ->
        listOf(link to depth) + flattenToc(link.children, depth + 1)
    }
