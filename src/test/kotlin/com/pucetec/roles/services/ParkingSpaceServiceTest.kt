package com.pucetec.roles.services

import com.pucetec.roles.dto.CreateParkingSpaceRequest
import com.pucetec.roles.entities.ParkingSpace
import com.pucetec.roles.exceptions.DuplicateParkingSpaceException
import com.pucetec.roles.repositories.ParkingSpaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ParkingSpaceServiceTest {

    @Mock
    private lateinit var parkingSpaceRepository: ParkingSpaceRepository

    @InjectMocks
    private lateinit var parkingSpaceService: ParkingSpaceService

    @Test
    fun `createSpace guarda y devuelve el espacio cuando el codigo no existe`() {
        val request = CreateParkingSpaceRequest(code = "A1")
        val saved = ParkingSpace(id = 1L, code = "A1", occupied = false)
        `when`(parkingSpaceRepository.existsByCode("A1")).thenReturn(false)
        `when`(parkingSpaceRepository.save(any(ParkingSpace::class.java))).thenReturn(saved)

        val response = parkingSpaceService.createSpace(request)

        assertEquals(1L, response.id)
        assertEquals("A1", response.code)
        assertFalse(response.occupied)
    }

    @Test
    fun `createSpace lanza DuplicateParkingSpaceException cuando el codigo ya existe`() {
        val request = CreateParkingSpaceRequest(code = "A1")
        `when`(parkingSpaceRepository.existsByCode("A1")).thenReturn(true)

        assertThrows<DuplicateParkingSpaceException> {
            parkingSpaceService.createSpace(request)
        }
    }

    @Test
    fun `getAvailableSpaces devuelve solo los espacios libres mapeados a response`() {
        val libre = ParkingSpace(id = 1L, code = "A1", occupied = false)
        `when`(parkingSpaceRepository.findByOccupiedFalse()).thenReturn(listOf(libre))

        val response = parkingSpaceService.getAvailableSpaces()

        assertEquals(1, response.size)
        assertEquals("A1", response[0].code)
    }
}
