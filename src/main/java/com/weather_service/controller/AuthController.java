package com.weather_service.controller;

import com.weather_service.security.JwtUtil;
import com.weather_service.swagger.HTTPResponseMessages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Logs in user, returns authentication token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HTTPResponseMessages.HTTP_200,
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = HTTPResponseMessages.HTTP_400, content = @Content),
            @ApiResponse(responseCode = "500", description = HTTPResponseMessages.HTTP_500, content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        var username = body.get("username");
        var password = body.get("password");
        log.info("Attempting to log in user with username: {}", username);
        var auth = new UsernamePasswordAuthenticationToken(username, password);
        authManager.authenticate(auth);

        String token = jwtUtil.generateToken(username);
        log.debug("User {} logged in successfully", username);
        return ResponseEntity.ok(token);
    }
}
