package app.ledgerly.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import app.ledgerly.android.R
import app.ledgerly.android.ui.home.HomeRoute
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_tab_home),
    Spend("Spend", R.drawable.ic_tab_spend),
    Debts("Debts", R.drawable.ic_tab_debts),
}

/**
 * The three tabs. This is a placeholder Material bar; the floating pill TabBar and the + LogButton
 * from the design system replace it in the Android UI phase.
 */
@Composable
fun LedgerlyRoot() {
    var selected by rememberSaveable { mutableStateOf(Tab.Home) }
    Scaffold(
        containerColor = LedgerlyTheme.colors.canvas,
        bottomBar = {
            NavigationBar(containerColor = LedgerlyTheme.colors.surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selected = tab },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selected) {
            Tab.Home -> HomeRoute(modifier)
            Tab.Spend -> Placeholder("Spend", modifier)
            Tab.Debts -> Placeholder("Debts", modifier)
        }
    }
}

@Composable
private fun Placeholder(title: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = LedgerlyTextStyles.heading, color = LedgerlyTheme.colors.inkMuted)
    }
}
