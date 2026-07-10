package com.vesttrack.service;

import com.vesttrack.domain.entity.User;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.UserRepository;
import com.vesttrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zalogowanego uzytkownika"));
    }
}
