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
