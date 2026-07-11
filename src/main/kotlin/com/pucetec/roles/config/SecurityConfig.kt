package com.pucetec.roles.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // 1. Endpoint PÚBLICO: Consultar espacios disponibles sin token
                auth.requestMatchers(HttpMethod.GET, "/parking-spaces/available").permitAll()

                // 2. Solo los usuarios con rol ADMIN pueden crear nuevos espacios de estacionamiento
                auth.requestMatchers(HttpMethod.POST, "/parking-spaces").hasRole("ADMIN")

                // 3. Solo los usuarios con rol USER pueden registrar la entrada y salida de vehículos
                auth.requestMatchers(HttpMethod.POST, "/tickets/**").hasRole("USER")

                // 4. Cualquier otra petición exige obligatoriamente un token válido
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                // Mapeamos los grupos de AWS Cognito ("ADMIN", "USER") a roles de Spring ("ROLE_ADMIN", "ROLE_USER")
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(cognitoGroupsConverter())
                }
            }
        return http.build()
    }

    /**
     * Esta función lee la lista "cognito:groups" dentro del Access Token de AWS
     * y le pone el prefijo "ROLE_" para que hasRole("ADMIN") funcione correctamente.
     */
    private fun cognitoGroupsConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            // Extrae la lista de grupos asignados en AWS Cognito
            val groups = jwt.getClaimAsStringList("cognito:groups") ?: emptyList()
            // Transforma cada grupo, ej: "ADMIN" -> "ROLE_ADMIN"
            groups.map { SimpleGrantedAuthority("ROLE_$it") }
        }
        return converter
    }
}