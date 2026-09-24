package com.beac.todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@DisplayName("할 일 API — 400 잘못된 요청")
class TodoValidationTest extends TodoApiTestSupport {

    @Test
    @DisplayName("생성: 공백뿐인 제목")
    void createWithBlankTitle() throws Exception {
        postJson(BASE, """
                {"title":"   "}
                """)
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 제목은 비어 있을 수 없습니다."));
        assertThat(todoRepository.count()).isZero();
    }

    @Test
    @DisplayName("생성: 빈 제목")
    void createWithEmptyTitle() throws Exception {
        postJson(BASE, """
                {"title":""}
                """)
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message", startsWith("title:")));
    }

    @Test
    @DisplayName("생성: 제목 누락")
    void createWithoutTitle() throws Exception {
        postJson(BASE, """
                {"content":"내용만"}
                """)
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message", startsWith("title:")));
    }

    @Test
    @DisplayName("생성: 20자를 넘는 제목")
    void createWithTooLongTitle() throws Exception {
        postJson(BASE, """
                {"title":"%s"}
                """.formatted("가".repeat(21)))
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 제목은 20자를 넘을 수 없습니다."));
        assertThat(todoRepository.count()).isZero();
    }

    @Test
    @DisplayName("생성: 본문 없음")
    void createWithoutBody() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(error(400, "Bad Request"));
    }

    @Test
    @DisplayName("수정: 공백뿐인 제목")
    void updateWithBlankTitle() throws Exception {
        long id = create("우유 사기", null);

        patchJson(BASE + "/" + id, """
                {"title":"   "}
                """)
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 제목은 공백만으로 이루어질 수 없습니다."));
        assertTitle(id, "우유 사기");
    }

    @Test
    @DisplayName("수정: 20자를 넘는 제목")
    void updateWithTooLongTitle() throws Exception {
        long id = create("우유 사기", null);

        patchJson(BASE + "/" + id, """
                {"title":"%s"}
                """.formatted("가".repeat(21)))
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 제목은 20자를 넘을 수 없습니다."));
        assertTitle(id, "우유 사기");
    }

    @Test
    @DisplayName("상태 변경: status 누락")
    void changeStatusWithoutStatus() throws Exception {
        long id = create("우유 사기", null);

        patchJson(BASE + "/" + id + "/status", "{}")
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message", startsWith("status:")));
    }

    @Test
    @DisplayName("상태 변경: 없는 enum 값")
    void changeStatusWithUnknownValue() throws Exception {
        long id = create("우유 사기", null);

        changeStatus(id, "done")
                .andExpect(error(400, "Bad Request"));
    }

    @Test
    @DisplayName("목록: 잘못된 page·size·status")
    void listWithInvalidParams() throws Exception {
        mockMvc.perform(get(BASE).param("page", "-1"))
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message", startsWith("page:")));

        mockMvc.perform(get(BASE).param("size", "101"))
                .andExpect(error(400, "Bad Request"))
                .andExpect(jsonPath("$.message", startsWith("size:")));

        mockMvc.perform(get(BASE).param("status", "done"))
                .andExpect(error(400, "Bad Request"));
    }

    @Test
    @DisplayName("id 가 숫자가 아님")
    void idTypeMismatch() throws Exception {
        mockMvc.perform(get(BASE + "/abc"))
                .andExpect(error(400, "Bad Request"));
    }
}
