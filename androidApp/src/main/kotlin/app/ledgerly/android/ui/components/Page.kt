package app.ledgerly.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles

/**
 * Gives [content] its own ViewModels, cleared when it leaves the screen, so each time a page
 * opens it starts from a fresh form.
 */
@Composable
fun ScopedViewModels(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}

/**
 * A full-screen page over the tabs: a bar with a close or back button, a title and an optional
 * action, then a scrolling column. System back closes it.
 */
@Composable
fun Page(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeLabel: String = "Close",
    back: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = LedgerlyTheme.colors
    BackHandler(onBack = onClose)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.canvas)
            // Keeps taps from reaching the tab underneath.
            .pointerInput(Unit) {}
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LedgerlySpacing.space5, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(onClose, closeLabel, size = 44.dp) {
                LedgerlyIcon(if (back) LedgerlyIcons.Back else LedgerlyIcons.Close, size = 22.dp)
            }
            Text(
                title,
                style = LedgerlyTextStyles.heading,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { action?.invoke() }
        }
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LedgerlySpacing.space5,
                end = LedgerlySpacing.space5,
                top = 8.dp,
                bottom = bottom + 32.dp,
            ),
            content = content,
        )
    }
}

/** A form field: a label above, then whatever input it holds. */
@Composable
fun Field(label: String, modifier: Modifier = Modifier, optional: Boolean = false, content: @Composable () -> Unit) {
    val colors = LedgerlyTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.padding(start = 4.dp)) {
            Text(label, style = LedgerlyTextStyles.label, color = colors.ink)
            if (optional) Text(" · Optional", style = LedgerlyTextStyles.caption, color = colors.inkMuted, modifier = Modifier.align(Alignment.CenterVertically))
        }
        content()
    }
}

/** Ledgerly's text input. [amount] uses the big money style and a decimal keypad. */
@Composable
fun LedgerlyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    amount: Boolean = false,
    prefix: String? = null,
    textStyle: TextStyle = if (amount) LedgerlyTextStyles.amount else LedgerlyTextStyles.body,
) {
    val colors = LedgerlyTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = colors.ink),
        placeholder = { Text(placeholder, style = textStyle, color = colors.inkFaint) },
        prefix = prefix?.let { { Text("$it ", style = LedgerlyTextStyles.heading, color = colors.inkFaint) } },
        singleLine = true,
        keyboardOptions = if (amount) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        shape = RoundedCornerShape(LedgerlyRadius.radiusSm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.cobalt,
            unfocusedBorderColor = colors.line,
            cursorColor = colors.cobalt,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
        ),
    )
}

/** A small muted group title inside a page, e.g. "Per category". */
@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = LedgerlyTextStyles.label,
        color = LedgerlyTheme.colors.inkMuted,
        modifier = modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun Gap(height: androidx.compose.ui.unit.Dp) = Spacer(Modifier.size(height))
