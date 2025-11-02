package ch.so.agi.gretl.copilot.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {
}
