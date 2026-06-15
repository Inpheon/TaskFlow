package pl.uj.taskflow.project;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException() {
        super("Project was not found");
    }
}
