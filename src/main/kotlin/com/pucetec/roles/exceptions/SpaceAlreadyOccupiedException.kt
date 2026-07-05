package com.pucetec.roles.exceptions

// Validación de negocio adicional: no se puede entrar a un espacio que ya está ocupado.
class SpaceAlreadyOccupiedException(
    message: String? = null
) : Exception(message)
