package com.beac.todo;

import com.beac.todo.repository.TodoRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 할 일 API 통합 테스트의 공통 설정과 헬퍼.
 * 테스트마다 테이블을 비워 기동 시 들어가는 예시 데이터의 영향을 받지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class TodoApiTestSupport {

    protected static final String BASE = "/api/v1/todos";
    protected static final long MISSING_ID = 999_999L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TodoRepository todoRepository;

    @BeforeEach
    void clearTodos() {
        todoRepository.deleteAll();
    }

    protected long create(String title, String content) throws Exception {
        String body = content == null
                ? """
                  {"title":"%s"}
                  """.formatted(title)
                : """
                  {"title":"%s","content":"%s"}
                  """.formatted(title, content);

        String response = postJson(BASE, body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    protected ResultActions update(long id, String body) throws Exception {
        return mockMvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected void assertTitle(long id, String expected) throws Exception {
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(jsonPath("$.title").value(expected));
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 모든 오류 응답이 따르는 {@code {status, error, message}} 형식을 검사한다. */
    protected static ResultMatcher error(int status, String error) {
        return ResultMatcher.matchAll(
                status().is(status),
                jsonPath("$.status").value(status),
                jsonPath("$.error").value(error),
                jsonPath("$.message").isString()
        );
    }
}
