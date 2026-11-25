import Observation
import Shared

/// Bridges the shared Kotlin `HomeViewModel` to SwiftUI. The Kotlin ViewModel holds the state
/// and the rules; this wrapper only mirrors `uiState` for the view and frees it when done.
@MainActor
@Observable
final class HomeModel {
    private(set) var state: HomeUiState

    @ObservationIgnored private let owner = ScreenViewModelOwner()
    @ObservationIgnored private let viewModel: HomeViewModel

    init() {
        viewModel = ViewModels.shared.home(owner: owner)
        state = viewModel.uiState.value
    }

    /// Call from `.task {}` so observation stops when the view disappears.
    func observe() async {
        for await next in viewModel.uiState {
            state = next
        }
    }

    deinit {
        owner.clear()
    }
}
