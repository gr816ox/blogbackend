package top.zjwwiki.blogbackend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import top.zjwwiki.blogbackend.generated.User;
import top.zjwwiki.blogbackend.security.CustomUserDetailsService;
import top.zjwwiki.blogbackend.security.JwtUtils;
import top.zjwwiki.blogbackend.service.UserService;

@RestController
@RequestMapping("/api/auth")
/**
 * 认证相关接口。
 *
 * - register：注册新账号（示例阶段使用内存存储）
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
	 * 这里的 service 负责用户存取逻辑（当前是内存实现）。
	 */
	private final CustomUserDetailsService customUserDetailsService;

	/**
	 * JWT 工具：负责生成 token。
	 */
	private final JwtUtils jwtUtils;

	private final UserService userService;

	public AuthController(AuthenticationManager authenticationManager,
						  CustomUserDetailsService customUserDetailsService,
						  JwtUtils jwtUtils,
						  UserService userService) {
		this.authenticationManager = authenticationManager;
		this.customUserDetailsService = customUserDetailsService;
		this.jwtUtils = jwtUtils;
		this.userService = userService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
		try {
			// request.username()/request.password() 来自前端传入 JSON。
			// 例如：{"username":"tom","password":"123456"}
			// 用户名唯一性检查与密码加密交由 service 处理。
			customUserDetailsService.register(request.username(), request.password());

			// 注册成功后返回 200 与提示信息。
			return ResponseEntity.ok(Map.of("message", "Register success"));
		} catch (IllegalArgumentException ex) {
			// 409 表示资源冲突，这里对应“用户名已存在”。
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
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
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
			}

			String role = user.getRole();
			if (role == null || role.isBlank()) {
				role = "USER";
			} else if (role.startsWith("ROLE_")) {
				role = role.substring("ROLE_".length());
			}

			Map<String, Object> userPayload = Map.of(
					"id", user.getUserId(),
					"username", user.getUsername(),
					"role", role);

			return ResponseEntity.ok(Map.of(
					"token", token,
					"user", userPayload));
		} catch (BadCredentialsException ex) {
			// 对外统一返回 401，避免暴露过多内部认证细节。
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
		}
	}

	/**
	 * 登录请求体。
	 *generateToken(userDetails);
			return ResponseEntity.ok
	 * 对应 JSON:
	 * {
	 *   "username": "你的用户名",
	 *   "password": "你的密码"
	 * }
	 */
	public record LoginRequest(String username, String password) {
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
	public record RegisterRequest(String username, String password) {
	}
}
