package com.mymoney.api.member.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FamilyMemberControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

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
                .findByEmailIgnoreCase("user@bolso-em-dia.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@bolso-em-dia.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        regularUser = familyMemberRepository.save(regularUser);

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListFamilyMembers() throws Exception {
        mockMvc.perform(get("/api/family-members")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "user")
                        .param("status", "ACTIVE")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("user@bolso-em-dia.local"))
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
                                  "email": "karol@bolso-em-dia.local",
                                  "password": "karol123456",
                                  "role": "USER",
                                  "allowanceEnabled": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Karol"))
                .andExpect(jsonPath("$.email").value("karol@bolso-em-dia.local"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.allowanceEnabled").value(true));
    }

    @Test
    void adminCanGetUpdateArchiveAndRestoreFamilyMember() throws Exception {
        mockMvc.perform(get("/api/family-members/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@bolso-em-dia.local"));

        mockMvc.perform(
                        put("/api/family-members/" + regularUser.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Updated User",
                                  "email": "user@bolso-em-dia.local",
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
                .andExpect(jsonPath("$.items[0].email").value("user@bolso-em-dia.local"));

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
                                  "email": "user@bolso-em-dia.local",
                                  "password": "user123456"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFamilyMembersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/family-members")).andExpect(status().isUnauthorized());
    }

    @Test
    void createFamilyMemberRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/family-members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "email": "test@bolso-em-dia.local", "role": "USER", "password": "test123456"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/family-members/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(
                        put("/api/family-members/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Updated", "email": "updated@bolso-em-dia.local", "role": "USER"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/family-members/00000000-0000-0000-0000-000000000000/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoreFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/family-members/00000000-0000-0000-0000-000000000000/restore")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveFamilyMemberRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/archive"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFamilyMemberWithEmptyNameReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/family-members")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "", "email": "test@bolso-em-dia.local", "role": "USER", "password": "test123456"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFamilyMemberWithInvalidEmailReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/family-members")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "email": "not-an-email", "role": "USER", "password": "test123456"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFamilyMemberWithShortPasswordReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/family-members")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "email": "test@bolso-em-dia.local", "role": "USER", "password": "123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
