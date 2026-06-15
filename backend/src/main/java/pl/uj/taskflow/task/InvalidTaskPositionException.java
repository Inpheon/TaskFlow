package pl.uj.taskflow.task;

public class InvalidTaskPositionException extends RuntimeException {

    public InvalidTaskPositionException(int position, int maximum) {
        super("Task position must be between 0 and " + maximum + ", received " + position);
    }
}
