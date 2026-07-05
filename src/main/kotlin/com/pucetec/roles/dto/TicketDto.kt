package com.pucetec.roles.dto

import java.time.LocalDateTime

data class EntryRequest(
    val plate: String,
    val parkingSpaceId: Long,
)

data class ExitRequest(
    val ticketId: Long,
)

data class TicketResponse(
    val id: Long,
    val plate: String,
    val entryTime: LocalDateTime,
    val exitTime: LocalDateTime?,
    val parkingSpace: ParkingSpaceResponse,
)
