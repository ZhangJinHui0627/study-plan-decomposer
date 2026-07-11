package com.example.studyplan;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TaskListChangeDetector {

    private TaskListChangeDetector() {
    }

    static boolean hasChanged(List<Task> displayed, List<Task> latest) {
        return !toStatusMap(displayed).equals(toStatusMap(latest));
    }

    private static Map<Long, Integer> toStatusMap(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> statuses = new HashMap<>();
        for (Task task : tasks) {
            statuses.put(task.id, task.status);
        }
        return statuses;
    }
}
