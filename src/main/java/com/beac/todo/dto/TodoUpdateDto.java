package com.beac.todo.dto;

import com.beac.todo.entity.TodoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TodoUpdateDto(

        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        @Size(max = 20, message = "제목은 20자를 넘을 수 없습니다.")
        String title,

        @Size(max = 50, message = "내용은 50자를 넘을 수 없습니다.")
        String content,

        @NotNull(message = "상태는 필수입니다. (TODO, DONE)")
        TodoStatus status
) {
}
