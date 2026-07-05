# Deber: Roles y Autorización con AWS Cognito (Estacionamiento)

> **Curso:** Arquitectura Empresarial — PUCE · Kotlin + Spring Boot 4
> **Tema:** Autorización por roles con AWS Cognito (grupos → `ROLE_...` en Spring Security)
> **Modalidad:** Individual · todo por la consola web de AWS · sin defensa oral
> **Entrega:** Link del repositorio en GitHub + carpeta `evidence/` (capturas). Calificado sobre 100 pts.

> 📝 **Este documento parte de la corrección del examen.** El microservicio de estacionamiento (dos
> tablas, endpoint público de disponibles, entrada/salida privadas con JWT de Cognito) es exactamente
> lo que se pedía en el examen y **así se debía resolver**. Lo **nuevo** de este deber es:
> **1)** crear **tu propio User Pool** en Cognito y **2)** crear el **grupo `ADMIN`** para introducir
> **autorización por roles**. Todo lo demás ya lo hiciste en el examen: aquí solo lo extendemos.

## Contexto

En el examen construiste un microservicio de **estacionamiento (parking)** donde había endpoints
**públicos** (consultar espacios disponibles) y endpoints **privados** que exigían un **JWT válido**
emitido por Cognito. Hasta ahí solo distinguíamos **"con token" vs "sin token"**.

En este deber damos el siguiente paso: **autorización basada en roles**. Ya no basta con estar
autenticado; **quién eres** determina **qué puedes hacer**.

Reglas de negocio de este deber:

- Solo los usuarios con rol **`ADMIN`** pueden **crear espacios de estacionamiento** (dar de alta puestos).
- Solo los usuarios con rol **`USER`** pueden **registrar la entrada y la salida** de un vehículo en esos espacios.
- Un usuario `USER` que intente crear un espacio debe recibir **`403 Forbidden`**.
- Un usuario `ADMIN` que intente registrar entrada/salida debe recibir **`403 Forbidden`**.
- Una petición **sin token** debe recibir **`401 Unauthorized`** en cualquier endpoint privado.

Este documento es a la vez el **enunciado del deber** y una **guía paso a paso** para:
1. Configurar Spring Security para leer los **grupos de Cognito** y convertirlos en **roles**.
2. Crear **desde cero** un **User Pool en Cognito** con **grupos de usuarios** (todo manual, desde la consola de AWS).

> **Idea clave:** en Cognito, un **grupo** (`cognito:groups`) se traduce, dentro de tu microservicio,
> en un **rol** de Spring Security (`ROLE_...`). El backend **no** guarda usuarios ni roles en una tabla:
> confía en lo que dice el **JWT**.

---

## Objetivos de aprendizaje

Al terminar este deber deberías poder explicar:

- La diferencia entre **autenticación** (¿quién eres?) y **autorización** (¿qué puedes hacer?).
- Cómo un **grupo de Cognito** viaja dentro del JWT en el claim **`cognito:groups`**.
- Cómo mapear ese claim a **authorities/roles** de Spring con un `JwtAuthenticationConverter`.
- La diferencia entre **`401 Unauthorized`** (no autenticado) y **`403 Forbidden`** (autenticado pero sin permiso).
- Cómo proteger endpoints por **rol** con `authorizeHttpRequests` y/o `@PreAuthorize`.

---

## Nomenclatura

- **Repositorio:** `ae_2026_01_[nrc]_[nombre]_[apellido]_roles` (reemplaza los corchetes por tus datos, sin corchetes).
- **Paquete principal:** `com.pucetec.roles`.

---

## Requisitos técnicos

- **Kotlin** + **Spring Boot 4** + **Gradle Kotlin DSL** (`build.gradle.kts`) + **Java 21**.
- Arquitectura **en capas**: `controllers`, `services`, `repositories`, `entities`, `dto`, `mappers`, `exceptions`, `config`.
- **Mappers**: los endpoints exponen **DTOs**, nunca entities.
- **Excepciones personalizadas** manejadas por un `@RestControllerAdvice` con los códigos HTTP correctos.
- Logs con `LoggerFactory` en los services.
- Base de datos **PostgreSQL** con **docker-compose** (igual que en el proyecto de referencia). Configura la conexión en tu `application.yaml`.
- **Spring Security** como **OAuth2 Resource Server** validando **JWT** emitido por **tu propio User Pool de Cognito** (el que crearás en este deber).

### Dependencia adicional (Spring Boot 4)

Recuerda que en Boot 4 el starter cambió de nombre. En tu `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // 👇 NUEVO para este deber (nombre renombrado en Boot 4)
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

    // ... resto de tus dependencias de test
}
```

---

## Modelo de datos: SOLO DOS TABLAS

El dominio se modela con **exactamente dos tablas** relacionadas entre sí. La autenticación y los roles
**no** agregan tablas: todo viene en el JWT.

1. **Espacio de estacionamiento** (`ParkingSpace`): código/identificador del puesto y su estado (disponible u ocupado).
2. **Ticket**: placa del vehículo, hora de entrada, hora de salida (nula mientras el auto sigue adentro) y el espacio asignado.

Relación: al **entrar** se ocupa un espacio y se crea un ticket; al **salir** se cierra el ticket y se libera el espacio.

---

## Endpoints y su rol requerido

| Método | Endpoint | Acceso | Rol requerido |
|---|---|---|---|
| `GET` | `/parking-spaces/available` | **Público** | ninguno |
| `POST` | `/parking-spaces` | Privado | **`ADMIN`** |
| `POST` | `/tickets/entry` | Privado | **`USER`** |
| `POST` | `/tickets/exit` | Privado | **`USER`** |

Comportamiento esperado (esto es lo que se evalúa):

- `GET /parking-spaces/available` → **200** sin token.
- `POST /parking-spaces` sin token → **401**; con token de **USER** → **403**; con token de **ADMIN** → **201/200**.
- `POST /tickets/entry` sin token → **401**; con token de **ADMIN** → **403**; con token de **USER** → **201/200**.

---

# Parte 1 — Configurar los roles en Spring Security

## Paso 1.1 — Entender qué trae el JWT de Cognito

Cuando un usuario pertenece a uno o varios grupos, Cognito incluye en el **access token** un claim:

```json
{
  "sub": "…",
  "cognito:groups": ["ADMIN"],
  "token_use": "access",
  "scope": "…",
  "username": "juan"
}
```

Ese arreglo `cognito:groups` es **la fuente de la verdad** de los roles. Nuestro trabajo es leerlo y
convertir cada grupo en un authority de Spring con el prefijo **`ROLE_`** (que es lo que espera `hasRole(...)`).

> **Usa el `access_token`, no el `id_token`.** El claim `cognito:groups` viaja en el access token.

## Paso 1.2 — Configurar el `issuer-uri` en `application.yaml`

Con el **User Pool ID** que obtendrás en la Parte 2 (formato `us-east-1_XXXXXXXXX`):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXXXXX
```

> Ese `issuer-uri` es de **tu** User Pool (el que crearás tú). No uses el del examen.

## Paso 1.3 — Crear la clase de seguridad (`config/SecurityConfig.kt`)

Aquí ocurre la magia: convertimos `cognito:groups` en roles y protegemos cada endpoint por rol.

```kotlin
package com.pucetec.roles.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize si quieres usarlo en los controllers
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // Público
                auth.requestMatchers(HttpMethod.GET, "/parking-spaces/available").permitAll()

                // Solo ADMIN puede crear espacios
                auth.requestMatchers(HttpMethod.POST, "/parking-spaces").hasRole("ADMIN")

                // Solo USER puede registrar entrada/salida
                auth.requestMatchers(HttpMethod.POST, "/tickets/**").hasRole("USER")

                // Todo lo demás exige token válido
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(cognitoGroupsConverter()) }
            }
        return http.build()
    }

    /**
     * Lee el claim "cognito:groups" del JWT y lo convierte en roles de Spring.
     * Cada grupo "ADMIN" se transforma en la authority "ROLE_ADMIN",
     * que es exactamente lo que hasRole("ADMIN") busca.
     */
    private fun cognitoGroupsConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val groups = jwt.getClaimAsStringList("cognito:groups") ?: emptyList()
            groups.map { SimpleGrantedAuthority("ROLE_$it") }
        }
        return converter
    }
}
```

**Detalle importante sobre `hasRole` vs `hasAuthority`:**

- `hasRole("ADMIN")` internamente busca la authority **`ROLE_ADMIN`** (agrega el prefijo por ti).
- Por eso en el converter agregamos manualmente **`ROLE_`** delante del nombre del grupo.
- Los grupos en Cognito los llamaremos exactamente **`ADMIN`** y **`USER`** (sin el prefijo `ROLE_`).

## Paso 1.4 — (Opcional) Proteger también a nivel de método

Además de `authorizeHttpRequests`, puedes documentar la intención en el propio controller con `@PreAuthorize`
(funciona porque activamos `@EnableMethodSecurity`):

```kotlin
@PostMapping("/parking-spaces")
@PreAuthorize("hasRole('ADMIN')")
fun createSpace(@RequestBody request: CreateParkingSpaceRequest): ResponseEntity<ParkingSpaceResponse> { … }

@PostMapping("/tickets/entry")
@PreAuthorize("hasRole('USER')")
fun registerEntry(@RequestBody request: EntryRequest): ResponseEntity<TicketResponse> { … }
```

> Para este deber basta con **una** de las dos formas. Si usas ambas, deben ser **coherentes** entre sí.

---

# Parte 2 — Crear el User Pool en Cognito con grupos (manual)

> ⚠️ **Todo esta parte se hace por la INTERFAZ WEB de AWS (la consola en el navegador). Nada por CLI,
> nada por SDK, nada por Terraform.** Los roles se asignan **a mano**, agregando cada usuario a su grupo
> desde la consola.

## Paso 2.1 — Crear el User Pool

1. Entra a la **consola de AWS** → busca el servicio **Cognito**.
2. Asegúrate de estar en la región **`us-east-1`** (arriba a la derecha). Debe coincidir con tu `issuer-uri`.
3. Clic en **Create user pool**.
4. **Sign-in options:** elige **User name** (o **Email**, pero sé consistente). Continúa.
5. **Password policy:** deja la de Cognito (o una simple). **MFA:** selecciona **No MFA** para simplificar las pruebas.
6. **Self-service sign-up:** puedes **deshabilitarlo** (crearemos los usuarios a mano).
7. **App client:** crea un **Public client** (sin client secret) para poder pedir el token fácilmente desde la Hosted UI.
8. **Nombre del pool:** algo como `parking-roles-pool`.
9. Revisa y clic en **Create user pool**.

## Paso 2.2 — Anotar el User Pool ID y armar el `issuer-uri`

1. Abre tu pool recién creado.
2. Copia el **User Pool ID** (formato `us-east-1_XXXXXXXXX`).
3. Arma tu `issuer-uri` y ponlo en `application.yaml`:
   ```
   https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXXXXX
   ```

## Paso 2.3 — Usar el dominio POR DEFECTO / Hosted UI (para obtener tokens)

> **No crees un dominio propio (custom domain).** Usa el **dominio por defecto de Cognito** que AWS te
> asigna automáticamente al crear el pool (el que termina en `.auth.us-east-1.amazoncognito.com`).

1. En el pool → pestaña **App integration**.
2. En **Domain**, verás el **dominio Cognito por defecto** ya asignado. **Úsalo tal cual** (no registres uno nuevo).
3. Abre tu **App client** → en **Hosted UI / Login pages** configura (todo desde la web):
   - **Allowed callback URL:** `https://example.com` (o `http://localhost:8787`; solo se usa para recibir el token en la URL).
   - **OAuth grant types:** marca **Implicit** (para ver el token directamente en la URL) o **Authorization code grant**.
   - **OpenID Connect scopes:** `openid`, `email`, `profile`.
4. Guarda.

## Paso 2.4 — Crear los grupos (aquí nacen los roles)

1. En el pool → pestaña **Groups** → **Create group**.
2. Crea el grupo **`ADMIN`** (nombre exacto, en mayúsculas). Descripción: *Puede crear espacios de estacionamiento*.
3. Crea el grupo **`USER`**. Descripción: *Puede registrar entrada y salida de vehículos*.

> ⚠️ El **nombre del grupo** debe ser **exactamente** `ADMIN` y `USER`, porque el converter arma
> `ROLE_ADMIN` / `ROLE_USER` a partir de ese texto.

## Paso 2.5 — Crear usuarios de prueba

En la pestaña **Users** → **Create user**. Crea **dos** usuarios:

| Usuario | Grupo al que pertenecerá |
|---|---|
| `admin_parking` | `ADMIN` |
| `user_parking` | `USER` |

Para cada uno:
1. **Create user**.
2. Elige **Don't send an invitation** (o email si prefieres).
3. Asigna un **usuario** y una **contraseña permanente** (marca *Set password as permanent* / *Mark email as verified* para evitar el flujo de cambio de contraseña).

## Paso 2.6 — Asignar cada usuario a su grupo (ASIGNACIÓN MANUAL DE ROLES)

Este es el corazón del deber: **el rol se asigna a mano**.

1. Ve a **Users** → abre `admin_parking`.
2. Baja a la sección **Group memberships** → **Add user to group** → selecciona **`ADMIN`** → **Add**.
3. Repite con `user_parking`, agregándolo al grupo **`USER`**.

> Puedes verificarlo también desde la pestaña **Groups** → abrir el grupo → ver sus miembros.

## Paso 2.7 — Obtener un token y verificar los grupos

1. Construye la URL de login de la **Hosted UI** (la ves en *App integration → App client → View Hosted UI*), por ejemplo:
   ```
   https://TU-DOMINIO.auth.us-east-1.amazoncognito.com/login?client_id=TU_CLIENT_ID&response_type=token&scope=openid+email+profile&redirect_uri=https://example.com
   ```
2. Inicia sesión con `admin_parking`. Serás redirigido a `redirect_uri#access_token=...`.
3. Copia el **`access_token`** (el que está después de `access_token=`, **no** el `id_token`).
4. Pega el token en [https://jwt.io](https://jwt.io) y confirma que el payload contiene:
   ```json
   "cognito:groups": ["ADMIN"]
   ```
5. Repite con `user_parking` y confirma que trae `"cognito:groups": ["USER"]`.

> Si tu grant type es **Authorization code**, cambia `response_type=code` y luego intercambia el `code`
> por el token con un `curl` al endpoint `/oauth2/token` (mismo flujo del proyecto de referencia). Para
> este deber, **Implicit** (`response_type=token`) es lo más rápido para obtener el `access_token`.

---

# Parte 3 — Validaciones de negocio

Mantén las mismas validaciones del dominio de estacionamiento, cada una con su **excepción personalizada**
y su código HTTP:

1. Crear un espacio con un código **duplicado** → `409 Conflict`.
2. Registrar **entrada** en un espacio inexistente → `404 Not Found`.
3. Registrar **salida** de un ticket inexistente → `404 Not Found`.
4. **Estacionamiento lleno** (capacidad máxima como **variable del service**, ej. `private val capacidad = 20`) → `409 Conflict`.

Las **respuestas de seguridad** (`401` / `403`) las genera Spring Security automáticamente; **no** necesitas
excepciones personalizadas para ellas, pero **sí** debes evidenciarlas en Postman.

---

# Parte 4 — Pruebas (tests)

En la carpeta `src/test`:

1. **Test del service** que orquesta entrada/salida (usa dos repositorios): **100% de líneas y ramas**,
   mockeando ambos repositorios. Cubre el camino feliz y **cada** camino de error (espacio inexistente,
   ticket inexistente, estacionamiento lleno).
2. **Al menos un test de seguridad/web** que demuestre la autorización por rol. Con `spring-boot-starter-webmvc-test`
   y `spring-security-test` puedes simular usuarios con rol:

```kotlin
@Test
fun `un USER no puede crear espacios y recibe 403`() {
    mockMvc.perform(
        post("/parking-spaces").with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"code":"A1"}""")
    ).andExpect(status().isForbidden)
}

@Test
fun `un ADMIN si puede crear espacios`() {
    mockMvc.perform(
        post("/parking-spaces").with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"code":"A1"}""")
    ).andExpect(status().isCreated)
}
```

> Agrega `testImplementation("org.springframework.security:spring-security-test")` para usar `jwt()` y `.with(...)`.

---

## Entregables

1. **Link del repositorio** en GitHub (obligatorio). Incluye tu `docker-compose.yml`.
2. Una carpeta **`evidence/`** en la **raíz del repo** con **capturas tomadas desde la consola web de AWS**:
   - **Tu nuevo User Pool**: pantalla de resumen mostrando el **nombre del pool** y el **User Pool ID** (`us-east-1_XXXXXXXXX`).
   - **Los grupos** (`ADMIN` y `USER`) en la pestaña **Groups**.
   - **Las membresías**: captura de cada usuario mostrando a qué grupo pertenece (`admin_parking → ADMIN`, `user_parking → USER`).
   - **El App client** (pestaña App integration) y el **dominio por defecto** de Cognito que estás usando.
   - **Captura de jwt.io** decodificando el `access_token` de cada usuario, mostrando el claim `cognito:groups`.
   - **Capturas de Postman** de:
     - `GET /parking-spaces/available` respondiendo **200 sin token**.
     - `POST /parking-spaces` → **401 sin token**, **403 con token USER**, **201 con token ADMIN**.
     - `POST /tickets/entry` → **403 con token ADMIN**, **201 con token USER**.
   - La **colección de Postman** exportada.
   - Cualquier otra captura que consideres útil para demostrar que la autorización por roles funciona.
3. **Captura de Run with Coverage** mostrando el service de dos repositorios al **100% de líneas y ramas**.

---

## Calificación

Este deber se califica **sobre 100 puntos**, **toda de implementación** (no tiene defensa oral).

---

## Rúbrica de evaluación

| Criterio | Puntos |
|---|---|
| Arquitectura en capas correcta (incluye paquete `config`) | 10 |
| Exactamente **dos tablas** con su relación (roles **no** agregan tablas) | 8 |
| Mappers (no se exponen entities) + DTOs en los endpoints | 8 |
| Excepciones personalizadas + manejo con códigos HTTP correctos | 8 |
| **Tu propio User Pool** en Cognito + grupos `ADMIN` y `USER` (evidencia en consola) | 12 |
| `SecurityConfig` con `JwtAuthenticationConverter` que mapea `cognito:groups` → roles | 14 |
| **Solo `ADMIN` crea espacios** (USER recibe `403`) | 10 |
| **Solo `USER` registra entrada/salida** (ADMIN recibe `403`) | 10 |
| Endpoint público accesible sin token; privados devuelven `401` sin token | 6 |
| **100% de cobertura** del service de dos repositorios | 8 |
| Test de autorización por rol (`ADMIN` vs `USER`) | 6 |
| Carpeta `evidence/` completa (Postman con `401`/`403`/éxitos, jwt.io, capturas de Cognito) + link del repo | requisito para evaluar |
| **Total** | **100** |

---

## Lista de verificación

- [ ] Repositorio `ae_2026_01_[nrc]_[nombre]_[apellido]_roles` y paquete principal `com.pucetec.roles`.
- [ ] Kotlin + Spring Boot 4 + Gradle Kotlin DSL + Java 21, con PostgreSQL en **docker-compose**.
- [ ] Agregué `spring-boot-starter-security-oauth2-resource-server` (nombre de Boot 4).
- [ ] Creé **mi propio User Pool** en Cognito (`us-east-1`) y puse su `issuer-uri` en `application.yaml`.
- [ ] Creé los grupos **`ADMIN`** y **`USER`** (nombres exactos) en Cognito.
- [ ] Creé dos usuarios y los **asigné manualmente** a su grupo.
- [ ] Verifiqué en jwt.io que el `access_token` trae `cognito:groups`.
- [ ] `SecurityConfig` mapea `cognito:groups` a `ROLE_...` con `JwtAuthenticationConverter`.
- [ ] `POST /parking-spaces` solo para `ADMIN`; `POST /tickets/**` solo para `USER`.
- [ ] Endpoint de disponibles **público**; privados devuelven `401` sin token y `403` sin el rol correcto.
- [ ] Solo **dos tablas**; capacidad máxima como **variable del service**.
- [ ] Service de dos repositorios con **Line 100% y Branch 100%** + test de autorización por rol.
- [ ] Carpeta **`evidence/`** en la raíz con todas las capturas y la colección de Postman.
- [ ] Adjunté `docker-compose.yml`, captura de cobertura y **link del repositorio**.
</content>
</invoke>
