package top.zjwwiki.blogbackend.config; // ← 包路径必须匹配目录结构

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // ← 确保有此注解
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // ⚠️ 修正1：删除多余空格！原代码 "https://your-vue-domain.com  " 有空格会失效
<<<<<<< HEAD
                .allowedOrigins("https://zjwwiki.top","https://www.zjwwiki.top", "http://localhost:5173", "http://localhost:8081") // ← 开发环境常用端口
=======
                .allowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost:8081") // ← 开发环境常用端口
>>>>>>> origin/feature-deleteandupdate
                // ⚠️ 修正2：添加必要配置（否则 OPTIONS 预检失败）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 允许携带 Cookie（如需要）
                .maxAge(3600); // 预检请求缓存时间（秒）
    }
}