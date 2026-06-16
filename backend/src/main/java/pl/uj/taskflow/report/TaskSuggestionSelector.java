package pl.uj.taskflow.report;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskPriority;

@Component
public class TaskSuggestionSelector {

    public Optional<TaskSuggestion> select(List<Task> tasks, LocalDate today) {
        return tasks.stream()
            .filter(TaskInsightRules::isOpen)
            .min(comparator(today))
            .map(task -> new TaskSuggestion(task, reason(task, today)));
    }

    private Comparator<Task> comparator(LocalDate today) {
        return Comparator
            .comparing((Task task) -> !TaskInsightRules.isOverdue(task, today))
            .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(task -> priorityRank(task.getPriority()))
            .thenComparing(Task::getCreatedAt)
            .thenComparing(Task::getId);
    }

    private SuggestionReason reason(Task task, LocalDate today) {
        if (TaskInsightRules.isOverdue(task, today)) {
            return SuggestionReason.OVERDUE;
        }
        if (task.getDueDate() != null) {
            return SuggestionReason.NEAREST_DUE_DATE;
        }
        if (task.getPriority() == TaskPriority.HIGH) {
            return SuggestionReason.HIGH_PRIORITY;
        }
        return SuggestionReason.OLDEST_OPEN_TASK;
    }

    private int priorityRank(TaskPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
