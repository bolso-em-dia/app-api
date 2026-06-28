package com.mymoney.api.member.api;

import com.mymoney.api.member.FamilyMemberService;
import com.mymoney.api.member.api.request.CreateFamilyMemberRequest;
import com.mymoney.api.member.api.request.UpdateFamilyMemberRequest;
import com.mymoney.api.member.api.response.FamilyMemberResponse;
import com.mymoney.api.member.mapper.FamilyMemberMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/family-members")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;
    private final FamilyMemberMapper familyMemberMapper;

    @GetMapping
    public ResponseEntity<List<FamilyMemberResponse>> list() {
        return ResponseEntity.ok(familyMemberService.listAll().stream()
                .map(familyMemberMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyMemberResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(familyMemberMapper.toResponse(familyMemberService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<FamilyMemberResponse> create(@Valid @RequestBody CreateFamilyMemberRequest request) {
        FamilyMemberResponse response = familyMemberMapper.toResponse(familyMemberService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyMemberResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateFamilyMemberRequest request) {
        return ResponseEntity.ok(familyMemberMapper.toResponse(familyMemberService.update(id, request)));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<FamilyMemberResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(familyMemberMapper.toResponse(familyMemberService.archive(id)));
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<FamilyMemberResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(familyMemberMapper.toResponse(familyMemberService.restore(id)));
    }
}
