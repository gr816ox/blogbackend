package top.zjwwiki.blogbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@MapperScan("top.zjwwiki.mapper")
public class BlogbackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogbackendApplication.class, args);
    }

}
