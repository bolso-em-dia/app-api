package com.mymoney.api.preference.api;

import com.mymoney.api.preference.UserPreferencesService;
import com.mymoney.api.preference.api.request.UpdateUserPreferencesRequest;
import com.mymoney.api.preference.api.response.UserPreferencesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @GetMapping
    public ResponseEntity<UserPreferencesResponse> getCurrentUserPreferences() {
        return ResponseEntity.ok(userPreferencesService.getCurrentUserPreferences());
    }

    @PutMapping
    public ResponseEntity<UserPreferencesResponse> updateCurrentUserPreferences(
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        return ResponseEntity.ok(userPreferencesService.updateCurrentUserPreferences(request));
    }
}
