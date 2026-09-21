package com.techstore.api.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 140) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug deve conter apenas letras minúsculas, números e hífens") String slug
) {}
