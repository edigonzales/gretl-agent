package ch.so.agi.gretl.copilot.orchestration;

public enum TaskType {
    FIND_TASK,
    EXPLAIN_TASK,
    GENERATE_TASK;

    public static TaskType fromModelResponse(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Model response must not be null");
        }
        String normalized = response.trim().toUpperCase();
        for (TaskType value : values()) {
            if (normalized.contains(value.name())) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to map response '%s' to TaskType".formatted(response));
    }
}
