package com.littera.reader.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.littera.reader.data.Book
import com.littera.reader.data.BookFormat
import com.littera.reader.reader.ReaderActivity

@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val books by viewModel.books.collectAsState()
    val openingBookId by viewModel.openingBookId.collectAsState()
    val openReady by viewModel.openReady.collectAsState()
    val openError by viewModel.openError.collectAsState()
    var infoBook by remember { mutableStateOf<Book?>(null) }
    val context = LocalContext.current

    // Al volver del lector, el índice pudo cambiar (posición y progreso).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Libro abierto y sesión lista: lanzar la actividad del lector.
    LaunchedEffect(openReady) {
        if (openReady != null) {
            context.startActivity(Intent(context, ReaderActivity::class.java))
            viewModel.consumeOpenReady()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importBooks(uris)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Littera Reader") })
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        picker.launch(
                            arrayOf(
                                "application/epub+zip",
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain"
                            )
                        )
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Importar libro")
                }
            }
        ) { padding ->
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tu biblioteca está vacía.\nToca + para importar tu primer libro (EPUB, PDF, DOCX o TXT).",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(books, key = { it.id }) { book ->
                        ListItem(
                            headlineContent = { Text(book.title) },
                            supportingContent = { Text(supportingText(book)) },
                            leadingContent = { BookCover(viewModel, book) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteBook(book) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar libro")
                                }
                            },
                            modifier = Modifier.clickable {
                                if (book.format == BookFormat.EPUB) {
                                    viewModel.openBook(book)
                                } else {
                                    infoBook = book
                                }
                            }
                        )
                    }
                }
            }
        }

        if (openingBookId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    infoBook?.let { book ->
        AlertDialog(
            onDismissRequest = { infoBook = null },
            title = { Text(book.title) },
            text = {
                Text("El lector de ${book.format.name} llega en la fase 3. El libro ya está guardado en tu biblioteca.")
            },
            confirmButton = {
                TextButton(onClick = { infoBook = null }) { Text("Entendido") }
            }
        )
    }

    openError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.consumeOpenError() },
            title = { Text("No se pudo abrir") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.consumeOpenError() }) { Text("Entendido") }
            }
        )
    }
}

private fun supportingText(book: Book): String {
    val parts = mutableListOf(book.format.name)
    if (book.author.isNotBlank()) parts.add(book.author)
    if (book.progress > 0f) parts.add("${(book.progress * 100).toInt()}% leído")
    return parts.joinToString(" · ")
}

@Composable
private fun BookCover(viewModel: LibraryViewModel, book: Book) {
    val bitmap = remember(book.id, book.coverFileName) {
        viewModel.coverFile(book)?.let { file ->
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(width = 44.dp, height = 62.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 62.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.format.name.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
