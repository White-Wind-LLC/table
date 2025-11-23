package ua.wwind.table.sample.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Simple app theme with light and dark color schemes. Uses Material3 defaults to keep the sample
 * minimal and portable across targets.
 */
@Composable
fun SampleTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
