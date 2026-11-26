package app.ledgerly.shared.di

import app.ledgerly.shared.data.LocalBudgetRepository
import app.ledgerly.shared.data.LocalCategoryRepository
import app.ledgerly.shared.data.LocalDebtRepository
import app.ledgerly.shared.data.LocalLogRepository
import app.ledgerly.shared.data.LocalSettingsRepository
import app.ledgerly.shared.data.LocalTransactionRepository
import app.ledgerly.shared.db.LedgerlyDatabase
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.repository.DebtRepository
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.repository.SettingsRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.domain.time.SystemAppClock
import app.ledgerly.shared.domain.usecase.AddDebt
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.domain.usecase.LogNothingSpent
import app.ledgerly.shared.domain.usecase.MarkReminded
import app.ledgerly.shared.domain.usecase.ObserveHomeSummary
import app.ledgerly.shared.domain.usecase.RecordDebtPayment
import app.ledgerly.shared.presentation.adddebt.AddDebtViewModel
import app.ledgerly.shared.presentation.app.AppViewModel
import app.ledgerly.shared.presentation.budgets.EditBudgetsViewModel
import app.ledgerly.shared.presentation.debts.DebtsViewModel
import app.ledgerly.shared.presentation.home.HomeViewModel
import app.ledgerly.shared.presentation.onboarding.OnboardingViewModel
import app.ledgerly.shared.presentation.quicklog.QuickLogViewModel
import app.ledgerly.shared.presentation.settings.SettingsViewModel
import app.ledgerly.shared.presentation.settings.ChangeCurrencyViewModel
import app.ledgerly.shared.presentation.spend.SpendViewModel
import app.ledgerly.shared.presentation.transaction.TransactionDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Everything the shared module provides. Each app adds [platformModule], which supplies the
 * SQLite driver for its platform.
 */
val sharedModule: Module = module {
    single<AppClock> { SystemAppClock }
    single { LedgerlyDatabase(get()) }

    // Local-only for v1. A remote-aware implementation can replace these bindings later.
    single { LocalCategoryRepository(get(), Dispatchers.IO) } bind CategoryRepository::class
    single { LocalTransactionRepository(get(), Dispatchers.IO) } bind TransactionRepository::class
    single { LocalLogRepository(get(), Dispatchers.IO) } bind LogRepository::class
    single { LocalBudgetRepository(get(), Dispatchers.IO) } bind BudgetRepository::class
    single { LocalDebtRepository(get(), Dispatchers.IO) } bind DebtRepository::class
    single { LocalSettingsRepository(get(), Dispatchers.IO) } bind SettingsRepository::class

    factoryOf(::LogEntry)
    factoryOf(::LogNothingSpent)
    factoryOf(::AddDebt)
    factoryOf(::RecordDebtPayment)
    factoryOf(::MarkReminded)
    factoryOf(::ObserveHomeSummary)

    viewModelOf(::HomeViewModel)
    viewModelOf(::SpendViewModel)
    viewModelOf(::DebtsViewModel)
    viewModelOf(::QuickLogViewModel)
    viewModelOf(::AddDebtViewModel)
    viewModelOf(::EditBudgetsViewModel)
    viewModelOf(::TransactionDetailViewModel)
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ChangeCurrencyViewModel)
}

/** Supplies the platform's `SqlDriver` for [LedgerlyDatabase]. */
expect val platformModule: Module
