package app.ledgerly.android

import android.app.Application
import app.ledgerly.android.reminders.Reminders
import app.ledgerly.shared.di.platformModule
import app.ledgerly.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LedgerlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LedgerlyApplication)
            modules(sharedModule, platformModule)
        }
        Reminders.createChannel(this)
    }
}
