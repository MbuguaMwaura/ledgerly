package app.ledgerly.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.ledgerly.shared.db.LedgerlyDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Needs `androidContext()` in the app's `startKoin`. */
actual val platformModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(LedgerlyDatabase.Schema, get(), "ledgerly.db") }
}
