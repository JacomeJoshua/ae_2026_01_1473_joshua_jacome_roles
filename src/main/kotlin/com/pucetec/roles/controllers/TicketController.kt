package com.pucetec.roles.controllers

import com.pucetec.roles.dto.EntryRequest
import com.pucetec.roles.dto.ExitRequest
import com.pucetec.roles.dto.TicketResponse
import com.pucetec.roles.services.TicketService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tickets")
class TicketController(
    private val ticketService: TicketService
) {

    private val logger = LoggerFactory.getLogger(TicketController::class.java)

    // PRIVADO: requiere token válido. (En el DEBER se restringe SOLO a USER.)
    @PostMapping("/entry")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerEntry(@RequestBody request: EntryRequest): TicketResponse {
        logger.info("Registering entry for plate ${request.plate}")
        return ticketService.registerEntry(request)
    }

    // PRIVADO: requiere token válido. (En el DEBER se restringe SOLO a USER.)
    @PostMapping("/exit")
    fun registerExit(@RequestBody request: ExitRequest): TicketResponse {
        logger.info("Registering exit for ticket ${request.ticketId}")
        return ticketService.registerExit(request)
    }
}
