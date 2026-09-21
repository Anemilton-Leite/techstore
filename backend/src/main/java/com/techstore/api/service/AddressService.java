package com.techstore.api.service;

import com.techstore.api.dto.address.AddressRequest;
import com.techstore.api.dto.address.AddressResponse;
import java.util.List;

public interface AddressService {
    List<AddressResponse> findMine();
    AddressResponse create(AddressRequest request);
    AddressResponse update(Long id, AddressRequest request);
    void delete(Long id);
    void setPrincipal(Long id);
}
