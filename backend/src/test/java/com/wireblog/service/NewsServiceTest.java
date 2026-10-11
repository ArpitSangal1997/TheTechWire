package com.wireblog.service;

import com.wireblog.model.NewsHeadline;
import com.wireblog.repository.NewsHeadlineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

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
        server.expect(ExpectedCount.times(6), requestTo(org.hamcrest.Matchers.containsString("feeds.bbci.co.uk")))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <rss version="2.0"><channel><item>
                          <title>Test headline</title>
                          <link>https://www.bbc.com/news/test-headline</link>
                          <description>A useful summary.</description>
                          <pubDate>Sun, 11 Oct 2026 12:00:00 GMT</pubDate>
                        </item></channel></rss>
                        """, MediaType.APPLICATION_XML));
        NewsService service = new NewsService(repository);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        service.refresh();

        server.verify();
        var headlines = forClass(NewsHeadline.class);
        verify(repository, times(6)).save(headlines.capture());
        assertThat(headlines.getAllValues())
                .extracting(NewsHeadline::getCategory)
                .containsExactlyInAnyOrder("general", "business", "technology", "sports", "science", "health");
        assertThat(headlines.getAllValues()).allSatisfy(headline -> {
            assertThat(headline.getTitle()).isEqualTo("Test headline");
            assertThat(headline.getSourceName()).isEqualTo("BBC News");
            assertThat(headline.getDescription()).isEqualTo("A useful summary.");
            assertThat(headline.getSourceUrl()).isEqualTo("https://www.bbc.com/news/test-headline");
        });
    }

    private NewsService serviceWithApiKey(NewsHeadlineRepository repository) {
        NewsService service = new NewsService(repository);
        ReflectionTestUtils.setField(service, "apiKey", "configured-test-key");
        return service;
    }
}
