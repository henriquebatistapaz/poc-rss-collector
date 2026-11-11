package com.example.rsscollector.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.rsscollector.application.FeedRegistryService;
import com.example.rsscollector.application.FeedRegistryService.FeedSummary;
import com.example.rsscollector.application.FeedUrlValidationService;
import com.example.rsscollector.application.FeedUrlValidationService.ValidationResult;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
class FeedValidationControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private FeedUrlValidationService service;
    @MockitoBean private FeedRegistryService registryService;

    @Test
    void validatesSubmittedFeedUrlAndReturnsResult() throws Exception {
        given(service.validate("https://feeds.example.org/news.xml"))
                .willReturn(new ValidationResult("https://feeds.example.org/news.xml", true, 3, "Valid feed"));

        mockMvc.perform(post("/v1/feeds/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedUrl\":\"https://feeds.example.org/news.xml\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.itemCount").value(3));
    }

    @Test
    void rejectsMissingUrl() throws Exception {
        mockMvc.perform(post("/v1/feeds/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsInvalidFeedAsNotValid() throws Exception {
        given(service.validate("https://feeds.example.org/not-feed"))
                .willReturn(new ValidationResult("https://feeds.example.org/not-feed", false, 0, "Not a feed"));

        mockMvc.perform(post("/v1/feeds/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedUrl\":\"https://feeds.example.org/not-feed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void exposesValidationEndpointInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/v1/feeds/validate'].post").exists())
                .andExpect(jsonPath("$.paths['/v1/feeds'].post").exists())
                .andExpect(jsonPath("$.paths['/v1/feeds'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/feeds/{id}/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/v1/links'].get").exists());
    }

    @Test
    void registersFeedAndListsIt() throws Exception {
        UUID id = UUID.randomUUID();
        given(registryService.register("https://feeds.example.org/news.xml"))
                .willReturn(new FeedSummary(id, URI.create("https://feeds.example.org/news.xml"),
                        Instant.parse("2026-11-11T10:00:00Z"), Instant.parse("2026-11-11T10:00:00Z"), 2));

        mockMvc.perform(post("/v1/feeds").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedUrl\":\"https://feeds.example.org/news.xml\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.itemCount").value(2));

        mockMvc.perform(get("/v1/feeds"))
                .andExpect(status().isOk());
    }
}
