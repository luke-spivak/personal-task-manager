package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionResponse;
import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;
    private OpenAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiClient(objectMapper, builder.build(), "test-key", "gpt-test", 1200);
    }

    @Test
    void suggestTaskNormalizesBlankDueDateFromProvider() throws Exception {
        server.expect(requestTo("https://api.openai.test/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(containsString("\"max_output_tokens\":1200")))
                .andRespond(withSuccess(completedResponse("""
                        {
                          "title": "Buy coffee",
                          "description": "",
                          "dueDate": "",
                          "priority": "medium",
                          "status": "todo"
                        }
                        """), MediaType.APPLICATION_JSON));

        TaskSuggestionResponse suggestion = client.suggestTask("buy coffee");

        assertThat(suggestion.title()).isEqualTo("Buy coffee");
        assertThat(suggestion.description()).isNull();
        assertThat(suggestion.dueDate()).isNull();
        assertThat(suggestion.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(suggestion.status()).isEqualTo(TaskStatus.TODO);
        server.verify();
    }

    @Test
    void suggestTaskMapsIncompleteProviderResponseBeforeParsingPartialJson() throws Exception {
        server.expect(requestTo("https://api.openai.test/v1/responses"))
                .andRespond(withSuccess("""
                        {
                          "status": "incomplete",
                          "error": null,
                          "incomplete_details": {
                            "reason": "max_output_tokens"
                          },
                          "output": [
                            {
                              "type": "message",
                              "status": "incomplete",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "{\\"title\\": \\"Buy"
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.suggestTask("buy coffee"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessage("AI provider response was incomplete: max_output_tokens");
        server.verify();
    }

    @Test
    void suggestTaskFallsBackToSdkOutputTextWhenOutputArrayIsMissing() throws Exception {
        server.expect(requestTo("https://api.openai.test/v1/responses"))
                .andRespond(withSuccess("""
                        {
                          "status": "completed",
                          "error": null,
                          "output_text": "{\\"title\\":\\"Plan errands\\",\\"description\\":null,\\"dueDate\\":null,\\"priority\\":\\"LOW\\",\\"status\\":\\"TODO\\"}"
                        }
                        """, MediaType.APPLICATION_JSON));

        TaskSuggestionResponse suggestion = client.suggestTask("plan errands");

        assertThat(suggestion.title()).isEqualTo("Plan errands");
        assertThat(suggestion.priority()).isEqualTo(TaskPriority.LOW);
        server.verify();
    }

    private String completedResponse(String outputText) throws Exception {
        return """
                {
                  "status": "completed",
                  "error": null,
                  "output": [
                    {
                      "type": "message",
                      "status": "completed",
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s,
                          "annotations": []
                        }
                      ]
                    }
                  ],
                  "text": {
                    "format": {
                      "type": "json_schema"
                    }
                  }
                }
                """.formatted(objectMapper.writeValueAsString(outputText));
    }
}
