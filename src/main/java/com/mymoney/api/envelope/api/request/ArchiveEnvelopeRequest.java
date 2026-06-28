package com.mymoney.api.envelope.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ArchiveEnvelopeRequest(@NotNull LocalDate archivedFromMonth) {}
