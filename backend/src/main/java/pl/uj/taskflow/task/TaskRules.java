package pl.uj.taskflow.task;

import java.time.LocalDate;

public final class TaskRules {

    private TaskRules() {
    }

    public static boolean isOpen(Task task) {
        return task.getStatus() != TaskStatus.DONE;
    }

    public static boolean isOverdue(Task task, LocalDate today) {
        return isOpen(task)
            && task.getDueDate() != null
            && task.getDueDate().isBefore(today);
    }
}
