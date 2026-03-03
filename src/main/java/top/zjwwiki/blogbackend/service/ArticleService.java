package top.zjwwiki.blogbackend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import top.zjwwiki.blogbackend.generated.Article;
import top.zjwwiki.mapper.ArticleMapper;
import top.zjwwiki.blogbackend.generated.*;

@Service
public class ArticleService {
    @Autowired
    private ArticleMapper articleMapper;

    public Article getById(Long articleId) {
        return articleMapper.selectByPrimaryKey(articleId);
    }

    public List<Article> getall() {
        return articleMapper.selectArticleList();
    }

    public void save(Article article) {
        articleMapper.insert(article);
    }

    public void delete(Long articleId) {
        articleMapper.deleteByPrimaryKey(articleId);
    }

    public void update(Article article) {
        articleMapper.updateByPrimaryKeyWithBLOBs(article);
    }
}
