package pl.uj.taskflow.task;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class TaskListRequest {

    private TaskStatus status;

    private TaskPriority priority;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueBefore;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueAfter;

    private Boolean overdue;

    private String q;

    private String sort;

    private String direction;
}
