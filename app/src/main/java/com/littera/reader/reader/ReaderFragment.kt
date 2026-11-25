package com.littera.reader.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError

/**
 * Fragmento anfitrión del EpubNavigatorFragment de Readium, con el
 * patrón oficial: la FragmentFactory del hijo se instala en onCreate,
 * ANTES de super, con la posición y las preferencias iniciales de la
 * sesión. Si la sesión no existe (el proceso murió y Android restaura
 * la actividad), se instala la fábrica dummy y la actividad se cierra:
 * el libro se reabre desde la biblioteca en su última posición.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderFragment : Fragment(), EpubNavigatorFragment.Listener {

    private lateinit var navigator: EpubNavigatorFragment
    private var containerId: Int = 0

    private val readerViewModel: ReaderViewModel by lazy {
        ViewModelProvider(requireActivity())[ReaderViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val factory = ReaderSession.navigatorFactory
        if (factory == null) {
            childFragmentManager.fragmentFactory =
                EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            requireActivity().finish()
            return
        }
        childFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = ReaderSession.initialLocator,
            initialPreferences = ReaderSession.initialPreferences,
            listener = this
        )
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        containerId = View.generateViewId()
        return FragmentContainerView(requireContext()).apply {
            id = containerId
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (ReaderSession.navigatorFactory == null) return

        if (childFragmentManager.findFragmentByTag(TAG) == null) {
            childFragmentManager.beginTransaction()
                .add(containerId, EpubNavigatorFragment::class.java, Bundle(), TAG)
                .commitNow()
        }
        navigator = childFragmentManager.findFragmentByTag(TAG)
            as EpubNavigatorFragment
        readerViewModel.navigator = navigator

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navigator.currentLocator.collect { locator ->
                    readerViewModel.saveProgression(locator)
                }
            }
        }
    }

    // --- EpubNavigatorFragment.Listener ---

    override fun onResourceLoadFailed(href: Url, error: ReadError) {
        // Un recurso suelto del EPUB no cargó (imagen, CSS): se ignora,
        // el texto sigue siendo legible.
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())))
        } catch (e: Exception) {
            // Sin navegador disponible: se ignora el enlace externo.
        }
    }

    override fun shouldFollowInternalLink(
        link: Link,
        context: HyperlinkNavigator.LinkContext?
    ): Boolean = true

    companion object {
        private const val TAG = "epub-navigator"
    }
}
