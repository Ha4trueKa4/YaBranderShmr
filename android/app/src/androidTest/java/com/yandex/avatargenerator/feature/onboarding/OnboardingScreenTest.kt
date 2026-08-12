package com.yandex.avatargenerator.feature.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingPagerTestTag
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingScreen
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingUiState
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipingToTheLastPageAndTappingGetStartedTriggersOnFinish() {
        var finished = false
        var lastReportedPage = -1

        composeRule.setContent {
            AvatarGeneratorTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(),
                    onPageChanged = { page -> lastReportedPage = page },
                    onGetStartedClick = { finished = true },
                    onSignInClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Brandkit AI").assertExists()

        repeat(2) {
            composeRule.onNodeWithTag(OnboardingPagerTestTag).performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }

        assertEquals(2, lastReportedPage)

        composeRule.onNodeWithText("Get Started").performClick()

        assertTrue(finished)
    }

    @Test
    fun signInLinkTriggersOnSignIn() {
        var signInRequested = false

        composeRule.setContent {
            AvatarGeneratorTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(currentPage = 2),
                    onPageChanged = {},
                    onGetStartedClick = {},
                    onSignInClick = { signInRequested = true },
                )
            }
        }

        composeRule.onNodeWithText("Already have an account? Sign in").performClick()

        assertTrue(signInRequested)
    }
}
