package com.pucetec.roles.services

import com.pucetec.roles.dto.EntryRequest
import com.pucetec.roles.dto.ExitRequest
import com.pucetec.roles.entities.ParkingSpace
import com.pucetec.roles.entities.Ticket
import com.pucetec.roles.exceptions.ParkingFullException
import com.pucetec.roles.exceptions.ParkingSpaceNotFoundException
import com.pucetec.roles.exceptions.SpaceAlreadyOccupiedException
import com.pucetec.roles.exceptions.TicketAlreadyClosedException
import com.pucetec.roles.exceptions.TicketNotFoundException
import com.pucetec.roles.repositories.ParkingSpaceRepository
import com.pucetec.roles.repositories.TicketRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

/**
 * Cobertura 100% (líneas y ramas) del service que orquesta entrada/salida con DOS repositorios.
 * Hay un test por cada camino: feliz + cada rama de error.
 */
@ExtendWith(MockitoExtension::class)
class TicketServiceTest {

    @Mock
    private lateinit var parkingSpaceRepository: ParkingSpaceRepository

    @Mock
    private lateinit var ticketRepository: TicketRepository

    @InjectMocks
    private lateinit var ticketService: TicketService

    // ───────────────────────────── ENTRADA ─────────────────────────────

    @Test
    fun `registerEntry ocupa el espacio y crea el ticket en el camino feliz`() {
        val space = ParkingSpace(id = 1L, code = "A1", occupied = false)
        val request = EntryRequest(plate = "PBA-1234", parkingSpaceId = 1L)
        val savedTicket = Ticket(id = 100L, plate = "PBA-1234", parkingSpace = space)

        `when`(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space))
        `when`(parkingSpaceRepository.countByOccupiedTrue()).thenReturn(5L)
        `when`(parkingSpaceRepository.save(any(ParkingSpace::class.java))).thenReturn(space)
        `when`(ticketRepository.save(any(Ticket::class.java))).thenReturn(savedTicket)

        val response = ticketService.registerEntry(request)

        assertEquals(100L, response.id)
        assertEquals("PBA-1234", response.plate)
        assertTrue(space.occupied)
    }

    @Test
    fun `registerEntry lanza ParkingSpaceNotFoundException si el espacio no existe`() {
        val request = EntryRequest(plate = "PBA-1234", parkingSpaceId = 99L)
        `when`(parkingSpaceRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ParkingSpaceNotFoundException> {
            ticketService.registerEntry(request)
        }
    }

    @Test
    fun `registerEntry lanza SpaceAlreadyOccupiedException si el espacio ya esta ocupado`() {
        val space = ParkingSpace(id = 1L, code = "A1", occupied = true)
        val request = EntryRequest(plate = "PBA-1234", parkingSpaceId = 1L)
        `when`(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space))

        assertThrows<SpaceAlreadyOccupiedException> {
            ticketService.registerEntry(request)
        }
    }

    @Test
    fun `registerEntry lanza ParkingFullException si se alcanzo la capacidad maxima`() {
        val space = ParkingSpace(id = 1L, code = "A1", occupied = false)
        val request = EntryRequest(plate = "PBA-1234", parkingSpaceId = 1L)
        `when`(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space))
        `when`(parkingSpaceRepository.countByOccupiedTrue()).thenReturn(20L)

        assertThrows<ParkingFullException> {
            ticketService.registerEntry(request)
        }
    }

    // ───────────────────────────── SALIDA ─────────────────────────────

    @Test
    fun `registerExit cierra el ticket y libera el espacio en el camino feliz`() {
        val space = ParkingSpace(id = 1L, code = "A1", occupied = true)
        val ticket = Ticket(id = 100L, plate = "PBA-1234", parkingSpace = space, exitTime = null)
        val request = ExitRequest(ticketId = 100L)

        `when`(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket))
        `when`(parkingSpaceRepository.save(any(ParkingSpace::class.java))).thenReturn(space)
        `when`(ticketRepository.save(any(Ticket::class.java))).thenReturn(ticket)

        val response = ticketService.registerExit(request)

        assertEquals(100L, response.id)
        assertNotNull(response.exitTime)
        assertFalse(space.occupied)
    }

    @Test
    fun `registerExit lanza TicketNotFoundException si el ticket no existe`() {
        val request = ExitRequest(ticketId = 404L)
        `when`(ticketRepository.findById(404L)).thenReturn(Optional.empty())

        assertThrows<TicketNotFoundException> {
            ticketService.registerExit(request)
        }
    }

    @Test
    fun `registerExit lanza TicketAlreadyClosedException si el ticket ya fue cerrado`() {
        val space = ParkingSpace(id = 1L, code = "A1", occupied = false)
        val ticket = Ticket(
            id = 100L,
            plate = "PBA-1234",
            parkingSpace = space,
            exitTime = LocalDateTime.now()
        )
        val request = ExitRequest(ticketId = 100L)
        `when`(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket))

        assertThrows<TicketAlreadyClosedException> {
            ticketService.registerExit(request)
        }
    }
}
