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

        changeStatus(id, "DONE")
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
    @DisplayName("부분 수정은 보낸 필드만 바꾼다")
    void updateChangesOnlySentFields() throws Exception {
        long id = create("우유 사기", "2L 한 통");

        patchJson(BASE + "/" + id, """
                {"content":"2L 저지방 한 통"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("우유 사기"))
                .andExpect(jsonPath("$.content").value("2L 저지방 한 통"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("완료했던 할 일을 다시 미완료로 돌릴 수 있다")
    void changeStatusBackToTodo() throws Exception {
        long id = create("우유 사기", null);
        changeStatus(id, "DONE");

        changeStatus(id, "TODO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("목록은 상태로 거르고 페이지로 나눈다")
    void listFiltersByStatusAndPages() throws Exception {
        create("하나", null);
        create("둘", null);
        long done = create("셋", null);
        changeStatus(done, "DONE");

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
