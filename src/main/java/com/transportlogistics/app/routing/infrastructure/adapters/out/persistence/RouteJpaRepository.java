package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface RouteJpaRepository extends JpaRepository<RouteEntity, UUID> {
    @Query("""
            select distinct route from RouteEntity route
            where (:query is null
                    or lower(route.code) like lower(concat('%', :query, '%'))
                    or lower(route.name) like lower(concat('%', :query, '%')))
              and (:originLocationId is null or route.originLocationId = :originLocationId)
              and (:destinationLocationId is null or route.destinationLocationId = :destinationLocationId)
              and (:active is null or route.active = :active)
            order by route.code
            """)
    List<RouteEntity> search(@Param("query") String query,
                             @Param("originLocationId") UUID originLocationId,
                             @Param("destinationLocationId") UUID destinationLocationId,
                             @Param("active") Boolean active);
}
