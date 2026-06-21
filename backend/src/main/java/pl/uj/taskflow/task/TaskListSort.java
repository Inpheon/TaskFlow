package pl.uj.taskflow.task;

import java.util.Arrays;

enum TaskListSort {

    CREATED_AT("createdAt"),
    DUE_DATE("dueDate"),
    PRIORITY("priority"),
    TITLE("title"),
    STATUS("status");

    private final String value;

    TaskListSort(String value) {
        this.value = value;
    }

    static TaskListSort from(String value) {
        return Arrays.stream(values())
            .filter(sort -> sort.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new InvalidTaskListQueryException("Unsupported task sort: " + value));
    }
}
