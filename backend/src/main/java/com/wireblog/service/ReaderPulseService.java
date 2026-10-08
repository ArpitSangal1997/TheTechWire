package com.wireblog.service;

import com.wireblog.dto.ReaderPulseOptionResponse;
import com.wireblog.dto.ReaderPulseResponse;
import com.wireblog.dto.ReaderPulseVoteRequest;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Post;
import com.wireblog.model.PulseChoice;
import com.wireblog.model.ReaderPulse;
import com.wireblog.repository.ReaderPulseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ReaderPulseService {

    private static final String QUESTION = "What did this piece give you?";
    private static final List<PulseChoice> CHOICES = List.of(
            PulseChoice.NEW_ANGLE,
            PulseChoice.NEEDS_RECEIPTS,
            PulseChoice.CHANGED_MY_MIND,
            PulseChoice.WORTH_SHARING
    );
    private static final Map<PulseChoice, String> LABELS = Map.of(
            PulseChoice.NEW_ANGLE, "A new angle",
            PulseChoice.NEEDS_RECEIPTS, "Needs receipts",
            PulseChoice.CHANGED_MY_MIND, "Changed my mind",
            PulseChoice.WORTH_SHARING, "Worth sharing");

    private final PostService postService;
    private final ReaderPulseRepository readerPulseRepository;

    public ReaderPulseService(PostService postService, ReaderPulseRepository readerPulseRepository) {
        this.postService = postService;
        this.readerPulseRepository = readerPulseRepository;
    }

    @Transactional(readOnly = true)
    public ReaderPulseResponse getForPost(Long postId) {
        requirePublishedPost(postId);
        return response(postId);
    }

    @Transactional
    public ReaderPulseResponse vote(Long postId, ReaderPulseVoteRequest request) {
        Post post = requirePublishedPost(postId);
        PulseChoice choice = parseChoice(request.choice());
        readerPulseRepository.save(ReaderPulse.builder().post(post).choice(choice).build());
        return response(postId);
    }

    private ReaderPulseResponse response(Long postId) {
        Map<PulseChoice, Long> counts = new EnumMap<>(PulseChoice.class);
        readerPulseRepository.countByPostGroupedByChoice(postId)
                .forEach(row -> counts.put((PulseChoice) row[0], (Long) row[1]));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<ReaderPulseOptionResponse> options = CHOICES.stream()
                .map(choice -> option(choice, LABELS.get(choice), counts.getOrDefault(choice, 0L), total))
                .toList();
        return new ReaderPulseResponse(QUESTION, total, options);
    }

    private ReaderPulseOptionResponse option(PulseChoice choice, String label, long count, long total) {
        int percentage = total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
        return new ReaderPulseOptionResponse(choice.name(), label, count, percentage);
    }

    private PulseChoice parseChoice(String value) {
        try {
            return PulseChoice.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown pulse choice.");
        }
    }

    private Post requirePublishedPost(Long postId) {
        Post post = postService.requirePost(postId);
        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            throw ApiException.notFound("Post not found.");
        }
        return post;
    }
}
