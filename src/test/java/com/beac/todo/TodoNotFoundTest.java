package com.beac.todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@DisplayName("할 일 API — 404 없는 할 일")
class TodoNotFoundTest extends TodoApiTestSupport {

    private static final String MESSAGE = "해당 할 일을 찾을 수 없습니다. id = " + MISSING_ID;

    @Test
    @DisplayName("조회")
    void findMissing() throws Exception {
        mockMvc.perform(get(BASE + "/" + MISSING_ID))
                .andExpect(error(404, "Not Found"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
    }

    @Test
    @DisplayName("수정")
    void updateMissing() throws Exception {
        patchJson(BASE + "/" + MISSING_ID, """
                {"title":"없음"}
                """)
                .andExpect(error(404, "Not Found"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
    }

    @Test
    @DisplayName("상태 변경")
    void changeStatusOfMissing() throws Exception {
        changeStatus(MISSING_ID, "DONE")
                .andExpect(error(404, "Not Found"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
    }

    @Test
    @DisplayName("삭제")
    void deleteMissing() throws Exception {
        mockMvc.perform(delete(BASE + "/" + MISSING_ID))
                .andExpect(error(404, "Not Found"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
    }
}
