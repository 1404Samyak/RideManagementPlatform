package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.Ride;
import com.iitr.ride_management_backend.domain.RideStatus;
import jakarta.persistence.LockModeType;
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
