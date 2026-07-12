package com.mymoney.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class ApiExceptionHandlerTest {

    @Test
    void optimisticLockFailureReturnsConflict() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/accounts/id");

        var response =
                handler.handleOptimisticLock(new ObjectOptimisticLockingFailureException("Account", "id"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Resource was modified by another user.");
        assertThat(response.getBody().path()).isEqualTo("/api/accounts/id");
    }
}
