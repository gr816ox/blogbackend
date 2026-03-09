package top.zjwwiki.blogbackend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * 通用 claims 提取方法，避免重复解析逻辑。
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
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
		String username = extractUsername(token);
		// 这里没有校验“用户权限是否变化”等业务条件，后续可按需要扩展。
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		// exp 是 JWT 标准字段，表示过期时间点。
		Date expiration = extractClaim(token, Claims::getExpiration);
		return expiration.before(new Date());
	}

	/**
	 * 解析并验签 JWT，返回 claims 载荷。
	 */
	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * 将配置中的密钥字符串转换为 HMAC 签名密钥对象。
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
