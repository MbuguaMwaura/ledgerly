package app.ledgerly.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import app.ledgerly.shared.di.platformModule
import app.ledgerly.shared.di.sharedModule
import app.ledgerly.shared.home.HomeViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import kotlin.reflect.KClass

/** Called once from the SwiftUI `App` initialiser: `LedgerlyShared.shared.start()`. */
object LedgerlyShared {
    private var started = false

    fun start() {
        if (started) return
        startKoin { modules(sharedModule, platformModule) }
        started = true
    }
}

/**
 * Owns the ViewModels of one SwiftUI screen. The Swift wrapper keeps one of these and calls
 * [clear] when the screen goes away, which cancels each ViewModel's `viewModelScope`.
 */
class ScreenViewModelOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    fun clear() = viewModelStore.clear()
}

/** Hands Swift the shared ViewModels, built by Koin and owned by a [ScreenViewModelOwner]. */
object ViewModels : KoinComponent {
    fun home(owner: ScreenViewModelOwner): HomeViewModel = provide(owner, HomeViewModel::class) { get() }

    private fun <VM : ViewModel> provide(owner: ScreenViewModelOwner, type: KClass<VM>, create: () -> VM): VM {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = create() as T
        }
        return ViewModelProvider.create(owner, factory)[type]
    }
}
