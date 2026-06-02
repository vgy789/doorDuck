package io.github.vgy789.doorDuck.ui

enum class WizardStep {
    CREDENTIALS,
    DONE,
}

object WizardStateMachine {
    fun next(step: WizardStep): WizardStep {
        return when (step) {
            WizardStep.CREDENTIALS -> WizardStep.DONE
            WizardStep.DONE -> WizardStep.DONE
        }
    }

    fun previous(step: WizardStep): WizardStep {
        return when (step) {
            WizardStep.CREDENTIALS -> WizardStep.CREDENTIALS
            WizardStep.DONE -> WizardStep.CREDENTIALS
        }
    }

    fun canProceed(
        step: WizardStep,
        userId: String,
        token: String,
        connectionCheckPassed: Boolean,
    ): Boolean {
        return when (step) {
            WizardStep.CREDENTIALS -> userId.isNotBlank() && token.isNotBlank()
            WizardStep.DONE -> true
        }
    }
}
