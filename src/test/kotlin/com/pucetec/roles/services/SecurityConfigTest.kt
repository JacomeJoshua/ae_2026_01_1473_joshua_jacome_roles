package com.pucetec.roles.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `test seguridad endpoints cobertura completa`() {
        // 1. Público: Acceso permitido (200 OK)
        mockMvc.perform(get("/parking-spaces/available"))
            .andExpect(status().isOk)

        // 2. POST parking-spaces sin token: Acceso denegado (401 Unauthorized)
        mockMvc.perform(post("/parking-spaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"A1\"}"))
            .andExpect(status().isUnauthorized)

        // 3. POST tickets sin token: Acceso denegado (401 Unauthorized)
        mockMvc.perform(post("/tickets/entry")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"plate\":\"ABC\", \"parkingSpaceId\": 1}"))
            .andExpect(status().isUnauthorized)
    }
}