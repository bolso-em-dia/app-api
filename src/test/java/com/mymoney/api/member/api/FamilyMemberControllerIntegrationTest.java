package com.mymoney.api.member.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import com.mymoney.api.member.api.request.CreateFamilyMemberRequest;
import com.mymoney.api.member.api.request.UpdateFamilyMemberRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FamilyMemberControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    private String adminToken;
    private String userToken;
    private FamilyMember regularUser;

    @BeforeEach
    void setUp() throws Exception {
        regularUser = fixtures().ensureRegularUser();
        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListFamilyMembers() throws Exception {
        mockMvc.perform(get("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
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
        mockMvc.perform(post("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFamilyMemberRequest(
                                "Karol", "karol@bolso-em-dia.local", "karol123456", FamilyRole.USER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Karol"))
                .andExpect(jsonPath("$.email").value("karol@bolso-em-dia.local"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void adminCanGetUpdateArchiveAndRestoreFamilyMember() throws Exception {
        mockMvc.perform(get("/api/family-members/" + regularUser.getId())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@bolso-em-dia.local"));

        mockMvc.perform(put("/api/family-members/" + regularUser.getId())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateFamilyMemberRequest(
                                "Updated User", "user@bolso-em-dia.local", "updated123456", FamilyRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"));

        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/archive")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("user@bolso-em-dia.local"));

        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/restore")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void familyMemberWritesShouldPopulateAuditFields() throws Exception {
        var createResult = mockMvc.perform(post("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFamilyMemberRequest(
                                "Audited User", "audited@bolso-em-dia.local", "audited123456", FamilyRole.USER))))
                .andExpect(status().isCreated())
                .andReturn();

        var createdId =
                JsonTestUtils.extractJsonValue(createResult.getResponse().getContentAsString(), "id");
        var created = familyMemberRepository
                .findById(java.util.UUID.fromString(createdId))
                .orElseThrow();
        assertThat(created.getCreatedBy()).isEqualTo(adminMemberId());
        assertThat(created.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(put("/api/family-members/" + createdId)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateFamilyMemberRequest(
                                "Audited User Updated",
                                "audited@bolso-em-dia.local",
                                "audited123456",
                                FamilyRole.USER))))
                .andExpect(status().isOk());

        var updated = familyMemberRepository
                .findById(java.util.UUID.fromString(createdId))
                .orElseThrow();
        assertThat(updated.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(patch("/api/family-members/" + createdId + "/archive")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());
        assertThat(familyMemberRepository
                        .findById(java.util.UUID.fromString(createdId))
                        .orElseThrow()
                        .getUpdatedBy())
                .isEqualTo(adminMemberId());

        mockMvc.perform(patch("/api/family-members/" + createdId + "/restore")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());
        assertThat(familyMemberRepository
                        .findById(java.util.UUID.fromString(createdId))
                        .orElseThrow()
                        .getUpdatedBy())
                .isEqualTo(adminMemberId());
    }

    @Test
    void validationErrorsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFamilyMemberRequest("", "invalid-email", "123", FamilyRole.USER))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotAccessFamilyMembersApi() throws Exception {
        mockMvc.perform(get("/api/family-members").header("Authorization", bearerToken(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void archivedMemberCannotAuthenticate() throws Exception {
        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/archive")
                        .header("Authorization", bearerToken(adminToken)))
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
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(
                        put("/api/family-members/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", bearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\": \"Updated\", \"email\": \"updated@bolso-em-dia.local\", \"role\": \"USER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/family-members/00000000-0000-0000-0000-000000000000/archive")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoreFamilyMemberReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/family-members/00000000-0000-0000-0000-000000000000/restore")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveFamilyMemberRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/family-members/" + regularUser.getId() + "/archive"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFamilyMemberWithEmptyNameReturns400() throws Exception {
        mockMvc.perform(post("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFamilyMemberRequest(
                                "", "test@bolso-em-dia.local", "test123456", FamilyRole.USER))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFamilyMemberWithInvalidEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(
                                new CreateFamilyMemberRequest("Test", "not-an-email", "test123456", FamilyRole.USER))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFamilyMemberWithShortPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/family-members")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFamilyMemberRequest(
                                "Test", "test@bolso-em-dia.local", "123", FamilyRole.USER))))
                .andExpect(status().isBadRequest());
    }

    private String toJson(Object value) throws Exception {
        return fixtures().writeJson(value);
    }

    private java.util.UUID adminMemberId() {
        return fixtures().ensureAdminCanUseProtectedApis().getId();
    }
}
