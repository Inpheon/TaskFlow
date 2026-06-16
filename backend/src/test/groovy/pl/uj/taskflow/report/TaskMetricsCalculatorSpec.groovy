package pl.uj.taskflow.report

import pl.uj.taskflow.task.Task
import pl.uj.taskflow.task.TaskPriority
import pl.uj.taskflow.task.TaskStatus
import spock.lang.Specification

import java.time.LocalDate

class TaskMetricsCalculatorSpec extends Specification {

    TaskMetricsCalculator calculator = new TaskMetricsCalculator()
    LocalDate today = LocalDate.of(2026, 6, 15)

    def "calculates status counts and rounded completion percentage"() {
        given:
        List<Task> tasks = [
            task(TaskStatus.TODO),
            task(TaskStatus.IN_PROGRESS),
            task(TaskStatus.DONE)
        ]

        when:
        TaskMetrics metrics = calculator.calculate(tasks, today)

        then:
        metrics.totalTasks() == 3
        metrics.todoTasks() == 1
        metrics.inProgressTasks() == 1
        metrics.doneTasks() == 1
        metrics.openTasks() == 2
        metrics.completionPercentage() == 33
    }

    def "returns zero metrics for no tasks"() {
        expect:
        calculator.calculate([], today) == new TaskMetrics(0, 0, 0, 0, 0, 0, 0)
    }

    def "counts only open tasks due before today as overdue"() {
        given:
        List<Task> tasks = [
            task(TaskStatus.TODO, TaskPriority.MEDIUM, today.minusDays(1)),
            task(TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, today),
            task(TaskStatus.TODO, TaskPriority.MEDIUM, today.plusDays(1)),
            task(TaskStatus.DONE, TaskPriority.MEDIUM, today.minusDays(2)),
            task(TaskStatus.TODO, TaskPriority.MEDIUM, null)
        ]

        expect:
        calculator.calculate(tasks, today).overdueTasks() == 1
    }

    def "counts high priority tasks only while open"() {
        given:
        List<Task> tasks = [
            task(TaskStatus.TODO, TaskPriority.HIGH, null),
            task(TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null),
            task(TaskStatus.DONE, TaskPriority.HIGH, null),
            task(TaskStatus.TODO, TaskPriority.MEDIUM, null)
        ]

        expect:
        calculator.calculate(tasks, today).highPriorityOpenTasks() == 2
    }

    private Task task(
        TaskStatus status,
        TaskPriority priority = TaskPriority.MEDIUM,
        LocalDate dueDate = null
    ) {
        Task task = Mock()
        task.getStatus() >> status
        task.getPriority() >> priority
        task.getDueDate() >> dueDate
        return task
    }
}
