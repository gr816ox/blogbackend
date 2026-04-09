package top.zjwwiki.blogbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import top.zjwwiki.blogbackend.security.JwtAuthenticationFilter;
import top.zjwwiki.blogbackend.security.RestAccessDeniedHandler;
import top.zjwwiki.blogbackend.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
/**
 * Spring Security 核心配置。
 *
 * 该类主要串联 JWT 鉴权的三个核心部分：
 * 1) 无状态的安全过滤链
 * 2) 密码加密策略
 * 3) 登录接口使用的 AuthenticationManager
 *
 * 你可以把这个类理解为“系统安保总开关”：
 * - 哪些 URL 可以匿名访问
 * - 哪些 URL 必须登录
 * - 请求进入系统后先走哪些安全过滤器
 */
public class SecurityConfig {

    /**
     * 安全过滤链是 Spring Security 6 的核心配置方式。
     * 每个 HTTP 请求都会经过这里定义的一系列安全规则。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                                                   RestAccessDeniedHandler restAccessDeniedHandler)
            throws Exception {
        // JWT 接口通常是无状态的，因此需要调整 CSRF 和 Session 的默认行为。
        http
            // 启用 CORS，使用 CorsConfig 中的规则处理跨域请求。
            .cors(Customizer.withDefaults())
            // 对于 Token 鉴权场景，通常不依赖服务端 Session，因此关闭 CSRF。
            .csrf(csrf -> csrf.disable())
            // 禁止创建/使用 HttpSession；每个请求都应自行携带 Token。
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 登录注册和公开接口放行。
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/**", "/api/public/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/**", "/api/private/articles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/articles/**", "/api/private/articles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/articles/**", "/api/private/articles/**").hasRole("ADMIN")
                // 其余接口都要求已认证。
                .anyRequest().authenticated())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler))
            // 关键点：JWT 过滤器要在用户名密码过滤器之前执行。
            // 原因：我们不是表单登录，而是希望优先从请求头里的 Token 还原登录态。
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 会自动加盐，是当前推荐的默认密码哈希方案。
        // 切记：数据库里存的应该是哈希值，不是明文密码。
        return new BCryptPasswordEncoder();
    }

	/**
	 * 提供认证管理器，供 AuthController 登录接口调用。
	 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        // 复用 Spring 根据已配置 Provider 组装出的 AuthenticationManager。
        return authenticationConfiguration.getAuthenticationManager();
    }
}

