package com.pucetec.roles.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Tabla 1: espacio de estacionamiento.
 * Guarda el código del puesto y si está ocupado o no.
 */
@Entity
@Table(name = "parking_spaces")
class ParkingSpace(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(unique = true)
    val code: String = "",

    var occupied: Boolean = false,
)
