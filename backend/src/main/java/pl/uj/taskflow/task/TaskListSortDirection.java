package pl.uj.taskflow.task;

import java.util.Arrays;

enum TaskListSortDirection {

    ASC("asc"),
    DESC("desc");

    private final String value;

    TaskListSortDirection(String value) {
        this.value = value;
    }

    static TaskListSortDirection from(String value) {
        return Arrays.stream(values())
            .filter(direction -> direction.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new InvalidTaskListQueryException("Unsupported task sort direction: " + value));
    }
}
