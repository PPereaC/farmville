# Call2Action: Farmville exercise

## 📚 Información Académica

**Asignatura:** Desarrollo en Entorno Servidor (Backend)  
**Tarea:** [Enlace a Google Classroom](https://classroom.google.com/u/2/c/MjM1MDUyMDEzNDBa/a/MjE5Mjk5MjMzMDRa/details)

## 📝 Descripción del Proyecto

Este proyecto implementa un sistema de carga masiva de datos desde archivos CSV a una base de datos PostgreSQL para **Farmville**. El programa gestiona la importación de diferentes entidades (granjeros, plantaciones, riegos, construcciones, tractores y relaciones entre granjeros) con un robusto sistema de manejo de transacciones, errores y duplicados.

## 🎯 Características Principales

- ✅ **Carga de datos desde CSV** con estructura de directorios preservada
- ✅ **Gestión de duplicados**: detecta registros duplicados y actualiza solo los campos modificados
- ✅ **Transacciones con SavePoints**: rollback parcial en caso de error sin afectar datos ya cargados
- ✅ **Logging completo**: registro de errores y duplicados en archivos separados
- ✅ **Validación de integridad referencial**: manejo de claves foráneas y valores NULL
- ✅ **Try-with-resources**: cierre automático de recursos JDBC
- ✅ **Logger profesional**: uso de SLF4J/Logback en lugar de System.out
- ✅ **Configuración externa**: parámetros de conexión y rutas en archivo de propiedades

## 🏗️ Estructura del Proyecto

```
farmville/
├── src/main/java/com/ppereac/
│   ├── Main.java                    # Punto de entrada de la aplicación
│   ├── service/
│   │   └── CsvLoader.java           # Lógica de carga de CSV y procesamiento
│   └── util/
│       ├── DatabaseConnection.java  # Gestión de conexión a BD
│       └── LogHelper.java           # Utilidades de logging
├── datos/                           # Archivos CSV organizados por entidad
│   ├── granjeros/
│   ├── plantaciones/
│   ├── riegos/
│   ├── construcciones/
│   ├── tractores/
│   └── granjero_granjero/
├── config.properties                # Configuración de BD y rutas
└── pom.xml                         # Dependencias Maven
```

## 🔧 Tecnologías Utilizadas

- **Java 17+**
- **JDBC** - Conexión a base de datos
- **PostgreSQL** - Sistema de gestión de base de datos
- **Apache Commons CSV** - Lectura y parseo de archivos CSV
- **SLF4J + Logback** - Sistema de logging
- **Maven** - Gestión de dependencias

## 🚀 Instalación y Ejecución

### 1. Requisitos Previos

- JDK 17 o superior
- PostgreSQL instalado y configurado
- Maven (opcional, para compilar)

### 2. Configurar la Base de Datos

Ejecutar el script SQL proporcionado para crear la base de datos `farmville` y sus tablas.

### 3. Configurar el Archivo de Propiedades

Editar `config.properties` con los datos de tu conexión:

```properties
db.url=jdbc:postgresql://localhost:5432/farmville
db.user=tu_usuario
db.password=tu_contraseña
ruta.csv=datos/
ruta.errores=errores.log
ruta.duplicados=duplicados.log
```

### 4. Compilar el Proyecto

```bash
mvn clean package
```

### 5. Ejecutar el Programa

```bash
java -jar target/farmville-1.0-SNAPSHOT.jar config.properties
```

O desde el IDE ejecutando la clase `Main.java` con el argumento `config.properties`.

## 📊 Orden de Carga

El programa procesa los archivos CSV en el siguiente orden (respetando dependencias de claves foráneas):

1. **Granjeros** → Base de datos de usuarios
2. **Plantaciones** → Requiere granjeros
3. **Riegos** → Requiere plantaciones
4. **Construcciones** → Requiere granjeros
5. **Tractores** → Requiere construcciones
6. **Granjero_Granjero** → Relaciones entre granjeros

## 🔍 Manejo de Errores

### Errores de Integridad
Si un registro viola restricciones de la BD (ej: clave foránea inexistente):
- Se hace **rollback** solo de la tabla actual (usando SavePoints)
- Se registra el error en `errores.log`
- **El programa se detiene** para que se corrija el CSV
- Las tablas anteriores ya procesadas **se mantienen** (COMMIT realizado)

### Duplicados
Si un registro ya existe:
- **Con cambios**: se realiza UPDATE automático
- **Sin cambios**: se registra en `duplicados.log` sin provocar error

## 📄 Archivos de Log

- **errores.log**: Acumula todos los errores críticos durante la ejecución
- **duplicados.log**: Registra intentos de inserción de datos ya existentes

## 👨‍💻 Autor

Proyecto desarrollado para la asignatura de **Desarrollo en Entorno Servidor** por **Pablo Perea Campos**

## 📋 Notas Importantes

⚠️ **Los archivos CSV contienen errores intencionales** que deben ser corregidos consultando con el profesor.

⚠️ **La estructura de la carpeta `datos/`** debe mantenerse exactamente como se proporciona en el ZIP original.

---

*Última actualización: Febrero 2026*
