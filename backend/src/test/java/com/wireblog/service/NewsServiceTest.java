package com.wireblog.service;

import com.wireblog.repository.NewsHeadlineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private NewsService serviceWithApiKey(NewsHeadlineRepository repository) {
        NewsService service = new NewsService(repository);
        ReflectionTestUtils.setField(service, "apiKey", "configured-test-key");
        return service;
    }
}
