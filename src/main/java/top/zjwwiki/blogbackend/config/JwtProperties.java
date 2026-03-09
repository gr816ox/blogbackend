package top.zjwwiki.blogbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
/**
 * JWT 外部配置对象，会从 application.properties 绑定。
 *
 * 典型配置项：
 * - jwt.secret
 * - jwt.expiration-ms
 */
public class JwtProperties {

    /**
     * HS256 建议至少 32 个字符。
        * 生产环境应使用足够随机、不可预测的长密钥。
     */
    private String secret = "change-me-to-a-long-random-secret-key-32+";

        /**
        * Token 过期时长，单位毫秒。
        * 例如 86400000 = 24 小时。
        */
    private long expirationMs = 86400000;

    public String getSecret() {
        // 用于 HMAC 签名和验签的密钥。
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        // Token 过期时长（毫秒）。
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}