import Observation
import Shared

// Each model bridges one shared Kotlin ViewModel to SwiftUI. The Kotlin ViewModel holds the state
// and the rules; the model mirrors `uiState` for the view and frees the ViewModel when it goes away.
// Views call `observe()` from `.task {}`, so collection stops with the view.

@MainActor
@Observable
final class HomeModel {
    private(set) var state: HomeUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: HomeViewModel

    init() {
        viewModel = ViewModels.shared.home(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class SpendModel {
    private(set) var state: SpendUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: SpendViewModel

    init() {
        viewModel = ViewModels.shared.spend(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class DebtsModel {
    private(set) var state: DebtsUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: DebtsViewModel

    init() {
        viewModel = ViewModels.shared.debts(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class QuickLogModel {
    private(set) var state: QuickLogUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: QuickLogViewModel

    init() {
        viewModel = ViewModels.shared.quickLog(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class AddDebtModel {
    private(set) var state: AddDebtUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: AddDebtViewModel

    init(direction: DebtDirection) {
        viewModel = ViewModels.shared.addDebt(owner: owner)
        viewModel.onDirectionChange(direction: direction)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class EditBudgetsModel {
    private(set) var state: EditBudgetsUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: EditBudgetsViewModel

    init() {
        viewModel = ViewModels.shared.editBudgets(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class TransactionDetailModel {
    private(set) var state: TransactionDetailUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: TransactionDetailViewModel

    init(entryId: String) {
        viewModel = ViewModels.shared.transactionDetail(owner: owner)
        viewModel.load(id: entryId)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class AppModel {
    private(set) var state: AppUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: AppViewModel

    init() {
        viewModel = ViewModels.shared.app(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class OnboardingModel {
    private(set) var state: OnboardingUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: OnboardingViewModel

    init() {
        viewModel = ViewModels.shared.onboarding(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class SettingsModel {
    private(set) var state: SettingsUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: SettingsViewModel

    init() {
        viewModel = ViewModels.shared.settings(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}

@MainActor
@Observable
final class ChangeCurrencyModel {
    private(set) var state: ChangeCurrencyUiState
    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored let viewModel: ChangeCurrencyViewModel

    init() {
        viewModel = ViewModels.shared.changeCurrency(owner: owner)
        state = viewModel.uiState.value
    }

    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}
