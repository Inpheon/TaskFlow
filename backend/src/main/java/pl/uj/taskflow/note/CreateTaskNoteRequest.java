package pl.uj.taskflow.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskNoteRequest(
    @NotBlank
    @Size(max = 5000)
    String content
) {
}
