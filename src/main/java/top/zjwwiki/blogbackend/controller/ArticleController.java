package top.zjwwiki.blogbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import top.zjwwiki.blogbackend.dto.ArticleRequest;
import top.zjwwiki.blogbackend.generated.Article;
import top.zjwwiki.blogbackend.service.ArticleService;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @GetMapping(value = "/{id}", produces = "application/json;charset=utf-8")
    public Article get(@PathVariable Long id) {
        return articleService.getById(id);
    }

    @GetMapping("")
    public List<Article> getall() {
        return articleService.getall();
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public void create(@RequestBody ArticleRequest request) {
        System.out.println("123213");
        Article newArticle = new Article();
        newArticle.setTitle(request.getTitle());
        newArticle.setContent(request.getContent());
        newArticle.setSummary(request.getSummary());
        newArticle.setCategory(request.getCategory());
        articleService.save(newArticle);
    }

    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public void update(@PathVariable Long id, @RequestBody ArticleRequest request) {
        Article article = articleService.getById(id);
        if (article != null) {
            System.out.println("asdfasf");
            article.setArticleId(id);
            article.setTitle(request.getTitle());
            article.setContent(request.getContent());
            article.setSummary(request.getSummary());
            article.setCategory(request.getCategory());
            articleService.update(article);
        }
    }

    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable Long id) {
        articleService.delete(id);
    }
}
