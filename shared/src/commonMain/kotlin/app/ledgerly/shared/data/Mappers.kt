package app.ledgerly.shared.data

import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import kotlinx.datetime.Instant

internal fun money(minor: Long, currency: String): Money = Money(minor, CurrencyCode.valueOf(currency))

internal fun Instant.toDb(): Long = toEpochMilliseconds()

internal fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun BudgetMonth.toDb(): String = "$year-${month.toString().padStart(2, '0')}"
