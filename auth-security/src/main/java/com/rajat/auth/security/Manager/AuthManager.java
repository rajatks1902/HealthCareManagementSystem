package com.rajat.auth.security.Manager;

import com.rajat.auth.security.Dao.RoleRepository;
import com.rajat.auth.security.Dao.UserRepository;
import com.rajat.auth.security.Security.jwt.JwtUtils;
import com.rajat.auth.security.Security.services.UserDetailsImpl;
import com.rajat.auth.security.models.Role;
import com.rajat.auth.security.models.User;
import com.rajat.auth.security.models.UserRole;
import com.rajat.auth.security.payload.request.LoginRequest;
import com.rajat.auth.security.payload.request.SignupRequest;
import com.rajat.auth.security.payload.response.JwtResponse;
import com.rajat.auth.security.payload.response.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthManager {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Authenticate a user and return a JWT token with user details.
     *
     * @param loginRequest The login request containing username and password.
     * @return A JwtResponse containing the JWT token and user details.
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.id(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
    }

    /**
     * Register a new user account.
     *
     * @param signUpRequest The signup request containing user details.
     * @return A MessageResponse indicating success or error.
     */
    public MessageResponse registerUser(SignupRequest signUpRequest) {

        Optional<User> userOptional = userRepository.findById(signUpRequest.username());
        if (userOptional.isPresent()) {
            return new MessageResponse("Error: Username is already taken!");
        }

//        if (userRepository.existsByEmail(signUpRequest.email())) {
//            return new MessageResponse("Error: Email is already in use!");
//        }

        User user = new User(signUpRequest.username(), signUpRequest.email(), encoder.encode(signUpRequest.password()));

        Set<String> strRoles = signUpRequest.roles();
        Set<String> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findById(String.valueOf(UserRole.ROLE_PATIENT))
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole.toString());
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findById(String.valueOf(UserRole.ROLE_ADMIN))
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole.toString());
                        break;
                    case "doctor":
                        Role doctorRole = roleRepository.findById(String.valueOf(UserRole.ROLE_DOCTOR))
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(doctorRole.toString());
                        break;
                    default:
                        Role userRole = roleRepository.findById(String.valueOf(UserRole.ROLE_PATIENT))
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole.toString());
                }
            });
        }

        user.setRoles(roles);
        user.setId(user.getUsername());
        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }
}
