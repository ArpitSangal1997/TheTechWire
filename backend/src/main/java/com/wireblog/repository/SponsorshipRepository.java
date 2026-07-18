package com.wireblog.repository;

import com.wireblog.model.Sponsorship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, Long> {
    List<Sponsorship> findByPlacementAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Sponsorship.Placement placement, Sponsorship.Status status, LocalDate today1, LocalDate today2);
}
