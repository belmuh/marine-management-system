package com.marine.management.modules.auth.application;

import com.marine.management.modules.auth.domain.AuthDomainService;
import com.marine.management.modules.auth.domain.AuthResult;
import com.marine.management.modules.auth.domain.LoginCommand;
import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.auth.presentation.UserResponse;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.domain.UserRepository;
import com.marine.management.shared.exceptions.AuthenticationFailedException;
import com.marine.management.shared.exceptions.UnauthorizedAccessException;
import com.marine.management.shared.exceptions.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final AuthDomainService authDomainService;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       AuthDomainService authDomainService,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.authDomainService = authDomainService;
        this.jwtUtil = jwtUtil;
    }

    public AuthResult login(LoginCommand command){
        User user = findUserByUsernameOrThrow(command.username());

        boolean authenticated = authDomainService.authenticateUser(user, command.password());

        if (!authenticated) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        if (!authDomainService.canGenerateTokenForUser(user)){
            throw new UnauthorizedAccessException("User cannot generate token");
        }

        String token =  jwtUtil.generateToken(user);
        UserResponse userResponse = UserResponse.from(user);

        return new AuthResult(token, userResponse);
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public String extractUsernameFromToken(String token){
        return jwtUtil.extractUsername(token);
    }

    @Transactional
    public User getUserFromToken(String token){
        String username = jwtUtil.extractUsername(token);
        return findUserByUsernameOrThrow(username);
    }

    private User findUserByUsernameOrThrow(String username){
        return userRepository.findByUsername(username).orElseThrow(() -> UserNotFoundException.withUsername(username));
    }
}
