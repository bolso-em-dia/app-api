package com.mymoney.api.fixedexpense.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import java.math.BigDecimal;
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
class FixedExpenseTemplateControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    private String adminToken;
    private String userToken;
    private Category category;
    private Account account;
    private FixedExpenseTemplate template;

    @BeforeEach
    void setUp() throws Exception {
        FamilyMember regularUser = familyMemberRepository
                .findByEmailIgnoreCase("user@my-money.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@my-money.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        familyMemberRepository.save(regularUser);

        category = new Category();
        category.setName("Housing");
        category.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        category = categoryRepository.save(category);

        account = new Account();
        account.setName("Main Checking");
        account.setType(AccountType.CHECKING);
        account.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        account = accountRepository.save(account);

        template = new FixedExpenseTemplate();
        template.setName("Rent");
        template.setAmount(new BigDecimal("1800.00"));
        template.setCategory(category);
        template.setAccount(account);
        template.setDueDay((short) 5);
        template.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        template.setActive(true);
        template = fixedExpenseTemplateRepository.save(template);

        adminToken = login("admin@my-money.local", "admin123456");
        userToken = login("user@my-money.local", "user123456");
    }

    @Test
    void adminCanListCreateGetAndUpdateTemplates() throws Exception {
        mockMvc.perform(get("/api/fixed-expense-templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        template.setArchivedFromMonth(LocalDate.of(2026, 7, 1));
        template.setActive(false);
        fixedExpenseTemplateRepository.save(template);

        mockMvc.perform(get("/api/fixed-expense-templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "rent")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Rent"))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(post("/api/fixed-expense-templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Internet",
                                  "amount": 120.50,
                                  "categoryId": "%s",
                                  "accountId": "%s",
                                  "dueDay": 12
                                }
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Internet"))
                .andExpect(jsonPath("$.amount").value(120.5))
                .andExpect(jsonPath("$.dueDay").value(12));

        mockMvc.perform(get("/api/fixed-expense-templates/" + template.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent"));

        mockMvc.perform(put("/api/fixed-expense-templates/" + template.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Rent Updated",
                                  "amount": 1850.00,
                                  "categoryId": "%s",
                                  "accountId": "%s",
                                  "dueDay": 7
                                }
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent Updated"))
                .andExpect(jsonPath("$.amount").value(1850.0))
                .andExpect(jsonPath("$.dueDay").value(7));
    }

    @Test
    void archiveValidationAndAuthorizationAreEnforced() throws Exception {
        mockMvc.perform(post("/api/fixed-expense-templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Invalid Due Day",
                                  "amount": 10.00,
                                  "categoryId": "%s",
                                  "accountId": "%s",
                                  "dueDay": 40
                                }
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(
                        patch("/api/fixed-expense-templates/" + template.getId() + "/archive")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "archivedFromMonth": "2026-07-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedFromMonth").value("2026-07-01"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/fixed-expense-templates").header("Authorization", "Bearer " + userToken))
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
