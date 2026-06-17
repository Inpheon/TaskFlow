package pl.uj.taskflow.note;

import java.time.Instant;
import java.util.UUID;

public record TaskNoteResponse(
    UUID id,
    UUID taskId,
    UUID authorId,
    String authorDisplayName,
    String content,
    Instant createdAt
) {

    static TaskNoteResponse from(TaskNote note) {
        return new TaskNoteResponse(
            note.getId(),
            note.getTask().getId(),
            note.getAuthor().getId(),
            note.getAuthor().getDisplayName(),
            note.getContent(),
            note.getCreatedAt()
        );
    }
}
