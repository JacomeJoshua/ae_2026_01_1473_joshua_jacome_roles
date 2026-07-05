package com.pucetec.roles.services

import com.pucetec.roles.dto.EntryRequest
import com.pucetec.roles.dto.ExitRequest
import com.pucetec.roles.dto.TicketResponse
import com.pucetec.roles.entities.Ticket
import com.pucetec.roles.exceptions.ParkingFullException
import com.pucetec.roles.exceptions.ParkingSpaceNotFoundException
import com.pucetec.roles.exceptions.SpaceAlreadyOccupiedException
import com.pucetec.roles.exceptions.TicketAlreadyClosedException
import com.pucetec.roles.exceptions.TicketNotFoundException
import com.pucetec.roles.mappers.toResponse
import com.pucetec.roles.repositories.ParkingSpaceRepository
import com.pucetec.roles.repositories.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service que orquesta entrada/salida usando DOS repositorios (espacios y tickets).
 * Este es el service que debe alcanzar 100% de cobertura de líneas y ramas.
 */
@Service
class TicketService(
    private val parkingSpaceRepository: ParkingSpaceRepository,
    private val ticketRepository: TicketRepository
) {

    private val logger = LoggerFactory.getLogger(TicketService::class.java)

    // Capacidad máxima del estacionamiento: única fuente de la verdad, fácil de cambiar aquí.
    private val capacity = 20

    fun registerEntry(request: EntryRequest): TicketResponse {
        logger.info("Registering entry for plate ${request.plate} on space ${request.parkingSpaceId}")

        val space = parkingSpaceRepository.findById(request.parkingSpaceId).orElseThrow {
            ParkingSpaceNotFoundException("Espacio ${request.parkingSpaceId} no encontrado")
        }

        if (space.occupied) {
            throw SpaceAlreadyOccupiedException("El espacio ${space.code} ya está ocupado")
        }

        if (parkingSpaceRepository.countByOccupiedTrue() >= capacity) {
            throw ParkingFullException("Estacionamiento lleno: capacidad máxima $capacity")
        }

        space.occupied = true
        parkingSpaceRepository.save(space)

        val ticket = Ticket(plate = request.plate, parkingSpace = space)
        return ticketRepository.save(ticket).toResponse()
    }

    fun registerExit(request: ExitRequest): TicketResponse {
        logger.info("Registering exit for ticket ${request.ticketId}")

        val ticket = ticketRepository.findById(request.ticketId).orElseThrow {
            TicketNotFoundException("Ticket ${request.ticketId} no encontrado")
        }

        if (ticket.exitTime != null) {
            throw TicketAlreadyClosedException("El ticket ${ticket.id} ya fue cerrado")
        }

        ticket.exitTime = LocalDateTime.now()
        val space = ticket.parkingSpace
        space.occupied = false
        parkingSpaceRepository.save(space)

        return ticketRepository.save(ticket).toResponse()
    }
}
