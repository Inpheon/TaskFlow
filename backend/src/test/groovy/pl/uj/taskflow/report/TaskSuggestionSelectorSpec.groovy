package pl.uj.taskflow.report

import pl.uj.taskflow.task.Task
import pl.uj.taskflow.task.TaskPriority
import pl.uj.taskflow.task.TaskStatus
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TaskSuggestionSelectorSpec extends Specification {

    static final LocalDate TODAY = LocalDate.of(2026, 6, 15)

    TaskSuggestionSelector selector = new TaskSuggestionSelector()

    def "prefers overdue task over future and undated tasks"() {
        given:
        Task overdue = task("Overdue", TaskStatus.TODO, TaskPriority.LOW, TODAY.minusDays(1), 3)
        Task future = task("Future", TaskStatus.TODO, TaskPriority.HIGH, TODAY.plusDays(1), 2)
        Task undated = task("Undated", TaskStatus.TODO, TaskPriority.HIGH, null, 1)

        expect:
        selector.select([future, undated, overdue], TODAY).orElseThrow().task() == overdue
    }

    def "uses earliest due date before priority"() {
        given:
        Task earlier = task("Earlier", TaskStatus.TODO, TaskPriority.LOW, TODAY.plusDays(1), 2)
        Task later = task("Later", TaskStatus.TODO, TaskPriority.HIGH, TODAY.plusDays(2), 1)

        expect:
        selector.select([later, earlier], TODAY).orElseThrow().task() == earlier
    }

    def "uses higher priority when due dates match"() {
        given:
        LocalDate dueDate = TODAY.plusDays(1)
        Task low = task("Low", TaskStatus.TODO, TaskPriority.LOW, dueDate, 1)
        Task high = task("High", TaskStatus.TODO, TaskPriority.HIGH, dueDate, 2)

        expect:
        selector.select([low, high], TODAY).orElseThrow().task() == high
    }

    def "uses oldest creation time as final business tie breaker"() {
        given:
        Task older = task("Older", TaskStatus.TODO, TaskPriority.MEDIUM, null, 1)
        Task newer = task("Newer", TaskStatus.TODO, TaskPriority.MEDIUM, null, 2)

        expect:
        selector.select([newer, older], TODAY).orElseThrow().task() == older
    }

    def "excludes completed tasks"() {
        given:
        Task done = task("Done", TaskStatus.DONE, TaskPriority.HIGH, TODAY.minusDays(10), 1)
        Task open = task("Open", TaskStatus.TODO, TaskPriority.LOW, null, 2)

        expect:
        selector.select([done, open], TODAY).orElseThrow().task() == open
    }

    def "returns empty when every task is completed"() {
        given:
        Task done = task("Done", TaskStatus.DONE, TaskPriority.HIGH, TODAY.minusDays(1), 1)

        expect:
        selector.select([done], TODAY).isEmpty()
    }

    def "returns structured reason for selected task"() {
        given:
        Task candidate = task("Candidate", TaskStatus.TODO, priority, dueDate, 1)

        expect:
        selector.select([candidate], TODAY).orElseThrow().reason() == expectedReason

        where:
        priority            | dueDate            || expectedReason
        TaskPriority.HIGH   | TODAY.minusDays(1) || SuggestionReason.OVERDUE
        TaskPriority.LOW    | TODAY.plusDays(1)  || SuggestionReason.NEAREST_DUE_DATE
        TaskPriority.HIGH   | null               || SuggestionReason.HIGH_PRIORITY
        TaskPriority.MEDIUM | null               || SuggestionReason.OLDEST_OPEN_TASK
    }

    private Task task(
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        long createdSecond
    ) {
        Task task = Mock()
        task.getTitle() >> title
        task.getStatus() >> status
        task.getPriority() >> priority
        task.getDueDate() >> dueDate
        task.getCreatedAt() >> Instant.ofEpochSecond(createdSecond)
        task.getId() >> UUID.nameUUIDFromBytes(title.getBytes(StandardCharsets.UTF_8))
        return task
    }
}
