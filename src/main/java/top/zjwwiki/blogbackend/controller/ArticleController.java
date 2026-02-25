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
        Article newArticle = new Article();
        newArticle.setTitle(request.getTitle());
        newArticle.setContent(request.getContent());
        newArticle.setSummary(request.getSummary());
        newArticle.setCategory(request.getCategory());
        articleService.save(newArticle);
    }
}
