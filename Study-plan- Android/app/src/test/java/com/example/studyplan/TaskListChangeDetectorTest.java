package com.example.studyplan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TaskListChangeDetectorTest {

    @Test
    public void detectsTaskAddedByDatabaseRefresh() {
        List<Task> displayed = Collections.singletonList(task(1L, 0));
        List<Task> latest = Arrays.asList(task(1L, 0), task(2L, 0));

        assertTrue(TaskListChangeDetector.hasChanged(displayed, latest));
    }

    @Test
    public void detectsTaskRemovedByDatabaseRefresh() {
        List<Task> displayed = Arrays.asList(task(1L, 0), task(2L, 0));
        List<Task> latest = Collections.singletonList(task(2L, 0));

        assertTrue(TaskListChangeDetector.hasChanged(displayed, latest));
    }

    @Test
    public void detectsCompletionStatusChange() {
        List<Task> displayed = Collections.singletonList(task(1L, 0));
        List<Task> latest = Collections.singletonList(task(1L, 1));

        assertTrue(TaskListChangeDetector.hasChanged(displayed, latest));
    }

    @Test
    public void ignoresOrderOnlyChanges() {
        List<Task> displayed = Arrays.asList(task(1L, 0), task(2L, 1));
        List<Task> latest = Arrays.asList(task(2L, 1), task(1L, 0));

        assertFalse(TaskListChangeDetector.hasChanged(displayed, latest));
    }

    @Test
    public void treatsTwoNullListsAsUnchanged() {
        assertFalse(TaskListChangeDetector.hasChanged(null, null));
    }

    private static Task task(long id, int status) {
        Task task = new Task();
        task.id = id;
        task.status = status;
        return task;
    }
}
