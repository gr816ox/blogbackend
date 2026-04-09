package top.zjwwiki.blogbackend.dto;

import java.util.Date;

public record ArticleSummaryResponse(Long id, String title, String category, String summary, Date createdAt) {
}