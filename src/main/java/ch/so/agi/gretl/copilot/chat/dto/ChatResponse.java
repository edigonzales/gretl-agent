package ch.so.agi.gretl.copilot.chat.dto;

import ch.so.agi.gretl.copilot.orchestration.TaskType;

public record ChatResponse(TaskType goal, String answer) {
}
