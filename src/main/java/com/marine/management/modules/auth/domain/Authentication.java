package com.marine.management.modules.auth.domain;

import com.marine.management.modules.users.domain.User;

public class Authentication {

    private static boolean userCanAuthenticate(User user){
        // MVP'de tüm kullanıcılar login olabilir
        // Future: suspended, banned users kontrolü eklenebilir
        return true;
    }

    public static boolean canGenerateTokenForUser(User user){
        return user != null && userCanAuthenticate(user);
    }
}