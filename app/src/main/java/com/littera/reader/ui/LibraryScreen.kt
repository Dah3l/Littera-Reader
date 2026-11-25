package com.littera.reader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littera.reader.data.Book

@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val books by viewModel.books.collectAsState()
    var infoBook by remember { mutableStateOf<Book?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importBooks(uris)
    }

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
                        supportingContent = {
                            Text(
                                if (book.author.isNotBlank()) "${book.format.name} · ${book.author}"
                                else book.format.name
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteBook(book) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Eliminar libro")
                            }
                        },
                        modifier = Modifier.clickable { infoBook = book }
                    )
                }
            }
        }
    }

    infoBook?.let { book ->
        AlertDialog(
            onDismissRequest = { infoBook = null },
            title = { Text(book.title) },
            text = {
                Text("El lector de ${book.format.name} llega en la siguiente fase. El libro ya está guardado en tu biblioteca.")
            },
            confirmButton = {
                TextButton(onClick = { infoBook = null }) { Text("Entendido") }
            }
        )
    }
}
