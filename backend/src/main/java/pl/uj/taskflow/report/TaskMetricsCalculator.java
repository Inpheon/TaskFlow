package pl.uj.taskflow.report;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskPriority;
import pl.uj.taskflow.task.TaskRules;
import pl.uj.taskflow.task.TaskStatus;

@Component
public class TaskMetricsCalculator {

    public TaskMetrics calculate(List<Task> tasks, LocalDate today) {
        long todo = countByStatus(tasks, TaskStatus.TODO);
        long inProgress = countByStatus(tasks, TaskStatus.IN_PROGRESS);
        long done = countByStatus(tasks, TaskStatus.DONE);
        long total = tasks.size();
        int completionPercentage = total == 0
            ? 0
            : (int) Math.round(done * 100.0 / total);
        long overdue = tasks.stream()
            .filter(task -> TaskRules.isOverdue(task, today))
            .count();
        long highPriorityOpen = tasks.stream()
            .filter(TaskRules::isOpen)
            .filter(task -> task.getPriority() == TaskPriority.HIGH)
            .count();

        return new TaskMetrics(
            total,
            todo,
            inProgress,
            done,
            completionPercentage,
            overdue,
            highPriorityOpen
        );
    }

    private long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream()
            .filter(task -> task.getStatus() == status)
            .count();
    }

}
