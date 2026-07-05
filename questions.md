# Preguntas de repaso — Roles y Autorización con Cognito

Estas 10 preguntas cubren lo que aprendiste en la guía `homework_roles.md`. Debes poder
responderlas explicando **tu propio código** y tu **User Pool**.

1. ¿Cuál es la diferencia entre **autenticación** y **autorización**? En el estacionamiento,
   ¿qué parte resuelve el token "con/sin" y qué parte resuelven los roles?

2. Cuando un usuario pertenece a un grupo en Cognito, ¿en qué **claim** del JWT viaja esa
   información y qué forma tiene (tipo de dato)? ¿Por qué usamos el **`access_token`** y no el `id_token`?

3. En tu `JwtAuthenticationConverter`, ¿por qué antepones el prefijo **`ROLE_`** al nombre del grupo?
   ¿Qué pasaría si el grupo en Cognito se llamara `ROLE_ADMIN` en lugar de `ADMIN`?

4. Explica la diferencia entre `hasRole("ADMIN")` y `hasAuthority("ADMIN")`. ¿Con cuál funciona
   tu converter tal como está escrito y por qué?

5. ¿Qué código HTTP responde el sistema a una petición **sin token** y cuál a un usuario
   **autenticado pero sin el rol correcto**? ¿Quién genera esas respuestas: tu `@RestControllerAdvice`
   o Spring Security? ¿Por qué?

6. Tu microservicio **no** tiene tabla de usuarios ni de roles. Entonces, ¿de dónde saca Spring
   la identidad y los permisos del usuario en cada petición? ¿Qué valida exactamente contra el `issuer-uri`?

7. En `SecurityConfig`, ¿por qué el orden de las reglas de `authorizeHttpRequests` importa? ¿Qué pasaría
   si `anyRequest().authenticated()` estuviera **antes** de las reglas específicas de `/parking-spaces`?

8. Describe, paso a paso y **desde la consola web de AWS**, cómo asignaste el rol `ADMIN` a un usuario.
   ¿En qué momento ese usuario "se convierte" en `ROLE_ADMIN` dentro de tu aplicación?

9. Un compañero inicia sesión, obtiene su token y **luego** lo agregas al grupo `ADMIN` en Cognito.
   ¿Puede crear espacios inmediatamente con ese token, o necesita algo más? Justifica.

10. Si quisieras un tercer rol (por ejemplo `SUPERVISOR`) que pueda **consultar todos los tickets**,
    ¿qué cambios harías en Cognito y qué cambios en `SecurityConfig`? ¿Tendrías que tocar el converter?
