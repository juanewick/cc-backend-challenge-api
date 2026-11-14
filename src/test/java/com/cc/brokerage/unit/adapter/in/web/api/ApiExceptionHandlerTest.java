package com.cc.brokerage.unit.adapter.in.web.api;

import com.cc.brokerage.adapter.in.web.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void unexpectedFailureIsA500ProblemWithoutInternalDetail() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("The request could not be processed."))
                .andExpect(jsonPath("$.invalid_params").doesNotExist());
    }

    @Test
    void illegalArgumentIsNotBlamedOnTheClient() throws Exception {
        mockMvc.perform(get("/illegal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The request could not be processed."));
    }

    @Test
    void springOwnClientErrorsKeepTheirStatus() throws Exception {
        mockMvc.perform(post("/boom")).andExpect(status().isMethodNotAllowed());
    }

    @RestController
    static class FailingController {

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret table name");
        }

        @GetMapping("/illegal")
        String illegal() {
            throw new IllegalArgumentException("secret internal detail");
        }
    }
}
