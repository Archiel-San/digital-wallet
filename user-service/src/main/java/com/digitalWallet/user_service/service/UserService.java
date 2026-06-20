package com.digitalWallet.user_service.service;

import com.digitalWallet.user_service.domain.User;
import com.digitalWallet.user_service.dtos.responses.UserResponse;
import com.digitalWallet.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserResponse findByEmail(String email){
        return  userRepository
                .findByEmail(email)
                .map(UserResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User Not found"));
    }
}
