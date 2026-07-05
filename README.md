# Solución del examen de Estacionamiento (referencia)

Este repositorio es la **resolución de referencia** del microservicio de estacionamiento del examen,
con **autenticación básica** (JWT de Cognito: con token vs sin token).

> 🎯 **La autorización POR ROLES NO está implementada aquí a propósito.** Ese es **el deber del
> estudiante**: partir de esta solución y, siguiendo la guía [`homework_roles.md`](homework_roles.md),
> crear su propio User Pool, el grupo `ADMIN` (y `USER`) y restringir los endpoints por rol.

## Stack

- Kotlin 2.2.21 + Spring Boot 4.0.6 + Gradle Kotlin DSL + Java 21
- PostgreSQL (vía `docker-compose`)
- Spring Security como OAuth2 Resource Server validando JWT de Cognito
- Arquitectura en capas: `controllers`, `services`, `repositories`, `entities`, `dto`, `mappers`, `exceptions`, `config`

## Dominio (dos tablas)

- `ParkingSpace`: código del puesto + estado (`occupied`).
- `Ticket`: placa, hora de entrada, hora de salida (nula mientras el auto sigue adentro) y el espacio asignado.

## Endpoints (autenticación básica)

| Método | Endpoint | Acceso | Rol en el DEBER |
|---|---|---|---|
| `GET` | `/parking-spaces/available` | Público | — |
| `POST` | `/parking-spaces` | Requiere token | `ADMIN` |
| `POST` | `/tickets/entry` | Requiere token | `USER` |
| `POST` | `/tickets/exit` | Requiere token | `USER` |

En esta solución los endpoints privados solo exigen **token válido**. La columna "Rol en el DEBER"
es lo que el estudiante debe implementar después siguiendo `homework_roles.md`.

## Cómo correr

```bash
# 1) Base de datos
docker compose up -d

# 2) Configura tu issuer-uri de Cognito en src/main/resources/application.yaml

# 3) App
./gradlew bootRun

# 4) Tests (service de dos repositorios con 100% de cobertura)
./gradlew test
```

## Notas para clase

- Esta solución cubre lo que se pedía en el examen: dos tablas, endpoint público, entrada/salida
  privadas con JWT y validaciones de negocio, con el `TicketService` (dos repos) al 100% de cobertura.
- **El deber** (`homework_roles.md`) parte de aquí y agrega: **tu propio User Pool**, el **grupo `ADMIN`**
  (y `USER`) y la **autorización por roles** en `SecurityConfig`.
- Recuerda: `401` (sin token) lo genera Spring Security **antes** del controller; no se maneja en el
  `@RestControllerAdvice`.
