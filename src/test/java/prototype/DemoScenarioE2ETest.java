package prototype;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DemoScenarioE2ETest extends ContainerizedSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoScenarioRunsThroughStepRollbackReplayAndHistory() throws Exception {
        mockMvc.perform(post("/simulate/step"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").exists());

        mockMvc.perform(post("/simulate/step"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/simulate/rollback"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/simulate/replay"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/simulate/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }
}

