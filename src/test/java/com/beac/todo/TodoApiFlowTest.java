package com.beac.todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("할 일 API — 정상 흐름")
class TodoApiFlowTest extends TodoApiTestSupport {

    @Test
    @DisplayName("만들기 → 목록 → 완료 처리 → 삭제")
    void createListCompleteDelete() throws Exception {
        long id = create("우체국 가기", "등기 보내기");

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        update(id, """
                {"title":"우체국 가기","content":"등기 보내기","status":"DONE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound());
        assertThat(todoRepository.count()).isZero();
    }

    @Test
    @DisplayName("생성하면 201 과 함께 미완료 상태로 저장된다")
    void create() throws Exception {
        postJson(BASE, """
                {"title":"우체국 가기","content":"등기 보내기"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("우체국 가기"))
                .andExpect(jsonPath("$.content").value("등기 보내기"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("제목은 20자까지 허용한다")
    void createWithTitleAtLimit() throws Exception {
        postJson(BASE, """
                {"title":"%s"}
                """.formatted("가".repeat(20)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("단건 조회")
    void findById() throws Exception {
        long id = create("우유 사기", null);

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("우유 사기"))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("수정은 제목·내용·상태를 한 번에 교체한다")
    void updateReplacesAllFields() throws Exception {
        long id = create("우유 사기", "2L 한 통");

        update(id, """
                {"title":"우유 두 개 사기","content":"2L 저지방","status":"DONE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("우유 두 개 사기"))
                .andExpect(jsonPath("$.content").value("2L 저지방"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("수정에서 content 를 빼면 내용이 지워진다")
    void updateWithoutContentClearsIt() throws Exception {
        long id = create("우유 사기", "2L 한 통");

        update(id, """
                {"title":"우유 사기","status":"TODO"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("완료했던 할 일을 다시 미완료로 돌릴 수 있다")
    void updateBackToTodo() throws Exception {
        long id = create("우유 사기", null);
        update(id, """
                {"title":"우유 사기","status":"DONE"}
                """);

        update(id, """
                {"title":"우유 사기","status":"TODO"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("같은 수정을 반복해도 결과가 같다")
    void updateIsIdempotent() throws Exception {
        long id = create("우유 사기", null);
        String body = """
                {"title":"우유 사기","status":"DONE"}
                """;

        String first = update(id, body).andReturn().getResponse().getContentAsString();
        String second = update(id, body).andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("목록은 상태로 거르고 페이지로 나눈다")
    void listFiltersByStatusAndPages() throws Exception {
        create("하나", null);
        create("둘", null);
        long done = create("셋", null);
        update(done, """
                {"title":"셋","status":"DONE"}
                """);

        mockMvc.perform(get(BASE).param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(done))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get(BASE).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("셋"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }
}
