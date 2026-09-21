package com.techstore.api.controller;

import com.techstore.api.dto.address.AddressRequest;
import com.techstore.api.dto.address.AddressResponse;
import com.techstore.api.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Endereços")
public class AddressController {
    private final AddressService service;
    public AddressController(AddressService service) { this.service = service; }

    @GetMapping("/me")
    @Operation(summary = "Lista meus endereços")
    public ResponseEntity<List<AddressResponse>> findMine() { return ResponseEntity.ok(service.findMine()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@Valid @RequestBody AddressRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PatchMapping("/{id}/principal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPrincipal(@PathVariable Long id) { service.setPrincipal(id); }
}
