package com.pucetec.roles.repositories

import com.pucetec.roles.entities.ParkingSpace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ParkingSpaceRepository : JpaRepository<ParkingSpace, Long> {
    fun findByOccupiedFalse(): List<ParkingSpace>
    fun countByOccupiedTrue(): Long
    fun existsByCode(code: String): Boolean
}
