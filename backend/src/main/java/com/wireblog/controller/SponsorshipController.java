package com.wireblog.controller;

import com.wireblog.dto.SponsorshipResponse;
import com.wireblog.service.SponsorshipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsorships")
public class SponsorshipController {

    private final SponsorshipService sponsorshipService;

    public SponsorshipController(SponsorshipService sponsorshipService) {
        this.sponsorshipService = sponsorshipService;
    }

    /** Public: what ad(s) should render in a given slot right now. */
    @GetMapping("/active")
    public List<SponsorshipResponse> active(@RequestParam String placement) {
        return sponsorshipService.activeFor(placement);
    }

    @PostMapping("/{id}/click")
    public void recordClick(@PathVariable Long id) {
        sponsorshipService.recordClick(id);
    }
}
