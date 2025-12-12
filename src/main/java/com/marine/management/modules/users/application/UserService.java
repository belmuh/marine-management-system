package com.marine.management.modules.users.application;

import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.exceptions.CategoryNotFoundException;
import com.marine.management.shared.exceptions.UserNotFoundException;
import com.marine.management.shared.exceptions.UserRegistrationException;
import com.marine.management.shared.exceptions.UserUpdateException;
import com.marine.management.shared.kernel.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // === USER REGISTRATION ===

    @Transactional
    public User registerUser(String username, String email, String plainPassword){
        validateUserAvailableForRegistration(username, email);
        validatePlainPassword(plainPassword);

        String encodedPassword = passwordEncoder.encode(plainPassword);
        User user = User.create(username, email, encodedPassword);

        return userRepository.save(user);
    }

    @Transactional
    public User registerUserWithRole(String username, String email, String plainPassword, Role role){
        validateUserAvailableForRegistration(username, email);
        validatePlainPassword(plainPassword);

        String encodedPassword = passwordEncoder.encode(plainPassword);
        User user = User.createWithHashedPassword(username, email, encodedPassword, role);

        return userRepository.save(user);
    }

    // === USER RETRIEVAL ===

    public Optional<User> getUserById(UUID userId){
        return userRepository.findById(userId);
    }

    public User getUserByIdOrThrow(UUID userId){
        return findUserOrThrow(userId);
    }

    public Optional<User> getUserByUsername(String username){
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public boolean userExists(String username, String email){
        return userRepository.existsByUsername(username) || userRepository.existsByEmail(email);
    }

    // === PROFILE MANAGEMENT ===

    @Transactional
    public User updateUserProfile(UUID userId, String newUsername, String newEmail){
        User user = findUserOrThrow(userId);

        if(!user.getUsername().equals(newUsername)){
            validateUsernameAvailableForUpdate(newUsername);
        }
        if(!user.getEmail().equals(newEmail)){
            validateEmailAvailableForUpdate(newEmail);
        }

        user.updateProfile(newUsername, newEmail);
        return user;
    }

    @Transactional
    public void changeUserPassword(UUID userId, String newPassword){
        validatePlainPassword(newPassword);

        User user = findUserOrThrow(userId);
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);
    }

    @Transactional
    public User activate(UUID id) {
        User user = getByIdOrThrow(id);
        user.activate();
        return user;
    }

    @Transactional
    public User deactivate(UUID id) {
        User user = getByIdOrThrow(id);
        user.deactivate();
        return user;
    }

    @Transactional
    public void deleteUser(UUID userId){
        User user = findUserOrThrow(userId);
        userRepository.delete(user);
    }

    // === ROLE MANAGEMENT ===

    @Transactional
    public User updateUserRole(UUID userId, Role newRole){
        User user = findUserOrThrow(userId);
        user.changeRole(newRole);
        return user;
    }

    // === VALIDATION METHODS ===
    private void validateUserAvailableForRegistration(String username, String email) {
        validateUsernameAvailableForRegistration(username);
        validateEmailAvailableForRegistration(email);
    }

    private void validateUsernameAvailableForRegistration(String username) {
        if (userRepository.existsByUsername(username)) {
            throw UserRegistrationException.usernameAlreadyExists(username);
        }
    }

    private void validateEmailAvailableForRegistration(String email) {
        if (userRepository.existsByEmail(email)) {
            throw UserRegistrationException.emailAlreadyExists(email);
        }
    }

    // UPDATE VALIDATIONS (UserUpdateException)
    private void validateUsernameAvailableForUpdate(String username) {
        if (userRepository.existsByUsername(username)) {
            throw UserUpdateException.usernameTaken(username);
        }
    }

    private void validateEmailAvailableForUpdate(String email) {
        if (userRepository.existsByEmail(email)) {
            throw UserUpdateException.emailRegistered(email);
        }
    }

    private User findUserOrThrow(UUID userId){
        return userRepository.findById(userId).orElseThrow(()-> UserNotFoundException.withId(userId));
    }


    private void validatePlainPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

    }

    // === UTILITY METHODS ===

    public long getUserCount(){
        return userRepository.count();
    }

    public  boolean isUsernameAvailable(String username){
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email){
        return !userRepository.existsByEmail(email);
    }

    private User getByIdOrThrow(UUID id){
        return userRepository.findById(id).orElseThrow(() -> CategoryNotFoundException.withId(id));
    }
}
