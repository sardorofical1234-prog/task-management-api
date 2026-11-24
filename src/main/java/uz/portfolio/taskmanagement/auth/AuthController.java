package uz.portfolio.taskmanagement.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.portfolio.taskmanagement.auth.dto.LoginRequest;
import uz.portfolio.taskmanagement.auth.dto.RegisterRequest;
import uz.portfolio.taskmanagement.auth.dto.TokenResponse;
import uz.portfolio.taskmanagement.auth.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register an account and obtain a JWT access token")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT access token")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
