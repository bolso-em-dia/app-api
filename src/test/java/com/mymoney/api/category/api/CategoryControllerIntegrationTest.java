package com.mymoney.api.category.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import java.time.LocalDate;
import java.time.YearMonth;
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
class CategoryControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    private String adminToken;
    private String userToken;
    private Category categoryA;
    private Category categoryB;

    @BeforeEach
    void setUp() throws Exception {
        FamilyMember regularUser = familyMemberRepository
                .findByEmailIgnoreCase("user@bolso-em-dia.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@bolso-em-dia.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        familyMemberRepository.save(regularUser);

        categoryA = new Category();
        categoryA.setName("Alpha Integration Category");
        categoryA.setIcon("shopping-cart");
        categoryA.setColor("#22aa66");
        categoryA.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        categoryA = categoryRepository.save(categoryA);

        categoryB = new Category();
        categoryB.setName("Beta Integration Category");
        categoryB.setCreatedInMonth(LocalDate.of(2026, 5, 1));
        categoryB = categoryRepository.save(categoryB);

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListCreateGetAndUpdateCategories() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Integration Category")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        categoryA.setArchivedFromMonth(LocalDate.of(2026, 7, 1));
        categoryA.setReplacementCategory(categoryB);
        categoryRepository.save(categoryA);

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Beta Integration Category")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Beta Integration Category"))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Alpha Integration Category")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Alpha Integration Category"));

        mockMvc.perform(
                        post("/api/categories")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Restaurants",
                                  "icon": "utensils",
                                  "color": "#ff3300"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Restaurants"))
                .andExpect(jsonPath("$.createdInMonth")
                        .value(currentReferenceMonth().toString()));

        mockMvc.perform(get("/api/categories/" + categoryA.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alpha Integration Category"));

        mockMvc.perform(
                        put("/api/categories/" + categoryA.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Supermarket",
                                  "icon": "basket",
                                  "color": "#228844"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Supermarket"))
                .andExpect(jsonPath("$.icon").value("basket"))
                .andExpect(jsonPath("$.color").value("#228844"));
    }

    @Test
    void adminCanArchiveCategoryAndReplacementMustBeDifferent() throws Exception {
        mockMvc.perform(patch("/api/categories/" + categoryA.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "replacementCategoryId": "%s"
                                }
                                """
                                        .formatted(categoryB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedFromMonth")
                        .value(currentReferenceMonth().toString()))
                .andExpect(jsonPath("$.replacementCategoryId")
                        .value(categoryB.getId().toString()));

        mockMvc.perform(patch("/api/categories/" + categoryB.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "replacementCategoryId": "%s"
                                }
                                """
                                        .formatted(categoryB.getId())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void optionsEndpointRespectsReferenceMonth() throws Exception {
        categoryA.setArchivedFromMonth(LocalDate.of(2026, 8, 1));
        categoryA.setReplacementCategory(categoryB);
        categoryRepository.save(categoryA);

        mockMvc.perform(get("/api/categories/options")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.name=='Alpha Integration Category')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='Beta Integration Category')]").isNotEmpty());

        mockMvc.perform(get("/api/categories/options")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.name=='Alpha Integration Category')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name=='Beta Integration Category')]").isNotEmpty());
    }

    @Test
    void validationAndAuthorizationAreEnforced() throws Exception {
        mockMvc.perform(
                        post("/api/categories")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "",
                                  "icon": "x",
                                  "color": "y"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCategoriesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void createCategoryRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "icon": "x", "color": "y"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategoryAsUserIsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/categories")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "icon": "x", "color": "y"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCategoryReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/categories/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategoryReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(
                        put("/api/categories/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Updated", "icon": "x", "color": "y"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveCategoryReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/categories/00000000-0000-0000-0000-000000000000/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replacementCategoryId\": \"" + categoryA.getId() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveCategoryRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/categories/" + categoryA.getId() + "/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replacementCategoryId\": \"" + categoryB.getId() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCategoryAsUserIsForbidden() throws Exception {
        mockMvc.perform(
                        put("/api/categories/" + categoryA.getId())
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Updated", "icon": "x", "color": "y"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategoryWithNameExceedingMaxLengthReturns400() throws Exception {
        String longName = "a".repeat(121);
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "%s", "icon": "x", "color": "y"}
                                """
                                        .formatted(longName)))
                .andExpect(status().isBadRequest());
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }
}
