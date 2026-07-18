package com.wireblog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "news_headlines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsHeadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceUrl; // where the user gets redirected on click

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceName; // e.g. "Reuters", "BBC News"

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(length = 80)
    private String category; // general, business, tech, sports, science...

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private Instant fetchedAt;

    @PrePersist
    public void prePersist() {
        fetchedAt = Instant.now();
    }
}
