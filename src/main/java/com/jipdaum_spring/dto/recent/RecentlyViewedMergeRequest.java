package com.jipdaum_spring.dto.recent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RecentlyViewedMergeRequest(
        @JsonProperty("product_ids") @NotEmpty List<@NotNull @Positive Long> productIds
) {}
