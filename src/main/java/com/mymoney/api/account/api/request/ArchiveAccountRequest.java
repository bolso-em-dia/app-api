package com.mymoney.api.account.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ArchiveAccountRequest(@NotNull LocalDate archivedFromMonth) {}
