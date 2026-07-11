package com.example.studyplan;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TaskInteractionPolicyTest {

    @Test
    public void manualCompletionEnabledAlwaysTogglesStatus() {
        assertEquals(TaskInteractionPolicy.TapAction.TOGGLE_STATUS,
                TaskInteractionPolicy.tapAction(true, 0));
        assertEquals(TaskInteractionPolicy.TapAction.TOGGLE_STATUS,
                TaskInteractionPolicy.tapAction(true, 1));
    }

    @Test
    public void manualCompletionDisabledStartsTimerForUnfinishedTask() {
        assertEquals(TaskInteractionPolicy.TapAction.OPEN_TIMER,
                TaskInteractionPolicy.tapAction(false, 0));
    }

    @Test
    public void manualCompletionDisabledCanRedoCompletedTask() {
        assertEquals(TaskInteractionPolicy.TapAction.TOGGLE_STATUS,
                TaskInteractionPolicy.tapAction(false, 1));
    }
}
