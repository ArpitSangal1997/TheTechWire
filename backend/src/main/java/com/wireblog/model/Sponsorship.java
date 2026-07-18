package com.wireblog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A paid placement booked by a brand: a banner in the feed, a "sponsored post"
 * label, or a slot in the news ticker. Admin creates/edits these; the public
 * site just renders whatever is ACTIVE and within its date range.
 */
@Entity
@Table(name = "sponsorships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sponsorship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brandName;

    @Column(nullable = false, length = 300)
    private String headline; // ad copy shown to readers

    @Column(nullable = false, length = 800)
    private String targetUrl; // where clicking the ad sends the reader

    private String creativeImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Placement placement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private long impressions = 0L;

    @Column(nullable = false)
    @Builder.Default
    private long clicks = 0L;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public enum Placement {
        FEED_BANNER, SIDEBAR, NEWS_TICKER, POST_INLINE
    }

    public enum Status {
        DRAFT, ACTIVE, PAUSED, ENDED
    }
}
