package com.example.mahjongcoach

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.mjai.MjaiAssessment
import com.example.mahjongcoach.mjai.MjaiAssessmentStatus
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class HistoryPaipuAiStatusE2ETest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        assumeTrue(
            "Set instrumentation argument runRealNetworkE2E=true to run real network E2E tests.",
            InstrumentationRegistry.getArguments().getString("runRealNetworkE2E") == "true",
        )
        context.getSharedPreferences("paipu_history", Context.MODE_PRIVATE).edit().clear().commit()
        ReviewDependencyOverrides.aiReviewProvider = object : ReviewAiProvider {
            override suspend fun assess(paipu: FinalPaipu, report: EvaluationReport): List<MjaiAssessment> {
                return report.decisionPoints.map { decision ->
                    MjaiAssessment(
                        decisionId = decision.aiDecisionId,
                        recommendedDiscard = null,
                        recommendedWeight = null,
                        chosenWeight = null,
                        model = "mini",
                        status = MjaiAssessmentStatus.TrialUnavailable,
                    )
                }
            }
        }
    }

    @After
    fun tearDown() {
        ReviewDependencyOverrides.reset()
        context.getSharedPreferences("paipu_history", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun openingImportedHistoryPaipuDisplaysLocalizedAiStatusInsteadOfInternalSchemaValue() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithTag(MahjongCoachTestTags.LinkInput)
                .performTextInput(REAL_PAIPU_URL)
            composeRule.onNodeWithTag(MahjongCoachTestTags.ImportButton)
                .performClick()

            waitForReviewScreen()
            assertLocalizedTrialStatusIsShown()

            composeRule.onNodeWithTag(MahjongCoachTestTags.ReviewScreen)
                .performScrollToNode(hasText("返回链接"))
            composeRule.onNodeWithText("返回链接")
                .performClick()
            composeRule.onNodeWithText("历史记录")
                .assertIsDisplayed()
            composeRule.onNodeWithTag(MahjongCoachTestTags.historyItem(REAL_PAIPU_UUID))
                .performClick()

            waitForReviewScreen()
            assertLocalizedTrialStatusIsShown()
        }
    }

    private fun waitForReviewScreen() {
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText("本局结论")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(MahjongCoachTestTags.ReviewScreen)
            .assertIsDisplayed()
    }

    private fun assertLocalizedTrialStatusIsShown() {
        composeRule.onNodeWithTag(MahjongCoachTestTags.ReviewScreen)
            .performScrollToNode(hasText("MJAI 试用暂不可用"))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("MJAI 试用暂不可用")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 1_000) {
            composeRule.onAllNodesWithText("trial_unavailable")
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    private companion object {
        const val REAL_PAIPU_UUID = "260522-2793b6c3-992e-424b-9205-11a121e9ac00"
        const val REAL_PAIPU_URL =
            "https://game.maj-soul.com/1/?paipu=${REAL_PAIPU_UUID}_a64445483"
    }
}
