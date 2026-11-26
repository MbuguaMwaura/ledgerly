package app.ledgerly.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.ledgerly.android.ui.LedgerlyApp

/** A FragmentActivity so the fingerprint and face prompt can attach to it. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LedgerlyApp(versionName = BuildConfig.VERSION_NAME)
        }
    }
}
