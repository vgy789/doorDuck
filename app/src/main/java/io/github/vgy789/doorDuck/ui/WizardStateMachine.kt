package io.github.vgy789.doorDuck.ui

enum class WizardStep {
    WELCOME,
    USER_ID,
    TOKEN,
    CHECK_CONNECTION,
    DONE,
}

object WizardStateMachine {
    fun next(step: WizardStep): WizardStep {
        return when (step) {
            WizardStep.WELCOME -> WizardStep.USER_ID
            WizardStep.USER_ID -> WizardStep.TOKEN
            WizardStep.TOKEN -> WizardStep.CHECK_CONNECTION
            WizardStep.CHECK_CONNECTION -> WizardStep.DONE
            WizardStep.DONE -> WizardStep.DONE
        }
    }

    fun previous(step: WizardStep): WizardStep {
        return when (step) {
            WizardStep.WELCOME -> WizardStep.WELCOME
            WizardStep.USER_ID -> WizardStep.WELCOME
            WizardStep.TOKEN -> WizardStep.USER_ID
            WizardStep.CHECK_CONNECTION -> WizardStep.TOKEN
            WizardStep.DONE -> WizardStep.CHECK_CONNECTION
        }
    }

    fun canProceed(
        step: WizardStep,
        userId: String,
        token: String,
        connectionCheckPassed: Boolean,
    ): Boolean {
        return when (step) {
            WizardStep.WELCOME -> true
            WizardStep.USER_ID -> userId.isNotBlank()
            WizardStep.TOKEN -> token.isNotBlank()
            WizardStep.CHECK_CONNECTION -> connectionCheckPassed
            WizardStep.DONE -> true
        }
    }
}
