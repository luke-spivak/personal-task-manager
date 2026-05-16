package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionResponse;
import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(AiTaskControllerTest.MockAiClientConfiguration.class)
class AiTaskControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProgrammableAiClient aiClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        aiClient.reset();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void suggestTaskReturnsStructuredSuggestionFromAiClient() throws Exception {
        aiClient.respondWith(new TaskSuggestionResponse(
                "Submit quarterly report",
                "Submit the quarterly report before Friday.",
                LocalDate.of(2026, 5, 22),
                TaskPriority.HIGH,
                TaskStatus.TODO
        ));

        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "  remind me to submit the quarterly report before Friday  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Submit quarterly report"))
                .andExpect(jsonPath("$.description").value("Submit the quarterly report before Friday."))
                .andExpect(jsonPath("$.dueDate").value("2026-05-22"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"));

        assertThat(aiClient.lastDescription()).isEqualTo("remind me to submit the quarterly report before Friday");
        assertThat(aiClient.callCount()).isEqualTo(1);
    }

    @Test
    void suggestTaskDefaultsMissingPriorityAndStatus() throws Exception {
        aiClient.respondWith(new TaskSuggestionResponse(
                "Buy coffee",
                null,
                null,
                null,
                null
        ));

        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "buy coffee"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy coffee"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.dueDate").doesNotExist())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void suggestTaskRejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("description: description is required"))
                .andExpect(jsonPath("$.path").value("/tasks/suggest"));

        assertThat(aiClient.callCount()).isZero();
    }

    @Test
    void suggestTaskMapsAiClientFailureToServiceUnavailable() throws Exception {
        aiClient.failWith(new AiServiceUnavailableException("AI model unavailable"));

        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "plan tomorrow's errands"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("AI model unavailable"))
                .andExpect(jsonPath("$.path").value("/tasks/suggest"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockAiClientConfiguration {

        @Bean
        @Primary
        ProgrammableAiClient aiClient() {
            return new ProgrammableAiClient();
        }
    }

    static class ProgrammableAiClient implements AiClient {

        private final AtomicInteger callCount = new AtomicInteger();
        private TaskSuggestionResponse response;
        private RuntimeException failure;
        private String lastDescription;

        void respondWith(TaskSuggestionResponse response) {
            this.response = response;
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        void reset() {
            callCount.set(0);
            response = null;
            failure = null;
            lastDescription = null;
        }

        int callCount() {
            return callCount.get();
        }

        String lastDescription() {
            return lastDescription;
        }

        @Override
        public TaskSuggestionResponse suggestTask(String description) {
            callCount.incrementAndGet();
            lastDescription = description;
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
