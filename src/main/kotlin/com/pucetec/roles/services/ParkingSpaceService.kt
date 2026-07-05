package com.pucetec.roles.services

import com.pucetec.roles.dto.CreateParkingSpaceRequest
import com.pucetec.roles.dto.ParkingSpaceResponse
import com.pucetec.roles.exceptions.DuplicateParkingSpaceException
import com.pucetec.roles.mappers.toEntity
import com.pucetec.roles.mappers.toResponse
import com.pucetec.roles.repositories.ParkingSpaceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ParkingSpaceService(
    private val parkingSpaceRepository: ParkingSpaceRepository
) {

    private val logger = LoggerFactory.getLogger(ParkingSpaceService::class.java)

    fun createSpace(request: CreateParkingSpaceRequest): ParkingSpaceResponse {
        logger.info("Creating parking space ${request.code}")
        if (parkingSpaceRepository.existsByCode(request.code)) {
            throw DuplicateParkingSpaceException("Ya existe un espacio con el código ${request.code}")
        }
        return parkingSpaceRepository.save(request.toEntity()).toResponse()
    }

    fun getAvailableSpaces(): List<ParkingSpaceResponse> {
        logger.info("Getting available parking spaces")
        return parkingSpaceRepository.findByOccupiedFalse().map { it.toResponse() }
    }
}
