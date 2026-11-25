package app.ledgerly.shared.di

import app.ledgerly.shared.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Everything the shared module provides. Each app adds its own platform module next to it. */
val sharedModule: Module = module {
    viewModelOf(::HomeViewModel)
}
