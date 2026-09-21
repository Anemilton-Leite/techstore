package com.techstore.api.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve possuir 8 dígitos") String cep,
        @NotBlank @Size(max = 120) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 120) String complement,
        @NotBlank @Size(max = 100) String neighborhood,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "Estado deve conter 2 letras") String state,
        boolean principal
) {}
