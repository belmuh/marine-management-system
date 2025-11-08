package com.marine.management.modules.auth.domain;

import com.marine.management.modules.users.domain.User;
import org.springframework.stereotype.Service;

@Service
public class AuthDomainService {

    public boolean authenticateUser(User user, String inputPassword){
        if (user == null){
            return false;
        }
        return user.credentialsMatch(inputPassword) && userCanAuthenticate(user);
    }

    private boolean userCanAuthenticate(User user){
        // MVP'de tüm kullanıcılar login olabilir
        // Future: suspended, banned users kontrolü eklenebilir
        return true;
    }

    public boolean canGenerateTokenForUser(User user){
        return user != null && user.canManageFinancialEntries();
    }
}
