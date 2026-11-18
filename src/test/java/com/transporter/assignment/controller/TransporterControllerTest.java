package com.transporter.assignment.controller;

import com.transporter.assignment.Samples;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void savesSubmittedInput() throws Exception {
        submitInput(Samples.read(Samples.TEST_CASE_2))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status": "success", "message": "Input data saved successfully."}
                        """));
    }

    @Test
    void returnsTheOptimalAssignmentForTestCaseTwo() throws Exception {
        submitInput(Samples.read(Samples.TEST_CASE_2)).andExpect(status().isOk());

        requestAssignment("{\"maxTransporters\": 3}")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "status": "success",
                          "totalCost": 7500,
                          "assignments": [
                            {"laneId": 1, "transporterId": 1},
                            {"laneId": 2, "transporterId": 1},
                            {"laneId": 3, "transporterId": 2},
                            {"laneId": 4, "transporterId": 2},
                            {"laneId": 5, "transporterId": 3}
                          ],
                          "selectedTransporters": [1, 2, 3],
                          "optimal": true
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void explainsWhyTheSpecExampleCannotBeCoveredByThreeTransporters() throws Exception {
        submitInput(Samples.read(Samples.SPEC_EXAMPLE)).andExpect(status().isOk());

        requestAssignment("{\"maxTransporters\": 3}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message")
                        .value("Covering all 10 lanes needs at least 5 transporters, but maxTransporters is 3."));
    }

    @Test
    void importsTheQuoteSheetAndOptimisesIt() throws Exception {
        MockMultipartFile sheet = new MockMultipartFile("file", Samples.QUOTE_SHEET, "text/csv",
                Samples.read(Samples.QUOTE_SHEET).getBytes());

        mockMvc.perform(multipart("/api/v1/transporters/input/csv").file(sheet))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Input data saved successfully. Loaded 9 lanes and 7 transporters."));

        requestAssignment("{\"maxTransporters\": 3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(134876))
                .andExpect(jsonPath("$.selectedTransporters").value(contains(1, 4, 6)));
    }

    @Test
    void rejectsACsvUploadWithoutAFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/transporters/input/csv"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void listsEveryMissingFieldInTheInput() throws Exception {
        submitInput("""
                {
                  "lanes": [{"id": 1, "destination": "Delhi"}],
                  "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": -5}]}]
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details").value(hasItem("lanes[0].origin must not be blank")))
                .andExpect(jsonPath("$.details").value(hasItem("transporters[0].laneQuotes[0].quote must be greater than 0")));
    }

    @Test
    void rejectsInputThatIsWellFormedButInconsistent() throws Exception {
        submitInput("""
                {
                  "lanes": [{"id": 1, "origin": "Pune", "destination": "Delhi"}, {"id": 2, "origin": "Agra", "destination": "Kanpur"}],
                  "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 500}]}]
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("Lane 2 has no quotes from any transporter, so it can never be covered"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        submitInput("{\"lanes\": [")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void requiresAPositiveMaxTransporters() throws Exception {
        requestAssignment("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("maxTransporters must not be null"));

        requestAssignment("{\"maxTransporters\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("maxTransporters must be greater than 0"));
    }

    @Test
    void answersUnsupportedMethodsInTheSameErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/transporters/input"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void sendsVisitorsOfTheRootToSwagger() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void publishesTheSampleInputsInTheApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("testCase1")))
                .andExpect(content().string(containsString("specExample")));
    }

    private ResultActions submitInput(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/transporters/input").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions requestAssignment(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/transporters/assignment").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
