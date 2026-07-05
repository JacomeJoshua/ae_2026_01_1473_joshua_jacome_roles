package com.pucetec.roles.mappers

import com.pucetec.roles.dto.TicketResponse
import com.pucetec.roles.entities.Ticket

fun Ticket.toResponse() = TicketResponse(
    id = this.id,
    plate = this.plate,
    entryTime = this.entryTime,
    exitTime = this.exitTime,
    parkingSpace = this.parkingSpace.toResponse(),
)
