package com.rajat.auth.security.Resources;


import com.rajat.auth.security.Manager.AuthManager;
import com.rajat.auth.security.payload.request.LoginRequest;
import com.rajat.auth.security.payload.request.SignupRequest;
import com.rajat.auth.security.payload.response.JwtResponse;
import com.rajat.auth.security.payload.response.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthManager authManager;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		JwtResponse jwtResponse = authManager.authenticateUser(loginRequest);
		return ResponseEntity.ok(jwtResponse);
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		MessageResponse response = authManager.registerUser(signUpRequest);

		if (response.message().startsWith("Error:")) {
			return ResponseEntity.badRequest().body(response);
		}

		return ResponseEntity.ok(response);
	}
}
