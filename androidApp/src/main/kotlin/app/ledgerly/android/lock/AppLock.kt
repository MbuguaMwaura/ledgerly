package app.ledgerly.android.lock

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first

/** Fingerprint, face, or the phone's PIN, pattern or password: whatever the phone has set up. */
private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * True while the system prompt is up. Confirming with the phone's PIN opens a separate screen,
 * which stops this activity; that mustn't count as the user leaving the app.
 */
private var authenticating = false

/** Whether the phone has a screen lock or biometrics that app lock can use. */
@Composable
fun rememberCanUseAppLock(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

/** Returns a function that shows the system prompt with a title and reports whether it passed. */
@Composable
fun rememberAuthenticator(): (title: String, onResult: (Boolean) -> Unit) -> Unit {
    val activity = LocalContext.current.findActivity()
    return remember(activity) {
        { title, onResult -> authenticate(activity, title, onResult) }
    }
}

private fun authenticate(activity: FragmentActivity, title: String, onResult: (Boolean) -> Unit) {
    if (authenticating) return
    authenticating = true
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authenticating = false
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authenticating = false
                onResult(false)
            }
            // onAuthenticationFailed is a single bad try; the prompt stays up for another.
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setAllowedAuthenticators(AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}

/**
 * While app lock is on: keeps Ledgerly's screen out of the recent-apps preview, and reports when
 * the app leaves the screen so it locks. A rotation doesn't count.
 */
@Composable
fun AppLockEffects(appLock: Boolean, onHidden: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnHidden = rememberUpdatedState(onHidden)

    DisposableEffect(activity, appLock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(!appLock)
        } else if (appLock) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {}
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !activity.isChangingConfigurations && !authenticating) {
                latestOnHidden.value()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Each time the app becomes locked, opens the prompt once the app is on screen again. Waiting for
 * that matters: the app locks while it's in the background, where the prompt can't show.
 */
@Composable
fun PromptWhenLocked(isLocked: Boolean, prompt: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestPrompt = rememberUpdatedState(prompt)
    LaunchedEffect(isLocked, lifecycle) {
        if (!isLocked) return@LaunchedEffect
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        latestPrompt.value()
    }
}

private tailrec fun Context.findActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("App lock needs a FragmentActivity")
}
