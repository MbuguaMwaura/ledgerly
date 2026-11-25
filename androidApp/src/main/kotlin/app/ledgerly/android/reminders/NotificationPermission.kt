package app.ledgerly.android.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a function that asks for notification permission when the phone needs it (Android 13+)
 * and reports whether reminders can show.
 */
@Composable
fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { latest.value(it) }
    return remember(context, launcher) {
        {
            val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted) latest.value(true) else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** Keeps the phone's alarms in step with the reminder times the shared app state works out. */
@Composable
fun ReminderSync(times: List<Long>, message: String) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Reminders.clearShown(context) }
    LaunchedEffect(times, message) { Reminders.schedule(context, times, message) }
}
