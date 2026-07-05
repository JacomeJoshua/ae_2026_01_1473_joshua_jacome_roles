package com.pucetec.roles.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Seguridad de la resolución del examen: AUTENTICACIÓN BÁSICA (con token vs sin token).
 *
 * - El endpoint de espacios disponibles es PÚBLICO.
 * - Todo lo demás exige un JWT válido emitido por Cognito (OAuth2 Resource Server).
 *
 * ⚠️ Aquí NO hay autorización por roles: cualquier usuario autenticado puede crear espacios y
 * registrar entrada/salida. Convertir esto en autorización POR ROLES (solo ADMIN crea espacios,
 * solo USER registra entrada/salida) es EL DEBER del estudiante; la guía está en `homework_roles.md`.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // PÚBLICO: consultar espacios disponibles.
                auth.requestMatchers(HttpMethod.GET, "/parking-spaces/available").permitAll()
                // Cualquier otra ruta exige token válido (aún SIN distinguir rol).
                auth.anyRequest().authenticated()
            }
            // Resource Server: valida firma, emisor (iss) y expiración (exp) del JWT contra el
            // JWKS de Cognito (tomado del issuer-uri de application.yaml). Sin token válido -> 401.
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { } }
        return http.build()
    }
}
