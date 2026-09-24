package com.beac.todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ErrorResponseFormatTest extends TodoApiTestSupport {

    @Test
    @DisplayName("404 — 없는 경로")
    void unknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(error(404, "Not Found"));
    }

    @Test
    @DisplayName("405 — 지원하지 않는 메서드")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(put(BASE + "/1"))
                .andExpect(error(405, "Method Not Allowed"));
    }

    @Test
    @DisplayName("415 — 지원하지 않는 Content-Type")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(error(415, "Unsupported Media Type"));
    }
}
