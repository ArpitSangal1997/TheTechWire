package com.wireblog.controller;

import com.wireblog.dto.SponsorshipRequest;
import com.wireblog.dto.SponsorshipResponse;
import com.wireblog.service.SponsorshipService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything here is gated to ROLE_ADMIN in SecurityConfig — this is the
 * "you and me" back office: manage brand deals, moderate content, see
 * platform-wide numbers.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SponsorshipService sponsorshipService;

    public AdminController(SponsorshipService sponsorshipService) {
        this.sponsorshipService = sponsorshipService;
    }

    @GetMapping("/sponsorships")
    public List<SponsorshipResponse> listSponsorships() {
        return sponsorshipService.listAll();
    }

    @PostMapping("/sponsorships")
    public SponsorshipResponse createSponsorship(@Valid @RequestBody SponsorshipRequest req) {
        return sponsorshipService.create(req);
    }

    @PutMapping("/sponsorships/{id}")
    public SponsorshipResponse updateSponsorship(@PathVariable Long id, @Valid @RequestBody SponsorshipRequest req) {
        return sponsorshipService.update(id, req);
    }

    @DeleteMapping("/sponsorships/{id}")
    public void deleteSponsorship(@PathVariable Long id) {
        sponsorshipService.delete(id);
    }
}
