package pl.uj.taskflow.report;

import pl.uj.taskflow.task.Task;

public record TaskSuggestion(Task task, SuggestionReason reason) {
}
