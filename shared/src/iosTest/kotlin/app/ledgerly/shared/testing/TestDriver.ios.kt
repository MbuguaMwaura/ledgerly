package app.ledgerly.shared.testing

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.ledgerly.shared.db.LedgerlyDatabase

actual fun createTestDriver(): SqlDriver = inMemoryDriver(LedgerlyDatabase.Schema)
