package uz.portfolio.taskmanagement.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.portfolio.taskmanagement.auth.dto.LoginRequest;
import uz.portfolio.taskmanagement.auth.dto.RegisterRequest;
import uz.portfolio.taskmanagement.auth.dto.TokenResponse;
import uz.portfolio.taskmanagement.auth.dto.UserResponse;
import uz.portfolio.taskmanagement.auth.exception.EmailAlreadyRegisteredException;
import uz.portfolio.taskmanagement.auth.exception.InvalidCredentialsException;
import uz.portfolio.taskmanagement.auth.exception.InvalidPasswordException;
import uz.portfolio.taskmanagement.config.JwtProperties;
import uz.portfolio.taskmanagement.user.User;
import uz.portfolio.taskmanagement.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateBcryptPasswordLength(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        String passwordHash = passwordEncoder.encode(request.password());

        try {
            User savedUser = userRepository.saveAndFlush(new User(email, passwordHash));
            return toUserResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            // Also handles the rare case where two registrations race past existsByEmail.
            throw new EmailAlreadyRegisteredException(email);
        }
    }

    public TokenResponse login(LoginRequest request) {
        validateBcryptPasswordLength(request.password());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeEmail(request.email()),
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            // Do not disclose whether the email or password was incorrect.
            throw new InvalidCredentialsException();
        }

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("USER");

        String accessToken = jwtService.generateAccessToken(authentication.getName(), role);

        return new TokenResponse(
                accessToken,
                "Bearer",
                jwtProperties.accessTokenTtl().toSeconds()
        );
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
