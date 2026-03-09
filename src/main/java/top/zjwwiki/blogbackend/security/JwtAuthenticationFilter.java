package top.zjwwiki.blogbackend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
/**
 * 每请求执行一次的 JWT 认证过滤器。
 *
 * 主要职责：
 * - 从 Authorization 请求头提取 Bearer Token
 * - 从 Token 中解析用户名
 * - 加载用户信息并校验 Token 是否有效/过期
 * - 将认证信息写入 SecurityContext，供后续接口识别登录态
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

		// ======== 步骤 1：从请求头拿 Token ========

        // 请求头格式应为：Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

			// ======== 步骤 2：从 Token 解析用户名 ========

            // 从 Token 解析 subject（用户名）；签名非法时 JwtUtils 可能抛异常。
            String username = jwtUtils.extractUsername(token);

			// ======== 步骤 3：如果当前上下文尚未认证，则尝试认证 ========
			// 某些场景下，前置过滤器可能已写入认证信息，这里避免重复覆盖。
            // 避免覆盖当前请求链中已存在的认证信息。
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				// ======== 步骤 4：校验 Token 与用户是否匹配且未过期 ========

                // 只有用户名匹配且 Token 未过期时，才认为 Token 有效。
                if (jwtUtils.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // 挂载请求维度信息（如来源 IP），便于审计与排查。
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 写入上下文后，Spring Security 会将本次请求视为已认证。
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

		// ======== 步骤 5：继续后续过滤器与控制器处理 ========
        // 无论是否携带 Token，都继续执行后续过滤器链。
        filterChain.doFilter(request, response);
    }
}