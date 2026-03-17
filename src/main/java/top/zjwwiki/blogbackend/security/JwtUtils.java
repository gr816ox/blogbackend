package top.zjwwiki.blogbackend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import top.zjwwiki.blogbackend.config.JwtProperties;

@Component
/**
 * JWT 工具类：负责生成、解析与校验 Token。
 *
 * 基于 JJWT，使用 HMAC（兼容 HS256）对 claims 进行签名。
 *
 * 初学者可把 JWT 理解成“后端签名的身份证”：
 * - 后端发证（generateToken）
 * - 后端验真（extract/validate）
 */
public class JwtUtils {
	private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
	private final JwtProperties jwtProperties;

	public JwtUtils(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	/**
	 * 读取 subject 字段，作为系统用户名。
	 *
	 * 约定：我们把 username 放在 JWT 的 subject 中。
	 */
	public String extractUsername(String token) {
		try {
			if (token == null || token.isBlank()) {
				logger.warn("[JWT] 尝试从空的 token 提取用户名");
				return null;
			}
			String username = extractClaim(token, Claims::getSubject);
			logger.debug("[JWT] 成功从 token 提取用户名: {}", username);
			return username;
		} catch (ExpiredJwtException e) {
			logger.warn("[JWT] Token 已过期: {}", e.getMessage());
			return null;
		} catch (MalformedJwtException e) {
			logger.warn("[JWT] Token 格式错误，无法解析: {}", e.getMessage());
			return null;
		} catch (UnsupportedJwtException e) {
			logger.warn("[JWT] Token 类型不支持: {}", e.getMessage());
			return null;
		} catch (IllegalArgumentException e) {
			logger.warn("[JWT] Token 为空或无效: {}", e.getMessage());
			return null;
		} catch (JwtException e) {
			logger.warn("[JWT] Token 校验失败: {}", e.getMessage());
			return null;
		} catch (Exception e) {
			logger.error("[JWT] 提取用户名时发生未预期的错误", e);
			return null;
		}
	}

	/**
	 * 通用 claims 提取方法，避免重复解析逻辑。
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		try {
			Claims claims = extractAllClaims(token);
			if (claims == null) {
				logger.warn("[JWT] 从 token 提取的 claims 为 null");
				return null;
			}
			T result = claimsResolver.apply(claims);
			logger.debug("[JWT] 成功提取 claim");
			return result;
		} catch (Exception e) {
			logger.error("[JWT] 提取 claim 时发生错误", e);
			return null;
		}
	}

	/**
	 * 生成签名后的 JWT：用户名写入 subject，并附带过期时间。
	 *
	 * 生成后的 token 通常交给前端保存，后续请求放入 Authorization 头。
	 */
	public String generateToken(UserDetails userDetails) {
		return generateToken(userDetails.getUsername());
	}

	public String generateToken(String username) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

		return Jwts.builder()
				.subject(username)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(getSigningKey())
				.compact();
	}

	/**
	 * Token 有效条件：
	 * - subject 与当前用户一致
	 * - 当前时间未超过过期时间
	 */
	public boolean validateToken(String token, UserDetails userDetails) {
		try {
			if (token == null || token.isBlank()) {
				logger.warn("[JWT验证] Token 为空或 null");
				return false;
			}
			if (userDetails == null) {
				logger.warn("[JWT验证] UserDetails 为 null");
				return false;
			}

			String username = extractUsername(token);
			if (username == null) {
				logger.warn("[JWT验证] 无法从 token 提取用户名 (token 格式错误或已过期)");
				return false;
			}

			String dbUsername = userDetails.getUsername();
			if (!username.equals(dbUsername)) {
				logger.warn("[JWT验证] 用户名不匹配 - Token中: {}, 数据库中: {}", username, dbUsername);
				return false;
			}

			if (isTokenExpired(token)) {
				logger.warn("[JWT验证] Token 已过期, 用户: {}", username);
				return false;
			}

			logger.info("[JWT验证] Token 验证成功, 用户: {}", username);
			return true;
		} catch (ExpiredJwtException e) {
			logger.warn("[JWT验证] Token 已过期: {}", e.getMessage());
			return false;
		} catch (SecurityException e) {
			logger.warn("[JWT验证] Token 签名验证失败: {}", e.getMessage());
			return false;
		} catch (JwtException e) {
			logger.warn("[JWT验证] JWT 异常: {}", e.getMessage());
			return false;
		} catch (Exception e) {
			logger.error("[JWT验证] 发生未预期的错误", e);
			return false;
		}
	}

	private boolean isTokenExpired(String token) {
		try {
			if (token == null || token.isBlank()) {
				logger.warn("[JWT过期检查] Token 为空");
				return true;
			}
			Date expiration = extractClaim(token, Claims::getExpiration);
			if (expiration == null) {
				logger.warn("[JWT过期检查] 无法获取 token 的过期时间");
				return true;
			}
			boolean isExpired = expiration.before(new Date());
			if (isExpired) {
				logger.debug("[JWT过期检查] Token 已过期, 过期时间: {}", expiration);
			}
			return isExpired;
		} catch (ExpiredJwtException e) {
			logger.warn("[JWT过期检查] 捕获到过期异常: {}", e.getMessage());
			return true;
		} catch (Exception e) {
			logger.error("[JWT过期检查] 检查过期时间时发生错误", e);
			return true;
		}
	}

	/**
	 * 解析并验签 JWT，返回 claims 载荷。
	 */
	private Claims extractAllClaims(String token) {
		try {
			if (token == null || token.isBlank()) {
				logger.warn("[JWT解析] Token 为空");
				return null;
			}
			Claims claims = Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			logger.debug("[JWT解析] 成功解析 token");
			return claims;
		} catch (ExpiredJwtException e) {
			logger.warn("[JWT解析] Token 已过期: {}", e.getMessage());
			return null;
		} catch (MalformedJwtException e) {
			logger.warn("[JWT解析] Token 格式错误，无法解析: {}", e.getMessage());
			return null;
		} catch (UnsupportedJwtException e) {
			logger.warn("[JWT解析] 不支持的 JWT 类型: {}", e.getMessage());
			return null;
		} catch (SecurityException e) {
			logger.warn("[JWT解析] Token 签名验证失败 - 可能是密钥不匹配: {}", e.getMessage());
			return null;
		} catch (IllegalArgumentException e) {
			logger.warn("[JWT解析] Token 为空或无效: {}", e.getMessage());
			return null;
		} catch (JwtException e) {
			logger.warn("[JWT解析] JWT 解析异常: {}", e.getMessage());
			return null;
		} catch (Exception e) {
			logger.error("[JWT解析] 解析 token 时发生未预期的错误", e);
			return null;
		}
	}

	/**
	 * 将配置中的密钥字符串转换为 HMAC 签名密钥对象。
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
