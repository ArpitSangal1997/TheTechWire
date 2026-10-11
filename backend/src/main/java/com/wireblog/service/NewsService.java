package com.wireblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wireblog.dto.NewsHeadlineResponse;
import com.wireblog.model.NewsHeadline;
import com.wireblog.repository.NewsHeadlineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GK / news section. Pulls live headlines from an external provider (NewsAPI.org
 * by default — swap the base URL + mapping below for GNews, NewsData.io, etc.)
 * and caches them locally so the site stays fast and works even if the
 * provider is briefly down. Only headline + source link is ever shown —
 * full articles always open on the original publisher's site.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final List<String> SUPPORTED_CATEGORIES = List.of("general", "business", "technology", "sports", "science", "health");
    private static final Map<String, String> RSS_FEEDS = Map.of(
            "general", "https://feeds.bbci.co.uk/news/world/asia/india/rss.xml",
            "business", "https://feeds.bbci.co.uk/news/business/rss.xml",
            "technology", "https://feeds.bbci.co.uk/news/technology/rss.xml",
            "sports", "https://feeds.bbci.co.uk/sport/rss.xml",
            "science", "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
            "health", "https://feeds.bbci.co.uk/news/health/rss.xml");

    private final NewsHeadlineRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.news.api-key:}")
    private String apiKey;

    @Value("${app.news.base-url:https://newsapi.org/v2/top-headlines}")
    private String baseUrl;

    @Value("${app.news.fallback-url:https://newsapi.org/v2/everything}")
    private String fallbackUrl;

    @Value("${app.news.query:india}")
    private String query;

    @Value("${app.news.country:in}")
    private String country;

    @Value("${app.news.max-age-hours:24}")
    private long maxAgeHours;

    public NewsService(NewsHeadlineRepository repository) {
        this.repository = repository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh();
    }

    /** Refresh twice daily to stay within the provider's free-tier request quota. */
    @Scheduled(fixedRate = 12 * 60 * 60 * 1000, initialDelay = 12 * 60 * 60 * 1000)
    @Transactional
    public void refresh() {
        removeStaleHeadlines();
        if (!hasApiKey()) {
            log.warn("app.news.api-key is not set — refreshing headlines from public RSS feeds.");
            refreshFromRssFeeds();
            return;
        }
        try {
            int saved = 0;
            for (String category : SUPPORTED_CATEGORIES) {
                saved += refreshCategory(category);
            }

            if (saved == 0 && repository.findTop50ByOrderByPublishedAtDesc().isEmpty()) {
                log.info("Top headlines returned no usable articles; trying fallback news search for query='{}'.", query);
                int fallbackSaved = refreshFromUrl(fallbackSearchUrl(null), "fallback search", "general");
                if (fallbackSaved == 0 && repository.findTop50ByOrderByPublishedAtDesc().isEmpty()) {
                    refreshFromRssFeeds();
                }
            }

            log.info("Refreshed news headlines. Cached headline count now {}.", repository.findTop50ByOrderByPublishedAtDesc().size());
        } catch (HttpStatusCodeException e) {
            log.warn("News provider refresh failed with HTTP status {}.", e.getStatusCode());
            refreshFromRssIfCacheEmpty();
        } catch (Exception e) {
            log.error("Failed to refresh news headlines", e);
            refreshFromRssIfCacheEmpty();
        }
    }

    @Transactional
    public List<NewsHeadlineResponse> latest() {
        removeStaleHeadlines();
        List<NewsHeadline> headlines = repository.findTop50ByOrderByPublishedAtDesc();
        if (!headlines.isEmpty()) {
            return headlines.stream().map(this::toResponse).toList();
        }
        return List.of(
                new NewsHeadlineResponse(0L, "No live headlines available yet", "#", "TheTechWire", null, null, "general", Instant.now())
        );
    }

    @Transactional
    public List<NewsHeadlineResponse> byCategory(String category) {
        removeStaleHeadlines();
        List<NewsHeadline> headlines = repository.findTop50ByCategoryOrderByPublishedAtDesc(category);
        if (!headlines.isEmpty()) {
            return headlines.stream().map(this::toResponse).toList();
        }
        return List.of();
    }

    private int refreshCategory(String category) throws Exception {
        String url = topHeadlinesUrl(category);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "WireBlog/1.0");
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode root = mapper.readTree(resp.getBody());
        String status = textOrNull(root, "status");
        if (status != null && !"ok".equalsIgnoreCase(status)) {
            log.error("News provider returned status={} body={}", status, resp.getBody());
            return 0;
        }
        JsonNode articles = root.get("articles");
        if (articles == null || !articles.isArray() || articles.isEmpty()) {
            log.warn("News provider response had no articles for category {}. body={}", category, resp.getBody());
            return 0;
        }

        Set<String> existingUrls = new HashSet<>();
        repository.findTop50ByCategoryOrderByPublishedAtDesc(category).forEach(h -> existingUrls.add(h.getSourceUrl()));

        int saved = 0;
        for (JsonNode a : articles) {
            String title = textOrNull(a, "title");
            String sourceUrl = textOrNull(a, "url");
            Instant publishedAt = parsePublishedAt(textOrNull(a, "publishedAt"));
            if (!isUsableArticle(title, sourceUrl) || !isRecent(publishedAt) || !existingUrls.add(sourceUrl)) continue;

            NewsHeadline headline = NewsHeadline.builder()
                    .title(title)
                    .sourceUrl(sourceUrl)
                    .sourceName(a.has("source") ? textOrNull(a.get("source"), "name") : "Unknown")
                    .imageUrl(textOrNull(a, "urlToImage"))
                    .description(cleanDescription(textOrNull(a, "description")))
                    .category(category)
                    .publishedAt(publishedAt)
                    .build();
            repository.save(headline);
            saved++;
        }

        return saved;
    }

    private int refreshFromUrl(String url, String source, String category) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "WireBlog/1.0");
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode root = mapper.readTree(resp.getBody());
        String status = textOrNull(root, "status");
        if (status != null && !"ok".equalsIgnoreCase(status)) {
            log.error("News provider {} returned status={} body={}", source, status, resp.getBody());
            return 0;
        }
        JsonNode articles = root.get("articles");
        if (articles == null || !articles.isArray()) {
            log.warn("News provider {} response had no articles array. body={}", source, resp.getBody());
            return 0;
        }

        Set<String> existingUrls = new HashSet<>();
        repository.findTop50ByCategoryOrderByPublishedAtDesc(category).forEach(h -> existingUrls.add(h.getSourceUrl()));

        int saved = 0;
        for (JsonNode a : articles) {
            String title = textOrNull(a, "title");
            String sourceUrl = textOrNull(a, "url");
            Instant publishedAt = parsePublishedAt(textOrNull(a, "publishedAt"));
            if (!isUsableArticle(title, sourceUrl) || !isRecent(publishedAt) || !existingUrls.add(sourceUrl)) continue;

            NewsHeadline headline = NewsHeadline.builder()
                    .title(title)
                    .sourceUrl(sourceUrl)
                    .sourceName(a.has("source") ? textOrNull(a.get("source"), "name") : "Unknown")
                    .imageUrl(textOrNull(a, "urlToImage"))
                    .description(cleanDescription(textOrNull(a, "description")))
                    .category(category)
                    .publishedAt(publishedAt)
                    .build();
            repository.save(headline);
            saved++;
        }

        log.info("News provider {} returned {} articles and saved {} new headlines.", source, articles.size(), saved);
        return saved;
    }

    private void refreshFromRssIfCacheEmpty() {
        if (repository.findTop50ByOrderByPublishedAtDesc().isEmpty()) {
            refreshFromRssFeeds();
        }
    }

    private void refreshFromRssFeeds() {
        int saved = 0;
        for (String category : SUPPORTED_CATEGORIES) {
            try {
                saved += refreshRssFeed(category, RSS_FEEDS.get(category));
            } catch (Exception e) {
                log.warn("Could not refresh {} headlines from the public RSS feed.", category, e);
            }
        }
        if (saved == 0) {
            log.warn("Public RSS feeds returned no new headlines.");
        } else {
            log.info("Saved {} headlines from public RSS feeds.", saved);
        }
    }

    private int refreshRssFeed(String category, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "WireBlog/1.0");
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        Document feed = Jsoup.parse(response.getBody(), url, Parser.xmlParser());
        Set<String> existingUrls = new HashSet<>();
        repository.findTop50ByCategoryOrderByPublishedAtDesc(category)
                .forEach(headline -> existingUrls.add(headline.getSourceUrl()));

        int saved = 0;
        for (Element item : feed.select("item")) {
            String title = elementText(item, "title");
            String sourceUrl = elementText(item, "link");
            Instant publishedAt = parseRssPublishedAt(elementText(item, "pubDate"));
            if (!isUsableArticle(title, sourceUrl) || !isRecent(publishedAt) || !existingUrls.add(sourceUrl)) {
                continue;
            }

            String sourceName = elementText(item, "source");
            String description = elementText(item, "description");
            repository.save(NewsHeadline.builder()
                    .title(title)
                    .sourceUrl(sourceUrl)
                    .sourceName(sourceName == null || sourceName.isBlank() ? "BBC News" : sourceName)
                    .description(cleanDescription(description == null ? null : Jsoup.parse(description).text()))
                    .category(category)
                    .publishedAt(publishedAt)
                    .build());
            saved++;
        }
        return saved;
    }

    private String elementText(Element parent, String selector) {
        Element element = parent.selectFirst(selector);
        return element == null ? null : element.text();
    }

    private Instant parseRssPublishedAt(String publishedAt) {
        try {
            return publishedAt == null ? Instant.now()
                    : ZonedDateTime.parse(publishedAt, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String topHeadlinesUrl(String category) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("country", country)
                .queryParam("pageSize", 50)
                .queryParam("apiKey", apiKey);
        if (!"general".equalsIgnoreCase(category)) {
            builder.queryParam("category", category);
        }
        return builder.toUriString();
    }

    private String fallbackSearchUrl(String category) {
        String searchQuery = (category == null || category.isBlank()) ? query : category;
        return UriComponentsBuilder.fromHttpUrl(fallbackUrl)
                .queryParam("q", searchQuery)
                .queryParam("language", "en")
                .queryParam("sortBy", "publishedAt")
                .queryParam("pageSize", 50)
                .queryParam("apiKey", apiKey)
                .toUriString();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private Instant parsePublishedAt(String iso) {
        try {
            return iso == null ? Instant.now() : OffsetDateTime.parse(iso).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private NewsHeadlineResponse toResponse(NewsHeadline h) {
        return new NewsHeadlineResponse(h.getId(), h.getTitle(), h.getSourceUrl(), h.getSourceName(),
                h.getImageUrl(), h.getDescription(), h.getCategory(), h.getPublishedAt());
    }

    private boolean isUsableArticle(String title, String sourceUrl) {
        return title != null && !title.isBlank() && !"[Removed]".equalsIgnoreCase(title.trim())
                && sourceUrl != null && sourceUrl.startsWith("http");
    }

    private boolean isRecent(Instant publishedAt) {
        return !publishedAt.isBefore(Instant.now().minus(maxAgeHours, ChronoUnit.HOURS));
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank() || "[Removed]".equalsIgnoreCase(description.trim())) return null;
        return description.length() <= 300 ? description : description.substring(0, 297).trim() + "...";
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private void removeStaleHeadlines() {
        repository.deleteByPublishedAtBefore(Instant.now().minus(maxAgeHours, ChronoUnit.HOURS));
    }
}
