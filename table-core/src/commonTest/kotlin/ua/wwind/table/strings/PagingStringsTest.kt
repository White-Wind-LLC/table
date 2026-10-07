package ua.wwind.table.strings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PagingStringsTest {
    @Test
    fun `default strings cover the paging keys`() =
        runComposeUiTest {
            val resolved = mutableMapOf<UiString, String>()
            setContent {
                listOf(
                    UiString.PagingLoading,
                    UiString.PagingLoadError,
                    UiString.PagingLoadMoreError,
                    UiString.PagingRetry,
                ).forEach { resolved[it] = DefaultStrings.get(it) }
            }
            runOnIdle {
                assertThat(resolved).isEqualTo(
                    mapOf(
                        UiString.PagingLoading to "Loading",
                        UiString.PagingLoadError to "Couldn't load data",
                        UiString.PagingLoadMoreError to "Couldn't load some rows",
                        UiString.PagingRetry to "Retry",
                    ),
                )
            }
        }
}
