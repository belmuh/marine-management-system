package com.marine.management.modules.auth.domain;

import com.marine.management.modules.auth.presentation.UserResponse;
import com.marine.management.modules.users.domain.User;

public class AuthResult {
    private final String token;
    private final UserResponse user;

    public AuthResult(String token, UserResponse user){
        this.token = token;
        this.user = user;
    }

    public String getToken(){ return token;}
    public UserResponse getUser() { return user; }
}
