package top.zjwwiki.blogbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zjwwiki.blogbackend.dto.ArticleRequest;
import top.zjwwiki.blogbackend.dto.ArticleSummaryResponse;
import top.zjwwiki.blogbackend.dto.PageResponse;
import top.zjwwiki.blogbackend.generated.Article;
import top.zjwwiki.blogbackend.service.ArticleService;

@RestController
@RequestMapping
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping(value = {"/api/articles/{id}", "/api/public/articles/{id}"}, produces = "application/json;charset=utf-8")
    public Article get(@PathVariable Long id) {
        return articleService.getByIdOrThrow(id);
    }

    @GetMapping({"/api/articles", "/api/public/articles"})
    public PageResponse<ArticleSummaryResponse> getAll(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        return articleService.getPage(keyword, category, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = {"/api/articles", "/api/private/articles"}, consumes = "application/json", produces = "application/json")
    public ResponseEntity<Article> create(@RequestBody ArticleRequest request) {
        Article article = articleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(article);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = {"/api/articles/{id}", "/api/private/articles/{id}"}, consumes = "application/json", produces = "application/json")
    public Article update(@PathVariable Long id, @RequestBody ArticleRequest request) {
        return articleService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = {"/api/articles/{id}", "/api/private/articles/{id}"})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
