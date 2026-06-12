package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.Ride;
import com.iitr.ride_management_backend.domain.RideStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RideRepository extends JpaRepository<Ride, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") Long id);

    List<Ride> findByPassengerIdOrderByRequestedAtDesc(Long passengerId);

    List<Ride> findByDriverIdOrderByRequestedAtDesc(Long driverId);

    List<Ride> findByDriverIdAndStatusInOrderByRequestedAtDesc(Long driverId, Collection<RideStatus> statuses);

    @Query("""
            select r from Ride r
            where r.driver.id = :driverId
              and (
                r.status = com.iitr.ride_management_backend.domain.RideStatus.IN_PROGRESS
                or (
                  r.status = com.iitr.ride_management_backend.domain.RideStatus.ACCEPTED
                  and (r.scheduledFor is null or r.scheduledFor <= :now)
                )
              )
            order by r.requestedAt desc
            """)
    List<Ride> findBlockingRidesForDriver(@Param("driverId") Long driverId, @Param("now") Instant now);

    @Query("""
            select count(r) from Ride r
            where r.driver.id = :driverId
              and (
                r.status = com.iitr.ride_management_backend.domain.RideStatus.IN_PROGRESS
                or (
                  r.status = com.iitr.ride_management_backend.domain.RideStatus.ACCEPTED
                  and (r.scheduledFor is null or r.scheduledFor <= :now)
                )
              )
            """)
    long countBlockingRidesForDriver(@Param("driverId") Long driverId, @Param("now") Instant now);

    @Query("""
            select count(r) from Ride r
            where r.driver.id = :driverId
              and r.id <> :rideId
              and (
                r.status = com.iitr.ride_management_backend.domain.RideStatus.IN_PROGRESS
                or (
                  r.status = com.iitr.ride_management_backend.domain.RideStatus.ACCEPTED
                  and (r.scheduledFor is null or r.scheduledFor <= :now)
                )
              )
            """)
    long countOtherBlockingRidesForDriver(
            @Param("driverId") Long driverId,
            @Param("rideId") Long rideId,
            @Param("now") Instant now
    );

    @Query("""
            select count(r) from Ride r
            where r.passenger.id = :passengerId
              and (
                r.status = com.iitr.ride_management_backend.domain.RideStatus.IN_PROGRESS
                or (
                  r.status in (
                    com.iitr.ride_management_backend.domain.RideStatus.REQUESTED,
                    com.iitr.ride_management_backend.domain.RideStatus.ACCEPTED
                  )
                  and (r.scheduledFor is null or r.scheduledFor <= :now)
                )
              )
            """)
    long countBlockingRidesForPassenger(@Param("passengerId") Long passengerId, @Param("now") Instant now);

    @Query("""
            select count(r) from Ride r
            where r.passenger.id = :passengerId
              and r.scheduledFor is not null
              and r.scheduledFor > :windowStart
              and r.scheduledFor < :windowEnd
              and r.status in (
                com.iitr.ride_management_backend.domain.RideStatus.REQUESTED,
                com.iitr.ride_management_backend.domain.RideStatus.ACCEPTED,
                com.iitr.ride_management_backend.domain.RideStatus.IN_PROGRESS
              )
            """)
    long countPassengerScheduledOverlaps(
            @Param("passengerId") Long passengerId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    @Query("""
            select count(r) from Ride r
            where r.driver.id = :driverId
              and r.scheduledFor is not null
              and r.scheduledFor > :windowStart
              and r.scheduledFor < :windowEnd
              and r.status in (
                com.iitr.ride_management_backend.domain.RideStatus.ACCEPTED,
                com.iitr.ride_management_backend.domain.RideStatus.IN_PROGRESS
              )
            """)
    long countDriverScheduledOverlaps(
            @Param("driverId") Long driverId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    long countByDriverIdAndStatus(Long driverId, RideStatus status);

    long countByDriverIdAndStatusIn(Long driverId, Collection<RideStatus> statuses);

    @Query("""
            select r from Ride r
            where r.status = com.iitr.ride_management_backend.domain.RideStatus.REQUESTED
              and not exists (
                select rr.id from RideRejection rr
                where rr.ride = r and rr.driver.id = :driverId
              )
            order by r.requestedAt desc
            """)
    List<Ride> findIncomingRequestsForDriver(@Param("driverId") Long driverId);
}
