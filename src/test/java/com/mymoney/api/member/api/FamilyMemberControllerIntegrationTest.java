package com.mymoney.api.member.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FamilyMemberControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private FamilyMember regularUser;

    @BeforeEach
    void setUp() throws Exception {
        regularUser = familyMemberRepository
                .findByEmailIgnoreCase("user@my-money.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@my-money.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        regularUser = familyMemberRepository.save(regularUser);

        adminToken = login("admin@my-money.local", "admin123456");
        userToken = login("user@my-money.local", "user123456");
    }

    @Test
    void adminCanListFamilyMembers() throws Exception {
        mockMvc.perform(get("/api/family-members")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "user")
                        .param("status", "ACTIVE")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("user@my-money.local"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void adminCanCreateFamilyMember() throws Exception {
        mockMvc.perform(
                        post("/api/family-members")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Karol",
                                  "email": "karol@my-money.local",
                                  "password": "karol123456",
                                  "role": "USER",
                                  "allowanceEnabled": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Karol"))
                .andExpect(jsonPath("$.email").value("karol@my-money.local"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.allowanceEnabled").value(true));
    }

    @Test
    void adminCanGetUpdateArchiveAndRestoreFamilyMember() throws Exception {
        mockMvc.perform(get("/api/family-members/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@my-money.local"));

        mockMvc.perform(
                        put("/api/family-members/" + regularUser.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Updated User",
                                  "email": "user@my-money.local",
                                  "password": "updated123456",
                                  "role": "USER",
                                  "allowanceEnabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.allowanceEnabled").value(true));

        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.allowanceEnabled").value(false));

        mockMvc.perform(get("/api/family-members")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("user@my-money.local"));

        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/restore")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void validationErrorsReturnBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/family-members")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": "123",
                                  "role": "USER",
                                  "allowanceEnabled": false
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotAccessFamilyMembersApi() throws Exception {
        mockMvc.perform(get("/api/family-members").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void archivedMemberCannotAuthenticate() throws Exception {
        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "email": "user@my-money.local",
                                  "password": "user123456"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """
                                        .formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");
    }
}
