package com.littera.reader.reader

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.littera.reader.R
import com.littera.reader.ui.theme.LitteraTheme

/**
 * Actividad del lector EPUB: arriba la barra Compose ([ReaderChrome])
 * y debajo el [ReaderFragment] que aloja el Navigator de Readium.
 * La publicación ya viene abierta en [ReaderSession] (la abre la
 * biblioteca antes de lanzar esta actividad).
 */
class ReaderActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        val viewModel = ViewModelProvider(this)[ReaderViewModel::class.java]
        findViewById<ComposeView>(R.id.reader_chrome).setContent {
            LitteraTheme {
                ReaderChrome(viewModel, onClose = { finish() })
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.reader_container, ReaderFragment())
                .commitNow()
        }
    }

    override fun onDestroy() {
        // Al salir del lector se cierra la publicación y se libera la
        // sesión; en un cambio de configuración (rotación) la sesión
        // sigue viva y el lector se restaura donde iba.
        if (isFinishing) ReaderSession.clear()
        super.onDestroy()
    }
}
