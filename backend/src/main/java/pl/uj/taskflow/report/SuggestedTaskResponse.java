package pl.uj.taskflow.report;

import pl.uj.taskflow.task.Task;

public record SuggestedTaskResponse(
    SuggestedTaskSummary task,
    SuggestionReason reason
) {

    static SuggestedTaskResponse from(Task task, SuggestionReason reason) {
        return new SuggestedTaskResponse(SuggestedTaskSummary.from(task), reason);
    }
}
