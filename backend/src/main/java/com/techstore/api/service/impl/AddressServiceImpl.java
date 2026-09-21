package com.techstore.api.service.impl;

import com.techstore.api.dto.address.AddressRequest;
import com.techstore.api.dto.address.AddressResponse;
import com.techstore.api.entity.Address;
import com.techstore.api.entity.User;
import com.techstore.api.exception.ResourceNotFoundException;
import com.techstore.api.repository.AddressRepository;
import com.techstore.api.repository.UserRepository;
import com.techstore.api.service.AddressService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addresses;
    private final UserRepository users;

    public AddressServiceImpl(AddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> findMine() {
        User user = currentUser();
        return addresses.findAllByUserIdOrderByPrincipalDescIdDesc(user.getId()).stream().map(this::response).toList();
    }

    @Override
    @Transactional
    public AddressResponse create(AddressRequest request) {
        User user = currentUser();
        List<Address> existing = addresses.findAllByUserIdOrderByPrincipalDescIdDesc(user.getId());
        Address address = new Address();
        address.setUser(user);
        apply(address, request);
        boolean principal = request.principal() || existing.isEmpty();
        if (principal) clearPrincipal(existing);
        address.setPrincipal(principal);
        return response(addresses.save(address));
    }

    @Override
    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        User user = currentUser();
        Address address = owned(id, user.getId());
        List<Address> existing = addresses.findAllByUserIdOrderByPrincipalDescIdDesc(user.getId());
        apply(address, request);
        if (request.principal()) {
            clearPrincipal(existing, id);
            address.setPrincipal(true);
        }
        return response(addresses.save(address));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = currentUser();
        Address address = owned(id, user.getId());
        boolean principal = address.isPrincipal();
        addresses.delete(address);
        if (principal) {
            List<Address> remaining = addresses.findAllByUserIdOrderByPrincipalDescIdDesc(user.getId());
            if (!remaining.isEmpty()) {
                remaining.get(0).setPrincipal(true);
                addresses.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public void setPrincipal(Long id) {
        User user = currentUser();
        Address selected = owned(id, user.getId());
        List<Address> existing = addresses.findAllByUserIdOrderByPrincipalDescIdDesc(user.getId());
        clearPrincipal(existing);
        selected.setPrincipal(true);
        addresses.save(selected);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }

    private Address owned(Long id, Long userId) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado."));
    }

    private void apply(Address address, AddressRequest request) {
        address.setCep(request.cep().replaceAll("\\D", ""));
        address.setStreet(request.street().trim());
        address.setNumber(request.number().trim());
        address.setComplement(request.complement() == null || request.complement().isBlank() ? null : request.complement().trim());
        address.setNeighborhood(request.neighborhood().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim().toUpperCase());
    }

    private void clearPrincipal(List<Address> values) { values.forEach(address -> address.setPrincipal(false)); addresses.saveAll(values); }
    private void clearPrincipal(List<Address> values, Long exceptId) { values.stream().filter(address -> !address.getId().equals(exceptId)).forEach(address -> address.setPrincipal(false)); addresses.saveAll(values); }
    private AddressResponse response(Address address) {
        String cep = address.getCep();
        String formatted = cep != null && cep.length() == 8 ? cep.substring(0, 5) + "-" + cep.substring(5) : cep;
        return new AddressResponse(address.getId(), formatted, address.getStreet(), address.getNumber(), address.getComplement(), address.getNeighborhood(), address.getCity(), address.getState(), address.isPrincipal());
    }
}
