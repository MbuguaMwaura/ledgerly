import Shared
import SwiftUI

struct HomeView: View {
    @State private var model = HomeModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: LedgerlySpacing.space3) {
                Text("Ledgerly")
                    .ledgerlyText(LedgerlyText.title)
                    .foregroundStyle(LedgerlyColor.ink)

                VStack(alignment: .leading, spacing: LedgerlySpacing.space2) {
                    Text("Left to spend")
                        .ledgerlyText(LedgerlyText.heading)
                        .foregroundStyle(LedgerlyColor.ink)
                    AmountText(money: model.state.leftToSpend, style: LedgerlyText.amountHero)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(LedgerlySpacing.space5)
                .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusLg))
            }
            .padding(LedgerlySpacing.space5)
        }
        .background(LedgerlyColor.canvas)
        .task { await model.observe() }
    }
}

#Preview {
    HomeView()
}
