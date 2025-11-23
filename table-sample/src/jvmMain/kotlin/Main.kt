import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ua.wwind.table.sample.app.SampleApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Table Sample") {
        MaterialTheme { Surface(tonalElevation = 0.dp) { SampleApp() } }
    }
}
