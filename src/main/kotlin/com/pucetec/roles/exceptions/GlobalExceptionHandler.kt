package com.pucetec.roles.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Traduce las excepciones de negocio a códigos HTTP.
 *
 * OJO: los códigos 401 (sin token) y 403 (token sin el rol correcto) NO se manejan aquí:
 * los genera Spring Security en la cadena de filtros, ANTES de llegar a los controllers.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ParkingSpaceNotFoundException::class)
    fun handleParkingSpaceNotFound(e: ParkingSpaceNotFoundException): ResponseEntity<ExceptionResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ExceptionResponse(e.message ?: "Espacio no encontrado", "ParkingSpaceService"))

    @ExceptionHandler(TicketNotFoundException::class)
    fun handleTicketNotFound(e: TicketNotFoundException): ResponseEntity<ExceptionResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ExceptionResponse(e.message ?: "Ticket no encontrado", "TicketService"))

    @ExceptionHandler(ParkingFullException::class)
    fun handleParkingFull(e: ParkingFullException): ResponseEntity<ExceptionResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ExceptionResponse(e.message ?: "Estacionamiento lleno", "TicketService"))

    @ExceptionHandler(SpaceAlreadyOccupiedException::class)
    fun handleSpaceAlreadyOccupied(e: SpaceAlreadyOccupiedException): ResponseEntity<ExceptionResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ExceptionResponse(e.message ?: "El espacio ya está ocupado", "TicketService"))

    @ExceptionHandler(TicketAlreadyClosedException::class)
    fun handleTicketAlreadyClosed(e: TicketAlreadyClosedException): ResponseEntity<ExceptionResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ExceptionResponse(e.message ?: "El ticket ya fue cerrado", "TicketService"))

    @ExceptionHandler(DuplicateParkingSpaceException::class)
    fun handleDuplicateParkingSpace(e: DuplicateParkingSpaceException): ResponseEntity<ExceptionResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ExceptionResponse(e.message ?: "El código de espacio ya existe", "ParkingSpaceService"))
}

data class ExceptionResponse(
    val message: String,
    val source: String,
)
