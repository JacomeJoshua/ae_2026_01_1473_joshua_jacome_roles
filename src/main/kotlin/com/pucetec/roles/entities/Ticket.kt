package com.pucetec.roles.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Tabla 2: ticket de entrada/salida de un vehículo.
 * exitTime es null mientras el auto sigue adentro; se llena al registrar la salida.
 */
@Entity
@Table(name = "tickets")
class Ticket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    val plate: String = "",

    @Column(name = "entry_time")
    val entryTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "exit_time")
    var exitTime: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    val parkingSpace: ParkingSpace,
)
