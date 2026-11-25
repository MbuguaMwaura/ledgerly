package app.ledgerly.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.Amount
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.presentation.home.HomeUiState
import app.ledgerly.shared.presentation.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, modifier)
}

@Composable
fun HomeScreen(state: HomeUiState, modifier: Modifier = Modifier) {
    val colors = LedgerlyTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(LedgerlySpacing.space5),
        verticalArrangement = Arrangement.spacedBy(LedgerlySpacing.space3),
    ) {
        Text("Ledgerly", style = LedgerlyTextStyles.title, color = colors.ink)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(LedgerlyRadius.radiusLg))
                .padding(LedgerlySpacing.space5),
            verticalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2),
        ) {
            Text("Left to spend", style = LedgerlyTextStyles.heading, color = colors.ink)
            Amount(state.leftToSpend, style = LedgerlyTextStyles.amountHero)
        }
    }
}
