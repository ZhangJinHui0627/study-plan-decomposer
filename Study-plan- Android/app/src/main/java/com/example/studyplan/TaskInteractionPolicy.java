package com.example.studyplan;

final class TaskInteractionPolicy {

    enum TapAction {
        TOGGLE_STATUS,
        OPEN_TIMER,
        NONE
    }

    private TaskInteractionPolicy() {
    }

    static TapAction tapAction(boolean manualCompletionEnabled, int taskStatus) {
        if (taskStatus == 1) {
            return TapAction.TOGGLE_STATUS;
        }
        if (manualCompletionEnabled) {
            return TapAction.TOGGLE_STATUS;
        }
        return TapAction.OPEN_TIMER;
    }
}
