package pl.uj.taskflow.task;

public class InvalidTaskListQueryException extends RuntimeException {

    InvalidTaskListQueryException(String message) {
        super(message);
    }
}
