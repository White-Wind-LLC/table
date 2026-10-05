package ua.wwind.table.icon

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import kotlin.test.Test

class TableIconsTest {
    @Test
    fun `column menu icons are 24dp vectors with path data`() {
        listOf(
            TableIcons.PushPin,
            TableIcons.PushPinOutlined,
            TableIcons.Visibility,
            TableIcons.VisibilityOff,
            TableIcons.SettingsEthernet,
            TableIcons.SettingsBackupRestore,
            TableIcons.TableRows,
            TableIcons.MoreVert,
        ).forEach { icon ->
            assertThat(icon.viewportWidth).isEqualTo(24f)
            assertThat(icon.root.size).isGreaterThan(0)
        }
    }
}
