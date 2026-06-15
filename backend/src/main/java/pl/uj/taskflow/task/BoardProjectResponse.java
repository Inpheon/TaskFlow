package pl.uj.taskflow.task;

import java.util.UUID;

import pl.uj.taskflow.project.Project;

public record BoardProjectResponse(UUID id, String name) {

    static BoardProjectResponse from(Project project) {
        return new BoardProjectResponse(project.getId(), project.getName());
    }
}
