package com.shakib.smartorder.product.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shakib.smartorder.product.entity.Offer;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    // Offers that are switched on and inside their date window right now, for a set of brands.
    @Query("select o from Offer o where o.brandId in :brandIds and o.active = true "
            + "and o.startsAt <= :now and o.endsAt > :now")
    List<Offer> findActiveForBrands(@Param("brandIds") Collection<Long> brandIds, @Param("now") Instant now);

    List<Offer> findByBrandIdOrderByIdDesc(Long brandId);
}
