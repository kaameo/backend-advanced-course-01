package com.beac.todo.dto;

import com.beac.todo.entity.TodoStatus;
import jakarta.validation.constraints.NotNull;

public record TodoStatusUpdateDto(
        @NotNull(message = "상태는 필수입니다. (TODO, DONE)")
        TodoStatus status
) {
}
