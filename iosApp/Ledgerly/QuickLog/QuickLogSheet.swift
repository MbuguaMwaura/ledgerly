import Shared
import SwiftUI

/// Amount → category → save. On iOS 26 the sheet itself is Liquid Glass; the keypad stays solid
/// so the numbers read clearly. When the shared ViewModel reports the save, the sheet hands its
/// message to `onSaved` and closes.
struct QuickLogSheet: View {
    let model: QuickLogModel
    let showNothingSpent: Bool
    let onNothingSpent: () -> Void
    let onSaved: (String) -> Void
    let onClose: () -> Void

    private static let keys: [KeypadKey] = {
        var keys: [KeypadKey] = (1...9).map { KeypadKeyDigit(value: Int32($0)) }
        keys.append(KeypadKeyDecimal.shared)
        keys.append(KeypadKeyDigit(value: 0))
        keys.append(KeypadKeyBackspace.shared)
        return keys
    }()

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel
        let empty = state.amountText.isEmpty

        VStack(spacing: LedgerlySpacing.space3) {
            HStack {
                Text(state.kind == .expense ? "Log expense" : "Log income")
                    .ledgerlyText(LedgerlyText.heading)
                    .foregroundStyle(LedgerlyColor.ink)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                closeButton
            }

            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(state.amount.currency.code)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(LedgerlyColor.inkFaint)
                Text(empty ? "0" : state.amountText)
                    .font(.system(size: 44, weight: .medium))
                    .tracking(-1.7)
                    .monospacedDigit()
                    .foregroundStyle(empty ? LedgerlyColor.inkFaint : LedgerlyColor.ink)
                    .contentTransition(.numericText())
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, LedgerlySpacing.space2)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(empty ? "No amount yet" : state.amount.label(fraction: true))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: LedgerlySpacing.space2) {
                    ForEach(state.categories, id: \.id) { category in
                        ChipButton(title: category.name, selected: category.id == state.selectedCategoryId) {
                            viewModel.onSelectCategory(id: category.id)
                        }
                    }
                }
                .padding(.vertical, 2)
            }

            TextField(
                "Add a note (optional)",
                text: Binding(get: { model.state.note }, set: { viewModel.onNoteChange(note: $0) })
            )
            .ledgerlyText(LedgerlyText.body)
            .padding(.horizontal, 16)
            .frame(minHeight: 48)
            .background(LedgerlyColor.surfaceSunk, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous))

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: LedgerlySpacing.space2), count: 3), spacing: LedgerlySpacing.space2) {
                ForEach(Array(Self.keys.enumerated()), id: \.offset) { _, key in
                    KeyButton(key: key) { viewModel.onKey(key: key) }
                }
            }
            .padding(.vertical, LedgerlySpacing.space1)

            Button(state.isSaving ? "Saving…" : state.canSave ? "Save \(state.amount.label())" : "Enter an amount and category") {
                viewModel.onSave()
            }
            .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
            .disabled(!state.canSave)

            if showNothingSpent && state.kind == .expense {
                Button("Nothing spent today", action: onNothingSpent)
                    .buttonStyle(PillButtonStyle(kind: .quiet, fill: true))
            }
        }
        .padding(.horizontal, LedgerlySpacing.space5)
        .padding(.top, LedgerlySpacing.space5)
        .padding(.bottom, LedgerlySpacing.space2)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .sensoryFeedback(.success, trigger: state.savedMessage) { _, new in new != nil }
        .onChange(of: state.savedMessage) { _, message in
            guard let message else { return }
            onSaved(message)
            viewModel.reset()
        }
    }

    @ViewBuilder
    private var closeButton: some View {
        if #available(iOS 26.0, *) {
            Button(action: onClose) {
                Image(systemName: "xmark").font(.system(size: 15, weight: .semibold)).frame(width: 28, height: 28)
            }
            .buttonStyle(.glass)
            .buttonBorderShape(.circle)
            .accessibilityLabel("Close")
        } else {
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(LedgerlyColor.ink)
                    .frame(width: 44, height: 44)
                    .background(LedgerlyColor.surfaceSunk, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close")
        }
    }
}

private struct KeyButton: View {
    let key: KeypadKey
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                switch onEnum(of: key) {
                case .backspace:
                    Image(systemName: "delete.left").font(.system(size: 20, weight: .medium))
                case .decimal:
                    Text(".").font(.system(size: 22, weight: .medium))
                case .digit(let digit):
                    Text("\(digit.value)").font(.system(size: 22, weight: .medium)).monospacedDigit()
                }
            }
            .foregroundStyle(LedgerlyColor.ink)
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(LedgerlyColor.surfaceSunk, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private var label: String {
        switch onEnum(of: key) {
        case .backspace: "Delete"
        case .decimal: "Decimal point"
        case .digit(let digit): "\(digit.value)"
        }
    }
}
