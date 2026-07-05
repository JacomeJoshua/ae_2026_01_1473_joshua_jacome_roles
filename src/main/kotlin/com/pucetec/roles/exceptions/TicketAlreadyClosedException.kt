package com.pucetec.roles.exceptions

// Validación de negocio adicional: no se puede cerrar un ticket que ya fue cerrado.
class TicketAlreadyClosedException(
    message: String? = null
) : Exception(message)
