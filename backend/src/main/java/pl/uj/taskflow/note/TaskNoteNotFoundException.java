package pl.uj.taskflow.note;

public class TaskNoteNotFoundException extends RuntimeException {

    public TaskNoteNotFoundException() {
        super("Task note was not found");
    }
}
