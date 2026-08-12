package sorbonne.professional_website.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void managerApiRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/manager"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "test-admin", roles = "ADMIN")
    void managerApiAcceptsAnAuthenticatedAdministrator() throws Exception {
        mockMvc.perform(get("/manager"))
                .andExpect(status().isOk());
    }

    @Test
    void publicWebsiteEndpointRemainsPublicAndUsesTheErrorContract() throws Exception {
        mockMvc.perform(get("/website/9223372036854775807").header("X-Request-ID", "security-test-request"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-ID", "security-test-request"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value("security-test-request"));
    }
}
