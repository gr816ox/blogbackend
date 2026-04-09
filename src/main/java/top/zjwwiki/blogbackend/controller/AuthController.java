package top.zjwwiki.blogbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import top.zjwwiki.blogbackend.generated.User;
import top.zjwwiki.blogbackend.security.JwtUtils;
import top.zjwwiki.blogbackend.service.UserService;

@RestController
@RequestMapping("/api/auth")
/**
 * 认证相关接口。
 *
	 * - register：注册新账号
 * - login：校验账号密码并签发 JWT
 *
 * 初学者可先记住一句话：
 * 1) register 负责“创建用户”
 * 2) login 负责“验证身份 + 发放通行证(Token)”
 */
public class AuthController {

	/**
	 * AuthenticationManager 是 Spring Security 的“认证总入口”。
	 * 你把用户名/密码交给它，它会调用 UserDetailsService + PasswordEncoder 完成校验。
	 */
	private final AuthenticationManager authenticationManager;

	/**
	 * JWT 工具：负责生成 token。
	 */
	private final JwtUtils jwtUtils;

	private final UserService userService;

	public AuthController(AuthenticationManager authenticationManager,
						  JwtUtils jwtUtils,
						  UserService userService) {
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
		this.userService = userService;
	}

	@PostMapping("/register")
	public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest request) {
		validateRegisterRequest(request);
		userService.register(request.username(), request.password());
		return ResponseEntity.status(CREATED).body(new MessageResponse("Register success"));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		validateLoginRequest(request);
		try {
			// 第 1 步：构造“用户名+密码”的认证请求对象。
			// UsernamePasswordAuthenticationToken 在这里还只是“待认证”的凭据载体。
			// 通过 AuthenticationManager 进行账号密码认证。
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.username(), request.password()));

			// 第 2 步：认证通过后，直接基于用户名签发 JWT。
			// 之后前端要把这个 token 放到 Authorization 头里发给后端。
			String token = jwtUtils.generateToken(request.username());
			User user = userService.findByUsername(request.username());
			if (user == null) {
				throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
			}

			return ResponseEntity.ok(new LoginResponse(token, toUserPayload(user)));
		} catch (BadCredentialsException ex) {
			throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
		}
	}

	@GetMapping("/me")
	public UserPayload me(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
		}

		User user = userService.findByUsername(authentication.getName());
		if (user == null) {
			throw new ResponseStatusException(UNAUTHORIZED, "User not found");
		}

		return toUserPayload(user);
	}

	private void validateLoginRequest(LoginRequest request) {
		validateUsername(request.username(), 50, 1);
		validatePassword(request.password());
	}

	private void validateRegisterRequest(RegisterRequest request) {
		validateUsername(request.username(), 50, 3);
		validatePassword(request.password());
	}

	private void validateUsername(String username, int maxLength, int minLength) {
		if (!StringUtils.hasText(username)) {
			throw new IllegalArgumentException("username is required");
		}

		String trimmedUsername = username.trim();
		if (trimmedUsername.length() < minLength || trimmedUsername.length() > maxLength) {
			throw new IllegalArgumentException(
					"username length must be between " + minLength + " and " + maxLength);
		}
	}

	private void validatePassword(String password) {
		if (!StringUtils.hasText(password)) {
			throw new IllegalArgumentException("password is required");
		}

		String trimmedPassword = password.trim();
		if (trimmedPassword.length() < 6 || trimmedPassword.length() > 72) {
			throw new IllegalArgumentException("password length must be between 6 and 72");
		}
	}

	private UserPayload toUserPayload(User user) {
		String role = user.getRole();
		if (role == null || role.isBlank()) {
			role = "USER";
		} else if (role.startsWith("ROLE_")) {
			role = role.substring("ROLE_".length());
		}

		return new UserPayload(user.getUserId(), user.getUsername(), role);
	}

	/**
	 * 登录请求体。
	 * 对应 JSON:
	 * {
	 *   "username": "你的用户名",
	 *   "password": "你的密码"
	 * }
	 */
	public record LoginRequest(
			String username,
			String password) {
	}

	/**
	 * 注册请求体。
	 *
	 * 对应 JSON:
	 * {
	 *   "username": "新用户名",
	 *   "password": "新密码"
	 * }
	 */
	public record RegisterRequest(
			String username,
			String password) {
	}

	public record UserPayload(Integer id, String username, String role) {
	}

	public record LoginResponse(String token, UserPayload user) {
	}

	public record MessageResponse(String message) {
	}
}
