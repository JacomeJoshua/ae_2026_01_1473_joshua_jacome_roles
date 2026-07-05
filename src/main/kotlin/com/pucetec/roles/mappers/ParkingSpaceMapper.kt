package com.pucetec.roles.mappers

import com.pucetec.roles.dto.CreateParkingSpaceRequest
import com.pucetec.roles.dto.ParkingSpaceResponse
import com.pucetec.roles.entities.ParkingSpace

fun CreateParkingSpaceRequest.toEntity() = ParkingSpace(
    code = this.code,
    occupied = false,
)

fun ParkingSpace.toResponse() = ParkingSpaceResponse(
    id = this.id,
    code = this.code,
    occupied = this.occupied,
)
