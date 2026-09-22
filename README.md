# KlinikProBase

KlinikProBase es la base de un sistema web de gestión clínica y administrativa para una clínica o consultorio, desarrollado en un único archivo HTML con estilos y lógica embebidos. Actualmente el proyecto está funcionando como una aplicación cliente local, sin backend ni servidor externo, pensada para ejecutarse en el navegador y guardar la información localmente.

## Estado actual del proyecto

El repositorio actual contiene principalmente:

- `KlinikProVF.html`: aplicación principal
- `README.md`: documentación del proyecto

La aplicación es una solución de tipo front-end puro (HTML + CSS + JavaScript), con persistencia en IndexedDB del navegador y soporte para exportación/importación de respaldos JSON y CSV.

## Descripción del sistema

La app está orientada a la administración clínica y financiera. Sus funcionalidades principales incluyen:

- Gestión de pacientes
- Registro y control de citas
- Agenda médica y disponibilidad por día
- Gestión de caja y arqueo
- Cobros, gastos e ingresos
- Control de cuentas por cobrar y por pagar
- Reportes y exportación de agenda
- Configuración de sucursal, datos de la clínica y logo
- Respaldos locales y sincronización CSV

## Arquitectura actual

### 1. Frontend monolítico
El proyecto no está separado en componentes ni en carpetas de frontend/backend. Todo se concentra en un solo archivo:

- `KlinikProVF.html`

Dentro de ese archivo se integran:

- HTML estructural
- CSS para diseño visual y UI responsiva
- JavaScript para toda la lógica de negocio
- IndexedDB para almacenamiento local

### 2. Persistencia de datos
La información se guarda en IndexedDB con varias colecciones, entre ellas:

- pacientes
- appointments
- transactions
- expenses
- cashcounts
- receivables
- payables
- bankmovs
- services
- specialists
- treatments
- cashsessions
- branches
- settings

Esto significa que la aplicación funciona sin conexión a un servidor central y guarda datos directamente en el navegador.

### 3. Modelo de negocio
La lógica de la app está construida para operar con:

- sucursales/múltiples sedes locales
- pacientes con expediente básico
- especialistas y tratantes
- servicios/tratamientos
- citas con serie y disponibilidad por horario
- caja por sesión día/fecha
- reportes financieros

## Funcionalidades visibles en la app

### Dashboard
El panel principal muestra:

- citas del día
- ingresos del día
- gastos del día
- pacientes registrados
- cuentas por cobrar
- cuentas por pagar
- próximas citas

### Pacientes
Permite:

- registrar nuevo paciente
- editar información básica
- buscar por nombre, teléfono o código
- agregar notas clínicas
- marcar si viene por aseguradora o derivación
- asignar especialista o tratante
- generar reportes por paciente

### Agenda
Incluye:

- calendario mensual
- selección de fecha
- disponibilidad por horario
- programación de citas
- citas en serie
- filtros por todas, hoy, próximas y pendientes
- exportación CSV de agenda

### Caja
La funcionalidad financiera cubre:

- apertura de caja
- cierre de caja
- registro de ingresos por cobro
- registro de gastos
- cálculo de arqueo
- pagos mixtos con varias formas de pago
- manejo de servicios y conceptos adicionales

### Finanzas
Incluye:

- cuentas por cobrar
- cuentas por pagar
- movimientos bancarios
- estados financieros básicos
- conciliación

### Ajustes
El módulo de configuración permite:

- cambiar nombre de la sucursal
- configurar clínica y responsable
- cargar logo para reportes
- activar sincronización local CSV
- exportar e importar respaldo .json
- reiniciar todos los datos

## Requisitos actuales

### Para usar la app
Se requiere un navegador moderno, preferentemente:

- Chrome
- Edge

### Compatibilidad recomendada
La aplicación menciona explícitamente que la sincronización con carpeta local CSV funciona mejor en:

- Chrome
- Edge

Firefox y Safari tienen limitaciones para esa funcionalidad.

## Cómo se usa actualmente

1. Abrir `KlinikProVF.html` en el navegador.
2. La aplicación carga la interfaz principal.
3. El sistema identifica una sucursal activa por defecto.
4. Los datos se guardan automáticamente en IndexedDB.
5. Se puede hacer respaldo y exportación manual o automática.

## Ventajas del estado actual

- No requiere instalación ni backend
- Es rápida de desplegar localmente
- Funciona sin conexión a internet
- Tiene una interfaz enfocada en clínica y caja
- Permite respaldo y exportación sencilla

## Limitaciones observadas

El proyecto tiene varias características que conviene reconocer con claridad:

- No es una aplicación multiusuario ni compartida en red
- No existe backend, API REST ni base de datos centralizada
- Los datos quedan ligados al navegador y equipo donde se usan
- La persistencia es local; no hay autenticación de usuarios ni roles complejos
- La lógica está concentrada en un solo archivo HTML, lo que puede crecer y volverse difícil de mantener
- El proyecto parece ser más una herramienta de gestión operativa local que una solución empresarial completa

## Observación de diseño

El sistema está más orientado a una clínica privada o consultorio con flujo operativo diario que a una solución de gran escala multiempresa. Tiene un enfoque práctico en:

- atención clínica
- agenda
- caja
- reportes inmediatos
- gestión local de expediente

## Nombre del proyecto

El nombre del proyecto en este README queda definido como:

- `KlinikProBase`

Esto representa la base funcional actual del sistema antes de cualquier evolución, refinamiento o reestructuración.

## Recomendación de evolución

Si se quiere convertir esta base en un proyecto más robusto, el siguiente paso natural sería:

1. separar frontend y backend
2. migrar a una base de datos real (MySQL, PostgreSQL, SQLite u otra)
3. añadir autenticación
4. definir roles y permisos
5. mover lógica a módulos o archivos organizados
6. preparar despliegue web profesional

## Resumen breve

KlinikProBase es una aplicación web de gestión clínica local, construida en un solo archivo HTML, con almacenamiento en IndexedDB, muy enfocada en administración de pacientes, agenda, caja y reportes operativos. Actualmente funciona como una herramienta de escritorio web local, útil y funcional, pero limitada por su arquitectura monolítica y por no contar con backend ni sincronización centralizada.

## Licencia y autoría

El propio archivo de la app indica que fue desarrollado por:

- Cytohelix Systems
- Autor y fundador: Luis Fernando Mendoza Gómez

El documento actual se deja como base documental del proyecto y puede servir como punto de partida para una versión más modular y profesional.
