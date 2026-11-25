package app.ledgerly.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import app.ledgerly.shared.di.platformModule
import app.ledgerly.shared.di.sharedModule
import app.ledgerly.shared.presentation.adddebt.AddDebtViewModel
import app.ledgerly.shared.presentation.app.AppViewModel
import app.ledgerly.shared.presentation.budgets.EditBudgetsViewModel
import app.ledgerly.shared.presentation.debts.DebtsViewModel
import app.ledgerly.shared.presentation.home.HomeViewModel
import app.ledgerly.shared.presentation.onboarding.OnboardingViewModel
import app.ledgerly.shared.presentation.quicklog.QuickLogViewModel
import app.ledgerly.shared.presentation.settings.SettingsViewModel
import app.ledgerly.shared.presentation.spend.SpendViewModel
import app.ledgerly.shared.presentation.transaction.TransactionDetailViewModel
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

    fun spend(owner: ScreenViewModelOwner): SpendViewModel = provide(owner, SpendViewModel::class) { get() }

    fun debts(owner: ScreenViewModelOwner): DebtsViewModel = provide(owner, DebtsViewModel::class) { get() }

    fun quickLog(owner: ScreenViewModelOwner): QuickLogViewModel = provide(owner, QuickLogViewModel::class) { get() }

    fun addDebt(owner: ScreenViewModelOwner): AddDebtViewModel = provide(owner, AddDebtViewModel::class) { get() }

    fun editBudgets(owner: ScreenViewModelOwner): EditBudgetsViewModel = provide(owner, EditBudgetsViewModel::class) { get() }

    fun transactionDetail(owner: ScreenViewModelOwner): TransactionDetailViewModel =
        provide(owner, TransactionDetailViewModel::class) { get() }

    fun app(owner: ScreenViewModelOwner): AppViewModel = provide(owner, AppViewModel::class) { get() }

    fun onboarding(owner: ScreenViewModelOwner): OnboardingViewModel = provide(owner, OnboardingViewModel::class) { get() }

    fun settings(owner: ScreenViewModelOwner): SettingsViewModel = provide(owner, SettingsViewModel::class) { get() }

    private fun <VM : ViewModel> provide(owner: ScreenViewModelOwner, type: KClass<VM>, create: () -> VM): VM {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = create() as T
        }
        return ViewModelProvider.create(owner, factory)[type]
    }
}
