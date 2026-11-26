import Shared
import SwiftUI

enum AppTab: Hashable, CaseIterable {
    case home, spend, debts

    var title: String {
        switch self {
        case .home: "Home"
        case .spend: "Spend"
        case .debts: "Debts"
        }
    }

    var symbol: String {
        switch self {
        case .home: "house"
        case .spend: "creditcard"
        case .debts: "person.2"
        }
    }
}

/// A page that opens over the tabs as a sheet.
enum AppSheet: Identifiable {
    case addDebt(DebtDirection)
    case budgets
    case entry(String)
    case settings

    var id: String {
        switch self {
        case .addDebt: "add-debt"
        case .budgets: "budgets"
        case .entry(let id): "entry-\(id)"
        case .settings: "settings"
        }
    }
}

/// The app shell: three tabs under a floating Liquid Glass tab bar with the + beside it, the
/// Expense / Income menu, the Quick log sheet and the confirmation toast.
struct RootView: View {
    let app: AppUiState

    @State private var tab: AppTab = .home
    @State private var home = HomeModel()
    @State private var quickLog = QuickLogModel()
    @State private var debts = DebtsModel()
    @State private var page: AppSheet?
    @State private var menuOpen = false
    @State private var logging = false
    @State private var toast: String?

    var body: some View {
        let shownTab: AppTab = tab == .debts && !app.trackDebts ? .home : tab
        ZStack(alignment: .bottom) {
            Group {
                switch shownTab {
                case .home:
                    HomeView(
                        model: home,
                        onLog: { openLog(.expense) },
                        onOpenSpend: { tab = .spend },
                        onOpenDebts: { tab = .debts },
                        onOpenEntry: { page = .entry($0) },
                        onEditBudgets: { page = .budgets },
                        onOpenSettings: { page = .settings },
                        firstName: app.firstName,
                        trackBudgets: app.trackBudgets,
                        trackDebts: app.trackDebts
                    )
                case .spend:
                    SpendView(onOpenEntry: { page = .entry($0) }, onEditBudgets: { page = .budgets }, trackBudgets: app.trackBudgets)
                case .debts:
                    DebtsView(model: debts, onAddDebt: { page = .addDebt($0) })
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if menuOpen {
                Color.black.opacity(0.18)
                    .ignoresSafeArea()
                    .onTapGesture { menuOpen = false }
                    .accessibilityHidden(true)
            }

            VStack(alignment: .trailing, spacing: 12) {
                if menuOpen {
                    GlassGroup(spacing: 8) {
                        VStack(alignment: .trailing, spacing: 8) {
                            LogMenuPill(title: "Expense", symbol: "arrow.up") { openLog(.expense) }
                            LogMenuPill(title: "Income", symbol: "arrow.down") { openLog(.income) }
                        }
                    }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                if let toast {
                    ToastView(text: toast)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                LedgerlyTabBar(
                    tabs: AppTab.allCases.filter { $0 != .debts || app.trackDebts },
                    selection: shownTab,
                    onSelect: { selected in
                        menuOpen = false
                        tab = selected
                    },
                    menuOpen: menuOpen,
                    onLog: { withAnimation(.snappy) { menuOpen.toggle() } },
                    nudge: !home.state.isLoading && !home.state.streak.loggedToday
                )
            }
            .padding(.horizontal, LedgerlySpacing.space5)
            .padding(.bottom, 4)
            .animation(.snappy, value: toast)
        }
        .background(LedgerlyColor.canvas.ignoresSafeArea())
        .tint(LedgerlyColor.cobalt)
        .task { await home.observe() }
        .task { await quickLog.observe() }
        .task(id: toast) {
            guard toast != nil else { return }
            try? await Task.sleep(for: .seconds(3))
            toast = nil
        }
        .sheet(isPresented: $logging) {
            QuickLogSheet(
                model: quickLog,
                showNothingSpent: !home.state.streak.loggedToday,
                onNothingSpent: {
                    home.viewModel.onNothingSpentToday()
                    logging = false
                    toast = "Nothing spent today. Your streak is safe."
                },
                onSaved: { message in
                    logging = false
                    toast = message
                },
                onClose: { logging = false }
            )
        }
        .sheet(item: $page) { sheet in
            switch sheet {
            case .addDebt(let direction):
                AddDebtView(
                    direction: direction,
                    onSaved: { message, saved in
                        debts.viewModel.onSelectDirection(value: saved)
                        tab = .debts
                        close(message)
                    },
                    onClose: { close() }
                )
            case .budgets:
                EditBudgetsView(onSaved: { close("Budgets saved.") }, onClose: { close() })
            case .entry(let id):
                TransactionDetailView(entryId: id, onDone: { close($0) }, onClose: { close() })
            case .settings:
                SettingsView(onEditBudgets: { page = .budgets }, onClose: { close() })
            }
        }
    }

    private func close(_ message: String? = nil) {
        page = nil
        if let message { toast = message }
    }

    private func openLog(_ kind: EntryKind) {
        menuOpen = false
        quickLog.viewModel.reset()
        quickLog.viewModel.onKindChange(kind: kind)
        logging = true
    }
}

/// The floating tab bar: a glass capsule of tabs and the round + tinted with cobalt.
struct LedgerlyTabBar: View {
    let tabs: [AppTab]
    let selection: AppTab
    let onSelect: (AppTab) -> Void
    let menuOpen: Bool
    let onLog: () -> Void
    let nudge: Bool

    var body: some View {
        GlassGroup(spacing: 12) {
            HStack(spacing: 12) {
                HStack(spacing: 2) {
                    ForEach(tabs, id: \.self) { item in
                        let on = item == selection
                        Button { onSelect(item) } label: {
                            VStack(spacing: 2) {
                                Image(systemName: on ? "\(item.symbol).fill" : item.symbol)
                                    .font(.system(size: 19, weight: .medium))
                                Text(item.title).font(.system(size: 10, weight: .semibold))
                            }
                            .foregroundStyle(on ? LedgerlyColor.cobalt : LedgerlyColor.ink)
                            .frame(minWidth: 74, minHeight: 54)
                            .background(on ? Color.gray.opacity(0.16) : .clear, in: Capsule())
                            .contentShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(on ? .isSelected : [])
                    }
                }
                .padding(5)
                .glassControl(in: Capsule())

                Spacer(minLength: 0)

                Button(action: onLog) {
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .semibold))
                        .rotationEffect(.degrees(menuOpen ? 45 : 0))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)
                .glassControl(in: Circle(), tint: LedgerlyColor.cobalt.opacity(0.85))
                .overlay(alignment: .topTrailing) {
                    if nudge && !menuOpen {
                        Circle()
                            .fill(LedgerlyColor.loss)
                            .frame(width: 14, height: 14)
                            .overlay(Circle().stroke(.white.opacity(0.85), lineWidth: 3))
                            .offset(x: -3, y: 3)
                            .accessibilityHidden(true)
                    }
                }
                .accessibilityLabel(menuOpen ? "Close" : "Log money")
                .accessibilityHint(nudge ? "Nothing logged today" : "")
            }
        }
    }
}

private struct LogMenuPill: View {
    let title: String
    let symbol: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .font(.system(size: 14, weight: .semibold))
                    .frame(width: 32, height: 32)
                    .background(LedgerlyColor.surfaceSunk.opacity(0.7), in: Circle())
                Text(title).font(.system(size: 16, weight: .medium))
            }
            .foregroundStyle(LedgerlyColor.ink)
            .padding(.leading, 8)
            .padding(.trailing, 18)
            .padding(.vertical, 8)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .glassControl(in: Capsule())
    }
}

/// The confirmation after a save: a glass capsule on iOS 26.
struct ToastView: View {
    let text: String

    var body: some View {
        Text(text)
            .ledgerlyText(LedgerlyText.label)
            .foregroundStyle(LedgerlyColor.ink)
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassControl(in: Capsule(), interactive: false)
            .accessibilityAddTraits(.updatesFrequently)
    }
}
