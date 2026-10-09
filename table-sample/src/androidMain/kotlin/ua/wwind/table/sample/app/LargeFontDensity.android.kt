package ua.wwind.table.sample.app

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density

// Density(context) applies Android's non-linear font scale converter; Density(density, fontScale) is linear.
@Composable
internal actual fun largeFontDensity(): Density {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) {
        val scaled = Configuration(configuration).apply { fontScale = LARGE_FONT_SCALE }
        Density(context.createConfigurationContext(scaled))
    }
}
