package pl.uj.taskflow.report;

import java.time.LocalDate;

import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskStatus;

final class TaskInsightRules {

    private TaskInsightRules() {
    }

    static boolean isOpen(Task task) {
        return task.getStatus() != TaskStatus.DONE;
    }

    static boolean isOverdue(Task task, LocalDate today) {
        return isOpen(task)
            && task.getDueDate() != null
            && task.getDueDate().isBefore(today);
    }
}
