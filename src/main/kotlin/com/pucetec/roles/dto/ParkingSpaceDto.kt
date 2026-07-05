package com.pucetec.roles.dto

data class CreateParkingSpaceRequest(
    val code: String,
)

data class ParkingSpaceResponse(
    val id: Long,
    val code: String,
    val occupied: Boolean,
)
