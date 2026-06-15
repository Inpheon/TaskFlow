package pl.uj.taskflow.task;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException() {
        super("Task was not found");
    }
}
