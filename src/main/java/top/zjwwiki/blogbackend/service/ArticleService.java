package top.zjwwiki.blogbackend.service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import top.zjwwiki.blogbackend.dto.ArticleSummaryResponse;
import top.zjwwiki.blogbackend.dto.ArticleRequest;
import top.zjwwiki.blogbackend.dto.PageResponse;
import top.zjwwiki.blogbackend.generated.Article;
import top.zjwwiki.mapper.ArticleMapper;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ArticleService {
    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public Article getByIdOrThrow(Long articleId) {
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            throw new ResponseStatusException(NOT_FOUND, "Article not found");
        }
        return article;
    }

    public PageResponse<ArticleSummaryResponse> getPage(String keyword, String category, Integer page, Integer size) {
        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size);
        String normalizedKeyword = normalizeFilter(keyword);
        String normalizedCategory = normalizeFilter(category);
        int offset = (resolvedPage - 1) * resolvedSize;

        long total = articleMapper.countArticleSummaries(normalizedKeyword, normalizedCategory);
        List<ArticleSummaryResponse> items = total == 0
                ? List.of()
                : articleMapper.selectArticleSummaries(normalizedKeyword, normalizedCategory, offset, resolvedSize);

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / resolvedSize);
        boolean hasNext = resolvedPage < totalPages;
        return new PageResponse<>(items, resolvedPage, resolvedSize, total, totalPages, hasNext);
    }

    public Article create(ArticleRequest request) {
        Article article = new Article();
        applyRequest(article, request);
        article.setCreatedAt(new Date());
        articleMapper.insertSelective(article);
        return getByIdOrThrow(article.getArticleId());
    }

    public void delete(Long articleId) {
        getByIdOrThrow(articleId);
        articleMapper.deleteByPrimaryKey(articleId);
    }

    public Article update(Long articleId, ArticleRequest request) {
        Article article = getByIdOrThrow(articleId);
        applyRequest(article, request);
        articleMapper.updateByPrimaryKeyWithBLOBs(article);
        return getByIdOrThrow(articleId);
    }

    private void applyRequest(Article article, ArticleRequest request) {
        validateRequest(request);
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent().trim());
        article.setSummary(normalizeNullable(request.getSummary()));
        article.setCategory(normalizeNullable(request.getCategory()));
    }

    private void validateRequest(ArticleRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("title is required");
        }
        if (request.getTitle().trim().length() > 200) {
            throw new IllegalArgumentException("title must be at most 200 characters");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content is required");
        }
        if (StringUtils.hasText(request.getCategory()) && request.getCategory().trim().length() > 100) {
            throw new IllegalArgumentException("category must be at most 100 characters");
        }
        if (StringUtils.hasText(request.getSummary()) && request.getSummary().trim().length() > 500) {
            throw new IllegalArgumentException("summary must be at most 500 characters");
        }
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeFilter(String value) {
        return normalizeNullable(value);
    }

    private int resolvePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return 10;
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        return size;
    }
}
