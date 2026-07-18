package com.wireblog.service;

import com.wireblog.dto.SponsorshipRequest;
import com.wireblog.dto.SponsorshipResponse;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Sponsorship;
import com.wireblog.repository.SponsorshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SponsorshipService {

    private final SponsorshipRepository repository;

    public SponsorshipService(SponsorshipRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SponsorshipResponse create(SponsorshipRequest req) {
        Sponsorship s = Sponsorship.builder()
                .brandName(req.brandName())
                .headline(req.headline())
                .targetUrl(req.targetUrl())
                .creativeImageUrl(req.creativeImageUrl())
                .placement(Sponsorship.Placement.valueOf(req.placement().toUpperCase()))
                .status(Sponsorship.Status.valueOf(req.status().toUpperCase()))
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();
        return toResponse(repository.save(s));
    }

    @Transactional
    public SponsorshipResponse update(Long id, SponsorshipRequest req) {
        Sponsorship s = repository.findById(id).orElseThrow(() -> ApiException.notFound("Sponsorship not found."));
        s.setBrandName(req.brandName());
        s.setHeadline(req.headline());
        s.setTargetUrl(req.targetUrl());
        s.setCreativeImageUrl(req.creativeImageUrl());
        s.setPlacement(Sponsorship.Placement.valueOf(req.placement().toUpperCase()));
        s.setStatus(Sponsorship.Status.valueOf(req.status().toUpperCase()));
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        return toResponse(repository.save(s));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<SponsorshipResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    /** What the public site actually renders for a given ad slot. */
    @Transactional
    public List<SponsorshipResponse> activeFor(String placement) {
        LocalDate today = LocalDate.now();
        List<Sponsorship> active = repository.findByPlacementAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                Sponsorship.Placement.valueOf(placement.toUpperCase()), Sponsorship.Status.ACTIVE, today, today);
        active.forEach(s -> s.setImpressions(s.getImpressions() + 1));
        repository.saveAll(active);
        return active.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void recordClick(Long id) {
        Sponsorship s = repository.findById(id).orElseThrow(() -> ApiException.notFound("Sponsorship not found."));
        s.setClicks(s.getClicks() + 1);
        repository.save(s);
    }

    private SponsorshipResponse toResponse(Sponsorship s) {
        return new SponsorshipResponse(s.getId(), s.getBrandName(), s.getHeadline(), s.getTargetUrl(),
                s.getCreativeImageUrl(), s.getPlacement().name(), s.getStatus().name(),
                s.getStartDate(), s.getEndDate(), s.getImpressions(), s.getClicks());
    }
}
