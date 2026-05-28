package com.example.mahjongcoach

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.LargeTest
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RealPaipuImportE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun importsPublicMahjongSoulPaipuThroughRealNetwork() {
        assumeTrue(
            "Set instrumentation argument runRealNetworkE2E=true to run real network E2E tests.",
            InstrumentationRegistry.getArguments().getString("runRealNetworkE2E") == "true",
        )

        composeRule.onNodeWithTag(MahjongCoachTestTags.LinkInput)
            .performTextInput(REAL_PAIPU_URL)
        composeRule.onNodeWithTag(MahjongCoachTestTags.ImportButton)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithTag(MahjongCoachTestTags.ReviewScreen)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeRule.onAllNodesWithTag(MahjongCoachTestTags.ImportResult)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }

        composeRule.onNodeWithTag(MahjongCoachTestTags.ReviewScreen)
            .assertIsDisplayed()
        composeRule.onNodeWithText("本局结论")
            .assertIsDisplayed()
        composeRule.onNodeWithText("关键决策点")
            .assertIsDisplayed()
    }

    private companion object {
        const val REAL_PAIPU_URL =
            "https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483"
    }
}
