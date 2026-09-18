package com.beac.todo.dto;

import com.beac.todo.entity.Todo;
import com.beac.todo.entity.TodoStatus;

import java.time.LocalDateTime;

public record TodoResponseDto(
        Long id,
        String title,
        String content,
        TodoStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TodoResponseDto from(Todo todo) {
        return new TodoResponseDto(
                todo.getId(),
                todo.getTitle(),
                todo.getContent(),
                todo.getStatus(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
