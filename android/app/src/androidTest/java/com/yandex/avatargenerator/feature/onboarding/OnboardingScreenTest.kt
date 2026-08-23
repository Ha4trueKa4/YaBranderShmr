package com.yandex.avatargenerator.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingContent
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingIntent
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingPagerTestTag
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingState
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `OnboardingContent` сам состояние не хранит, поэтому тест играет за ViewModel:
 * складывает пришедшие интенты и повторяет её редьюсер.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<OnboardingIntent>()

    private fun setContent() {
        composeRule.setContent {
            var state by remember { mutableStateOf(OnboardingState()) }
            AvatarGeneratorTheme {
                OnboardingContent(
                    state = state,
                    onIntent = { intent ->
                        intents += intent
                        state = when (intent) {
                            is OnboardingIntent.OnPageChange -> state.copy(currentPage = intent.index)
                            OnboardingIntent.OnPrimaryClick -> if (state.isLastPage) {
                                state
                            } else {
                                state.copy(currentPage = state.currentPage + 1)
                            }

                            OnboardingIntent.OnBackClick -> state.copy(
                                currentPage = (state.currentPage - 1).coerceAtLeast(0)
                            )
                        }
                    },
                )
            }
        }
    }

    @Test
    fun swipingThroughAllSlidesReachesTheLastOneAndTappingGetStartedSendsPrimaryClick() {
        setContent()

        // Ищем именно заголовок слайда: «Ya Brander» нарисован внутри иллюстрации,
        // а она вычищена из дерева доступности и в поиск по тексту не попадает.
        composeRule.onNodeWithText("Turn words into").assertExists()

        repeat(3) {
            composeRule.onNodeWithTag(OnboardingPagerTestTag).performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }

        assertEquals(3, intents.filterIsInstance<OnboardingIntent.OnPageChange>().last().index)

        composeRule.onNodeWithText("Get Started").performClick()

        assertTrue(intents.contains(OnboardingIntent.OnPrimaryClick))
    }

    @Test
    fun theNextButtonPagesToTheLastSlideWhereItsLabelChanges() {
        setContent()

        repeat(3) {
            composeRule.onNodeWithText("Next").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText("Next").assertDoesNotExist()
        composeRule.onNodeWithText("Get Started").assertExists()
    }
}
