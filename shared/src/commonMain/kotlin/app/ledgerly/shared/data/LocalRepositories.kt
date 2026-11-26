package app.ledgerly.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.ledgerly.shared.db.LedgerlyDatabase
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.Category
import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.DebtPayment
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.domain.model.Transaction
import app.ledgerly.shared.domain.model.UserSettings
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.repository.DebtRepository
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.repository.SettingsRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

import kotlinx.coroutines.flow.map
import app.ledgerly.shared.money.CurrencyCode
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import app.ledgerly.shared.db.Debt as DebtRow
import app.ledgerly.shared.db.Debt_payment as PaymentRow
import app.ledgerly.shared.db.Entry as EntryRow

class LocalCategoryRepository(
    private val db: LedgerlyDatabase,
    private val io: CoroutineDispatcher,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> =
        db.categoryQueries.selectAll { id, name, icon, kind, _, _ -> Category(id, name, icon, EntryKind.valueOf(kind)) }
            .asFlow().mapToList(io)
}

class LocalTransactionRepository(
    private val db: LedgerlyDatabase,
    private val io: CoroutineDispatcher,
) : TransactionRepository {
    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        db.entryQueries.selectBetween(from.toString(), to.toString()).asFlow().mapToList(io).map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: String): Transaction? = withContext(io) {
        db.entryQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun save(transaction: Transaction) = withContext<Unit>(io) {
        with(transaction) {
            db.entryQueries.upsert(
                id, kind.name, amount.minorUnits, amount.currency.name, categoryId, note,
                occurredAt.toDb(), logicalDate.toString(), occurredAt.toDb(),
            )
        }
    }

    override suspend fun delete(id: String) = withContext<Unit>(io) { db.entryQueries.deleteById(id) }

    private fun EntryRow.toDomain() = Transaction(
        id = id,
        kind = EntryKind.valueOf(kind),
        amount = money(amount_minor, currency),
        categoryId = category_id,
        note = note,
        occurredAt = occurred_at.toInstant(),
        logicalDate = LocalDate.parse(logical_date),
    )
}

class LocalLogRepository(
    private val db: LedgerlyDatabase,
    private val io: CoroutineDispatcher,
) : LogRepository {
    override fun observeLoggedDays(): Flow<Set<LocalDate>> =
        db.entryQueries.loggedDays().asFlow().mapToList(io).map { days -> days.map(LocalDate::parse).toSet() }

    override suspend fun markNothingSpent(date: LocalDate, at: Instant) = withContext<Unit>(io) {
        db.entryQueries.markNothingSpent(date.toString(), at.toDb())
    }
}

class LocalBudgetRepository(
    private val db: LedgerlyDatabase,
    private val io: CoroutineDispatcher,
) : BudgetRepository {
    override fun observeMonth(month: BudgetMonth): Flow<List<Budget>> =
        db.budgetQueries.selectEffective(month.toDb()) { categoryId, _, limitMinor, currency, _ ->
            Budget(categoryId, month, money(limitMinor, currency))
        }.asFlow().mapToList(io)

    override suspend fun save(budget: Budget, at: Instant) = withContext<Unit>(io) {
        db.budgetQueries.upsert(
            budget.categoryId, budget.month.toDb(), budget.limit.minorUnits, budget.limit.currency.name, at.toDb(),
        )
    }

    override suspend fun delete(categoryId: String, month: BudgetMonth) = withContext<Unit>(io) {
        db.budgetQueries.delete(categoryId, month.toDb())
    }
}

class LocalDebtRepository(
    private val db: LedgerlyDatabase,
    private val io: CoroutineDispatcher,
) : DebtRepository {
    override fun observeAll(): Flow<List<Debt>> = combine(
        db.debtQueries.selectAll().asFlow().mapToList(io),
        db.debtQueries.selectAllPayments().asFlow().mapToList(io),
    ) { debts, payments ->
        val byDebt = payments.groupBy { it.debt_id }
        debts.map { it.toDomain(byDebt[it.id].orEmpty()) }
    }

    override suspend fun save(debt: Debt, at: Instant) = withContext<Unit>(io) {
        with(debt) {
            db.debtQueries.upsert(
                id, direction.name, counterparty, principal.minorUnits, principal.currency.name,
                dueDate?.toString(), note, createdAt.toDb(), lastRemindedAt?.toDb(), at.toDb(),
            )
        }
    }

    override suspend fun addPayment(payment: DebtPayment) = withContext<Unit>(io) {
        with(payment) {
            db.debtQueries.insertPayment(id, debtId, amount.minorUnits, amount.currency.name, paidAt.toDb(), paidAt.toDb())
        }
    }

    override suspend fun markReminded(debtId: String, at: Instant) = withContext<Unit>(io) {
        db.debtQueries.markReminded(remindedAt = at.toDb(), id = debtId)
    }

    override suspend fun delete(debtId: String) = withContext<Unit>(io) {
        db.transaction {
            db.debtQueries.deletePaymentsFor(debtId)
            db.debtQueries.deleteById(debtId)
        }
    }

    private fun DebtRow.toDomain(payments: List<PaymentRow>) = Debt(
        id = id,
        direction = DebtDirection.valueOf(direction),
        counterparty = counterparty,
        principal = money(principal_minor, currency),
        dueDate = due_date?.let(LocalDate::parse),
        note = note,
        createdAt = created_at.toInstant(),
        lastRemindedAt = last_reminded_at?.toInstant(),
        payments = payments.map { DebtPayment(it.id, it.debt_id, money(it.amount_minor, it.currency), it.paid_at.toInstant()) },
    )
}

class LocalSettingsRepository(
    private val db: LedgerlyDatabase,
    private val io: CoroutineDispatcher,
) : SettingsRepository {
    override fun observe(): Flow<UserSettings> =
        db.settingQueries.selectAll().asFlow().mapToList(io)
            .map { rows -> rows.associate { it.name to it.content }.toSettings() }
            .distinctUntilChanged()

    override suspend fun update(change: (UserSettings) -> UserSettings) = withContext<Unit>(io) {
        db.transaction {
            val current = db.settingQueries.selectAll().executeAsList().associate { it.name to it.content }.toSettings()
            change(current).toMap().forEach { (key, value) -> db.settingQueries.upsert(key, value) }
        }
    }

    private fun Map<String, String>.toSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            name = this[NAME] ?: defaults.name,
            currency = this[CURRENCY]?.let { code -> CurrencyCode.entries.firstOrNull { it.name == code } } ?: defaults.currency,
            theme = this[THEME]?.let { t -> ThemeChoice.entries.firstOrNull { it.name == t } } ?: defaults.theme,
            trackBudgets = this[TRACK_BUDGETS]?.toBooleanStrictOrNull() ?: defaults.trackBudgets,
            trackDebts = this[TRACK_DEBTS]?.toBooleanStrictOrNull() ?: defaults.trackDebts,
            reminderOn = this[REMINDER_ON]?.toBooleanStrictOrNull() ?: defaults.reminderOn,
            appLock = this[APP_LOCK]?.toBooleanStrictOrNull() ?: defaults.appLock,
            reminderMinutes = this[REMINDER_MINUTES]?.toIntOrNull()?.takeIf { it in 0 until 24 * 60 } ?: defaults.reminderMinutes,
            startedOn = this[STARTED_ON]?.takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
        )
    }

    private fun UserSettings.toMap(): Map<String, String> = mapOf(
        NAME to name,
        CURRENCY to currency.name,
        THEME to theme.name,
        TRACK_BUDGETS to trackBudgets.toString(),
        TRACK_DEBTS to trackDebts.toString(),
        REMINDER_ON to reminderOn.toString(),
        REMINDER_MINUTES to reminderMinutes.toString(),
        APP_LOCK to appLock.toString(),
        STARTED_ON to (startedOn?.toString() ?: ""),
    )

    private companion object {
        const val NAME = "name"
        const val CURRENCY = "currency"
        const val THEME = "theme"
        const val TRACK_BUDGETS = "track_budgets"
        const val TRACK_DEBTS = "track_debts"
        const val REMINDER_ON = "reminder_on"
        const val REMINDER_MINUTES = "reminder_minutes"
        const val APP_LOCK = "app_lock"
        const val STARTED_ON = "started_on"
    }
}
