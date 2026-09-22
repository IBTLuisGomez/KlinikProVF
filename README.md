# KlinikProBase

KlinikProBase es el nombre de referencia del proyecto de gestión clínica y administrativa que se está consolidando en una base técnica moderna con Spring Boot, PostgreSQL y autenticación JWT.

Este repositorio contiene dos realidades importantes que conviene reconocer claramente:

1. una versión funcional de gestión clínica en HTML/JavaScript local
2. una versión evolutiva en Java/Spring Boot con arquitectura más preparada para producción

## Estado actual del repositorio

La estructura del proyecto incluye:

- [KlinikProVF.html](KlinikProVF.html): prototipo funcional de la clínica en navegador
- [klinikpro-vf](klinikpro-vf): proyecto Spring Boot activo
- [klinikpro-vf/docker-compose.yml](klinikpro-vf/docker-compose.yml): configuración del servicio PostgreSQL
- [klinikpro-vf/pom.xml](klinikpro-vf/pom.xml): configuración Maven del proyecto
- [klinikpro-vf/src/main/java](klinikpro-vf/src/main/java): código Java de la aplicación
- [klinikpro-vf/src/test/java](klinikpro-vf/src/test/java): pruebas base del proyecto

## Objetivo del proyecto

KlinikPro busca digitalizar la operación de una clínica o consultorio con flujo de:

- pacientes
- especialidades y expertos
- citas y agenda
- disponibilidad por horario
- caja y cobros
- gastos y arqueo
- finanzas y reportes
- configuración de sucursal y clínica
- acceso seguro por usuarios y roles

## Arquitectura real del proyecto

### 1. Prototipo funcional local
La primera versión del sistema se basa en un único archivo HTML con CSS y JavaScript embebidos. Tiene persistencia local en IndexedDB y está orientada a uso inmediato en navegador.

Características principales:

- gestión de pacientes
- agenda diaria y mensual
- programación de citas
- seguimiento de tratamientos
- cash flow local
- reportes y exportación CSV/JSON
- configuración de clínica, sucursal y logo

Este prototipo es útil como base operativa y como referencia funcional.

### 2. Base técnica en Spring Boot
La carpeta [klinikpro-vf](klinikpro-vf) representa la evolución hacia una aplicación web con backend real, base de datos persistente y autenticación.

Tecnologías usadas:

- Java 25
- Spring Boot 4.1.1
- PostgreSQL 16
- Flyway
- JPA
- Spring Security
- JWT
- Maven
- Docker Compose

## Estructura principal del proyecto Java

### auth
La carpeta de autenticación incluye:

- `AuthController` — endpoints de login y registro
- `MeController` — endpoint para ver el contexto del usuario autenticado
- `JwtService` — creación y validación de JWT
- `JwtFilter` — validación de tokens por petición
- `SecurityConfig` — configuración de seguridad
- `User` — entidad de usuario
- `UserRepository` — acceso a usuarios
- `Role` — roles del sistema
- `TenantContext` — contexto multi-tenant para sucursal/empresa

### Componentes principales

- `KlinikproVfApplication` — clase principal de Spring Boot
- `PingController` — endpoint de comprobación del servicio

## Flujos funcionales actuales

### Autenticación
El sistema ya incorpora flujo de autenticación basado en JWT con:

- registro de clínica/tenant
- creación automática de sucursal primaria
- creación de usuario administrador
- login con email y password
- acceso protegido por token
- roles: ADMIN, COORDINADOR, FISIO, RECEPCION

### Multi-tenant / organización
La implementación contempla:

- tenantId por clínica
- branchId por sucursal
- contexto del usuario por request
- separación lógica por empresa/sucursal

### API de prueba
El proyecto tiene un endpoint de salud:

- `/api/ping`

También se registra el acceso autenticado:

- `/api/me`

## Cómo arrancar el proyecto

### 1. Levantar la base de datos
```bash
docker compose up -d db
```

### 2. Ejecutar la aplicación
```bash
./mvnw spring-boot:run
```

### 3. Verificar funcionamiento
Se puede consultar:

- `http://localhost:8080/api/ping`

## Configuración actual de PostgreSQL

En el archivo [klinikpro-vf/docker-compose.yml](klinikpro-vf/docker-compose.yml) se configuran estas credenciales:

- base de datos: `klinikpro`
- usuario: `klinik`
- password: `klinik_dev`
- puerto: `5432`

## Estado de desarrollo

El proyecto está en fase de consolidación técnica. Actualmente se observa un avance importante en:

- base de negocio y requisitos clínicos
- migración a backend Java
- configuración de seguridad
- autenticación y contexto multi-tenant
- preparación para persistencia real con PostgreSQL

Sin embargo, aún sigue siendo una base inicial, sin una capa completa de dominio ni una versión final de negocio completamente consolidada.

## Observaciones importantes

- El prototipo HTML sigue siendo una referencia funcional valiosa.
- El backend Spring Boot es la línea de evolución principal del proyecto.
- La aplicación aún requiere validación de flujo real completo y pruebas funcionales.
- El punto de arranque actual es técnico, pero no final en términos de producto terminado.

## Historial Git actual

El repositorio tiene un historial corto y claro:

- `426a19a` — feat: Initialize KlinikProVF project with Spring Boot and PostgreSQL
- `2fb05e3` — first commit

Esto demuestra que el proyecto comenzó con una base inicial y luego avanzó hacia una estructura moderna con Spring Boot y PostgreSQL.

## Autoría

El sistema está ligado a:

- Cytohelix Systems
- Luis Fernando Mendoza Gómez

## Resumen ejecutivo

KlinikProBase representa la transición de una solución clínica local y operativa hacia una plataforma más sólida, segura y mantenible con Spring Boot. La base funcional ya existe; la parte técnica y de seguridad está avanzando para convertirla en una versión más seria y preparada para crecimiento.
