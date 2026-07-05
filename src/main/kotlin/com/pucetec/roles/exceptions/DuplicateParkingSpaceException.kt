package com.pucetec.roles.exceptions

// No se puede crear un espacio con un código que ya existe.
class DuplicateParkingSpaceException(
    message: String? = null
) : Exception(message)
