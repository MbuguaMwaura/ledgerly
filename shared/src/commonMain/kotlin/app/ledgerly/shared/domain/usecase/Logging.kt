package app.ledgerly.shared.domain.usecase

import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.model.Transaction
import app.ledgerly.shared.domain.model.newId
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.domain.time.logicalDate
import app.ledgerly.shared.money.Money
import kotlinx.datetime.Instant

/** Amount → category → save. Returns the saved entry. */
class LogEntry(
    private val transactions: TransactionRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        kind: EntryKind,
        amount: Money,
        categoryId: String,
        note: String? = null,
        occurredAt: Instant = clock.now(),
    ): Transaction {
        require(amount.minorUnits > 0) { "Amount must be positive; kind gives the direction" }
        val entry = Transaction(
            id = newId(),
            kind = kind,
            amount = amount,
            categoryId = categoryId,
            note = note?.trim()?.ifEmpty { null },
            occurredAt = occurredAt,
            logicalDate = logicalDate(occurredAt, clock.timeZone()),
        )
        transactions.save(entry)
        return entry
    }
}

/** "Nothing spent today" counts for the streak. */
class LogNothingSpent(
    private val logs: LogRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke() {
        logs.markNothingSpent(clock.today(), clock.now())
    }
}
