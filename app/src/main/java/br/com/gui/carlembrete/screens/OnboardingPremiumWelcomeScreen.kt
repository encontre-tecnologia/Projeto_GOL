package br.com.gui.carlembrete

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
fun OnboardingPremiumWelcomeScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit = onNext
) {
    BackHandler(onBack = onSkip)

    PremiumBeneficiosScreen(
        onDismiss = {},
        onSubscribeNow = {},
        showBackButton = false,
        showSubscribeButton = false,
        onPlanSelected = { onNext() }
    )
}

