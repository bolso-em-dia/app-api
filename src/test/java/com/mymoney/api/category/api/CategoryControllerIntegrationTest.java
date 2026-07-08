package com.mymoney.api.category.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import java.time.LocalDate;
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
class CategoryControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

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
        categoryA.setName("Groceries");
        categoryA.setIcon("shopping-cart");
        categoryA.setColor("#22aa66");
        categoryA.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        categoryA = categoryRepository.save(categoryA);

        categoryB = new Category();
        categoryB.setName("Transport");
        categoryB.setCreatedInMonth(LocalDate.of(2026, 5, 1));
        categoryB = categoryRepository.save(categoryB);

        adminToken = login("admin@bolso-em-dia.local", "admin123456");
        userToken = login("user@bolso-em-dia.local", "user123456");
    }

    @Test
    void adminCanListCreateGetAndUpdateCategories() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
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
                        .param("search", "trans")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Transport"))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Groceries"));

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
                .andExpect(jsonPath("$.createdInMonth").value("2026-06-01"));

        mockMvc.perform(get("/api/categories/" + categoryA.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groceries"));

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
                                  "archivedFromMonth": "2026-08-01",
                                  "replacementCategoryId": "%s"
                                }
                                """
                                        .formatted(categoryB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedFromMonth").value("2026-08-01"))
                .andExpect(jsonPath("$.replacementCategoryId")
                        .value(categoryB.getId().toString()));

        mockMvc.perform(patch("/api/categories/" + categoryB.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "archivedFromMonth": "2026-08-01",
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
                .andExpect(jsonPath("$[?(@.name=='Groceries')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='Transport')]").isNotEmpty());

        mockMvc.perform(get("/api/categories/options")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Groceries')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name=='Transport')]").isNotEmpty());
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
