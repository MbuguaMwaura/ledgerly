package app.ledgerly.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.ledgerly.shared.db.LedgerlyDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(LedgerlyDatabase.Schema, "ledgerly.db") }
}
