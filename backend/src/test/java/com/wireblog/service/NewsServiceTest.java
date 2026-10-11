package com.wireblog.service;

import com.wireblog.model.NewsHeadline;
import com.wireblog.repository.NewsHeadlineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NewsServiceTest {

    @Test
    void latestServesFallbackWithoutRefreshingProviderWhenCacheIsEmpty() {
        NewsHeadlineRepository repository = mock(NewsHeadlineRepository.class);
        when(repository.findTop50ByOrderByPublishedAtDesc()).thenReturn(List.of());
        NewsService service = serviceWithApiKey(repository);

        assertThat(service.latest()).singleElement()
                .extracting(headline -> headline.title())
                .isEqualTo("No live headlines available yet");
        verify(repository, times(1)).deleteByPublishedAtBefore(any());
    }

    @Test
    void categoryReadsDoNotRefreshProviderWhenCacheIsEmpty() {
        NewsHeadlineRepository repository = mock(NewsHeadlineRepository.class);
        when(repository.findTop50ByCategoryOrderByPublishedAtDesc("technology")).thenReturn(List.of());
        NewsService service = serviceWithApiKey(repository);

        assertThat(service.byCategory("technology")).isEmpty();
        verify(repository, times(1)).deleteByPublishedAtBefore(any());
    }

    @Test
    void refreshUsesPublicRssFeedsWhenApiKeyIsMissing() {
        NewsHeadlineRepository repository = mock(NewsHeadlineRepository.class);
        when(repository.findTop50ByCategoryOrderByPublishedAtDesc(anyString())).thenReturn(List.of());
        when(repository.findTop50ByOrderByPublishedAtDesc()).thenReturn(List.of());

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.times(6), requestTo(org.hamcrest.Matchers.containsString("https://")))
                .andRespond(withSuccess(rssFeed(), MediaType.APPLICATION_XML));
        NewsService service = new NewsService(repository);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "maxAgeHours", 24L);

        service.refresh();

        server.verify();
        var headlines = forClass(NewsHeadline.class);
        verify(repository, times(6)).save(headlines.capture());
        assertThat(headlines.getAllValues())
                .extracting(NewsHeadline::getCategory)
                .containsExactlyInAnyOrder("general", "business", "technology", "sports", "science", "health");
        assertThat(headlines.getAllValues()).allSatisfy(headline -> {
            assertThat(headline.getTitle()).isEqualTo("Test headline");
            assertThat(headline.getSourceName())
                    .isEqualTo("technology".equals(headline.getCategory()) ? "TechCrunch" : "BBC News");
            assertThat(headline.getDescription()).isEqualTo("A useful summary.");
            assertThat(headline.getSourceUrl()).isEqualTo("https://www.bbc.com/news/test-headline");
        });
    }

    @Test
    void refreshUsesRssWhenNewsApiFallbackOnlyReturnsStaleArticles() {
        NewsHeadlineRepository repository = mock(NewsHeadlineRepository.class);
        when(repository.findTop50ByCategoryOrderByPublishedAtDesc(anyString())).thenReturn(List.of());
        when(repository.findTop50ByOrderByPublishedAtDesc()).thenReturn(List.of());

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.times(6), requestTo(org.hamcrest.Matchers.containsString("/top-headlines")))
                .andRespond(withSuccess("{\"status\":\"ok\",\"articles\":[]}", MediaType.APPLICATION_JSON));
        String staleArticle = """
                {"status":"ok","articles":[{"title":"Old article","url":"https://www.bbc.com/news/old-article","publishedAt":"%s"}]}
                """.formatted(Instant.now().minus(48, ChronoUnit.HOURS));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/everything")))
                .andRespond(withSuccess(staleArticle, MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.times(6), requestTo(org.hamcrest.Matchers.containsString("https://")))
                .andRespond(withSuccess(rssFeed(), MediaType.APPLICATION_XML));
        NewsService service = serviceWithApiKey(repository);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        service.refresh();

        server.verify();
        verify(repository, times(6)).save(any());
    }

    private String rssFeed() {
        String publishedAt = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><item>
                  <title>Test headline</title>
                  <link>https://www.bbc.com/news/test-headline</link>
                  <description>A useful summary.</description>
                  <pubDate>%s</pubDate>
                </item></channel></rss>
                """.formatted(publishedAt);
    }

    private NewsService serviceWithApiKey(NewsHeadlineRepository repository) {
        NewsService service = new NewsService(repository);
        ReflectionTestUtils.setField(service, "apiKey", "configured-test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://newsapi.org/v2/top-headlines");
        ReflectionTestUtils.setField(service, "fallbackUrl", "https://newsapi.org/v2/everything");
        ReflectionTestUtils.setField(service, "maxAgeHours", 24L);
        return service;
    }
}
