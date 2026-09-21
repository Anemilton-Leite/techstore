package com.techstore.api.repository;

import com.techstore.api.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByUserIdOrderByPrincipalDescIdDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
