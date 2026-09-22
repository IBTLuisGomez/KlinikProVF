# KlinikProBase

KlinikProBase es la base documental y técnica del proyecto KlinikPro. En el estado actual del repositorio se observan dos líneas de trabajo:

1. un prototipo web local de gestión clínica desarrollado en un único archivo HTML
2. una versión más reciente del proyecto basada en Spring Boot con PostgreSQL, como base para una evolución más robusta y escalable

## Estado actual del repositorio

La estructura actual incluye:

- [KlinikProVF.html](KlinikProVF.html): prototipo funcional de la clínica con interfaz web e interacciones locales
- [klinikpro-vf](klinikpro-vf): proyecto activo en Java/Spring Boot
- [klinikpro-vf/docker-compose.yml](klinikpro-vf/docker-compose.yml): configuración del servicio PostgreSQL
- [klinikpro-vf/pom.xml](klinikpro-vf/pom.xml): configuración Maven del proyecto Spring Boot
- [klinikpro-vf/src/main/java](klinikpro-vf/src/main/java): clases Java principales
- [klinikpro-vf/src/test/java](klinikpro-vf/src/test/java): pruebas base del proyecto

## Visión general

El proyecto tiene como objetivo gestionar una clínica con flujo operativo de:

- pacientes
- agenda de citas
- disponibilidad y turnos
- caja y cobros
- gastos y arqueo
- finanzas y reportes
- configuración de sucursal y clínica

## Arquitectura actual

### 1. Prototipo HTML local
La versión inicial del sistema está contenida en el archivo [KlinikProVF.html](KlinikProVF.html). Tiene las siguientes características:

- interfaz web completa en una sola vista/archivo
- CSS integrado en la misma página
- lógica JavaScript embebida
- persistencia local con IndexedDB
- exportación/importación de datos JSON y CSV
- flujo operativo para pacientes, citas y caja

Este prototipo funciona como una herramienta de gestión local y sin backend.

### 2. Proyecto Java/Spring Boot
La carpeta [klinikpro-vf](klinikpro-vf) representa la evolución del proyecto hacia una arquitectura más sólida. Actualmente incluye:

- Java 25
- Spring Boot 4.1.1
- PostgreSQL 16
- Flyway
- JPA
- Actuator
- validación

La base funciona con una estructura típica de Spring Boot, con un controlador de prueba y configuración de base de datos.

## Estructura principal del proyecto Java

### Directorio src/main/java
Incluye la aplicación principal y la capa web inicial:

- `KlinikproVfApplication` — clase principal de Spring Boot
- `PingController` — endpoint de prueba para verificar la aplicación

### Directorio src/test/java
Tiene la prueba base de arranque del proyecto:

- `KlinikproVfApplicationTests`

## Cómo arrancar el proyecto actual

Se recomienda usar Docker para la base de datos y luego iniciar la app con Maven:

```bash
docker compose up -d db
./mvnw spring-boot:run
```

El proyecto está configurado para conectarse a PostgreSQL en el puerto 5432 con estas credenciales:

- base de datos: `klinikpro`
- usuario: `klinik`
- password: `klinik_dev`

## Endpoints relevantes

La app incluye un endpoint base para comprobación:

- `/api/ping`

Este endpoint responde con un JSON con información del servicio y estado.

## Estado de desarrollo

El repositorio está en una etapa inicial de transformación:

- el prototipo HTML es funcional y documentado como base del negocio
- el proyecto Spring Boot representa la migración técnica que se está preparando
- la Fase actual es de consolidación, configuración y documentación

## Observaciones importantes

Se recomienda considerar lo siguiente:

- el prototipo HTML no es la solución final en producción
- la versión Spring Boot aún está en base de arranque y estructura inicial
- la base de datos aún debe ser validada con una ejecución completa del proyecto
- la evolución del sistema debería contemplar separación por capas, autenticación, permisos, persistencia real y despliegue profesional

## Historial breve del repositorio

El proyecto tiene un historial muy corto en Git, con dos commits principales:

- `426a19a` — `feat: Initialize KlinikProVF project with Spring Boot and PostgreSQL`
- `2fb05e3` — `first commit`

## Objetivo del proyecto

KlinikProBase busca consolidar la gestión clínica en una herramienta útil, moderna y adaptable, partiendo de un flujo real de clínica y evolucionándolo hacia una solución web más estable, segura y mantenible.

## Autoría

El nombre y la marca del sistema están ligados a:

- Cytohelix Systems
- Luis Fernando Mendoza Gómez

## Resumen

El proyecto actualmente combina dos etapas:

- una base funcional local y operativa en HTML
- una base técnica nueva en Spring Boot + PostgreSQL para evolucionarlo

Esto hace que el repositorio sea una plataforma de transición entre una solución rápida y una versión más profesional y escalable.
