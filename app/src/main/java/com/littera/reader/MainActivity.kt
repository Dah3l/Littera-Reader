package com.littera.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.littera.reader.ui.LibraryScreen
import com.littera.reader.ui.LibraryViewModel
import com.littera.reader.ui.theme.LitteraTheme

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LitteraTheme {
                LibraryScreen(libraryViewModel)
            }
        }
    }
}
