package com.beac.todo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TodoUpdateDto(

        @Size(max = 20, message = "제목은 20자를 넘을 수 없습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 공백만으로 이루어질 수 없습니다.")
        String title,

        @Size(max = 50, message = "내용은 50자를 넘을 수 없습니다.")
        String content
) {
}
