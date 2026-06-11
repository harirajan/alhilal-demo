package com.alhilal.customer.controller;

import com.alhilal.customer.api.CustomerController;
import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.CustomerNotFoundException;
import com.alhilal.customer.domain.CustomerAlreadyExistsException;
import com.alhilal.customer.domain.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
// ↑ loads ONLY: Controller + MockMvc + Security
// ↑ does NOT load: Service, Repository, DB, Kafka
@DisplayName("CustomerController Tests — @WebMvcTest")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;           // fake HTTP client

    @Autowired
    private ObjectMapper objectMapper; // JSON serializer

    @MockBean
    private CustomerService customerService; // fake service

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId("CUST-C09DDEE2");
        testCustomer.setFirstName("Abdullah");
        testCustomer.setLastName("Al-Rashid");
        testCustomer.setEmail("abdullah@test.com");
        testCustomer.setMobileNumber("+971501234567");
        testCustomer.setKycStatus(Customer.KycStatus.PENDING);
        testCustomer.setCreatedAt(LocalDateTime.now());
    }

    // ─── GET /api/v1/customers/{id} ──────────────────────
    @Nested
    @DisplayName("GET /api/v1/customers/{customerId}")
    class GetCustomerTests {

        @Test
        @WithMockUser // ← Spring Security requires authenticated user
        @DisplayName("should return 200 with customer")
        void shouldReturn200WithCustomer() throws Exception {
            // GIVEN
            when(customerService.findById("CUST-C09DDEE2"))
                .thenReturn(testCustomer);

            // WHEN + THEN
            mockMvc.perform(get("/api/v1/customers/CUST-C09DDEE2"))
                .andDo(print()) // prints request/response to console
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId")
                    .value("CUST-C09DDEE2"))
                .andExpect(jsonPath("$.firstName")
                    .value("Abdullah"))
                .andExpect(jsonPath("$.kycStatus")
                    .value("PENDING"))
                .andExpect(jsonPath("$.email")
                    .value("abdullah@test.com"));

            verify(customerService).findById("CUST-C09DDEE2");
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 when customer not found")
        void shouldReturn400WhenNotFound() throws Exception {
            // GIVEN
            when(customerService.findById("CUST-UNKNOWN"))
                .thenThrow(new CustomerNotFoundException(
                    "Customer not found: CUST-UNKNOWN"));

            // WHEN + THEN
            mockMvc.perform(get("/api/v1/customers/CUST-UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                    .value("Customer not found: CUST-UNKNOWN"));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/customers/CUST-C09DDEE2"))
                .andExpect(status().isUnauthorized());
        }
    }

    // ─── POST /api/v1/customers ──────────────────────────
    @Nested
    @DisplayName("POST /api/v1/customers")
    class RegisterTests {

        @Test
        @WithMockUser
        @DisplayName("should return 201 when customer registered")
        void shouldReturn201WhenRegistered() throws Exception {
            // GIVEN
            when(customerService.register(
                anyString(), anyString(), anyString(),
                anyString(), anyString()))
                .thenReturn(testCustomer);

            String requestBody = """
                {
                    "firstName": "Abdullah",
                    "lastName": "Al-Rashid",
                    "email": "abdullah@test.com",
                    "mobileNumber": "+971501234567",
                    "emiratesId": "784-1990-1234567-1"
                }
                """;

            // WHEN + THEN
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf())) // Spring Security CSRF token
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId")
                    .value("CUST-C09DDEE2"))
                .andExpect(jsonPath("$.firstName")
                    .value("Abdullah"));

            verify(customerService).register(
                "Abdullah", "Al-Rashid",
                "abdullah@test.com",
                "+971501234567",
                "784-1990-1234567-1"
            );
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 when request body invalid")
        void shouldReturn400WhenInvalidRequest() throws Exception {
            // GIVEN — missing required fields
            String invalidRequest = """
                {
                    "firstName": "",
                    "email": "not-an-email"
                }
                """;

            // WHEN + THEN — validation fails before hitting service
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest)
                    .with(csrf()))
                .andExpect(status().isBadRequest());

            // Service should NEVER be called
            verify(customerService, never())
                .register(any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 when email already exists")
        void shouldReturn400WhenEmailExists() throws Exception {
            // GIVEN
            when(customerService.register(
                any(), any(), any(), any(), any()))
                .thenThrow(new CustomerAlreadyExistsException(
                    "Email already registered: abdullah@test.com"));

            String requestBody = """
                {
                    "firstName": "Abdullah",
                    "lastName": "Al-Rashid",
                    "email": "abdullah@test.com",
                    "mobileNumber": "+971501234567",
                    "emiratesId": "784-1990-1234567-1"
                }
                """;

            // WHEN + THEN
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                    .value("Email already registered: abdullah@test.com"));
        }
    }

    // ─── PATCH /api/v1/customers/{id}/verify-kyc ─────────
    @Nested
    @DisplayName("PATCH /api/v1/customers/{id}/verify-kyc")
    class VerifyKycTests {

        @Test
        @WithMockUser
        @DisplayName("should return 200 when KYC verified")
        void shouldReturn200WhenKycVerified() throws Exception {
            // GIVEN — customer now VERIFIED
            testCustomer.setKycStatus(Customer.KycStatus.VERIFIED);
            when(customerService.verifyKyc("CUST-C09DDEE2"))
                .thenReturn(testCustomer);

            // WHEN + THEN
            mockMvc.perform(patch(
                    "/api/v1/customers/CUST-C09DDEE2/verify-kyc")
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus")
                    .value("VERIFIED"));

            verify(customerService).verifyKyc("CUST-C09DDEE2");
        }
    }

    // ─── GET /api/v1/customers ───────────────────────────
    @Nested
    @DisplayName("GET /api/v1/customers")
    class GetAllTests {

        @Test
        @WithMockUser
        @DisplayName("should return list of customers")
        void shouldReturnAllCustomers() throws Exception {
            // GIVEN
            Customer fatima = new Customer();
            fatima.setCustomerId("CUST-AA385311");
            fatima.setFirstName("Fatima");
            fatima.setLastName("Al-Hassan");
            fatima.setEmail("fatima@test.com");
            fatima.setMobileNumber("+971509876543");
            fatima.setKycStatus(Customer.KycStatus.PENDING);
            fatima.setCreatedAt(LocalDateTime.now());

            when(customerService.findAll())
                .thenReturn(List.of(testCustomer, fatima));

            // WHEN + THEN
            mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName")
                    .value("Abdullah"))
                .andExpect(jsonPath("$[1].firstName")
                    .value("Fatima"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty list when no customers")
        void shouldReturnEmptyList() throws Exception {
            when(customerService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
