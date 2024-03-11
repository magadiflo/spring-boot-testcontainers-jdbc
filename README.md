# [Spring Boot Testcontainers - Integration Testing made easy!](https://www.youtube.com/watch?v=erp-7MCK5BU)

Tutorial tomado del canal de **youtube de Dan Vega**.

En este tutorial, aprenderá cómo escribir una prueba de integración en Spring Boot usando Testcontainers. Usaremos
Testcontainers para poner en marcha una base de datos PostgreSQL en un contenedor y usarla para nuestras pruebas.
Escribiremos una prueba de repositorio y una prueba de controlador que llame al repositorio que se comunica con nuestra
base de datos.

---

## Dependencias

````xml
<!--Spring Boot 3.2.3-->
<!--Java 21-->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-docker-compose</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
````

### [Docker Compose Support in Spring Boot 3.1](https://spring.io/blog/2023/06/21/docker-compose-support-in-spring-boot-3-1)

En este tutorial trabajaremos con una nueva dependencia que nunca antes había usado. La dependencia
`Docker Compose Support`, proporciona compatibilidad con `Docker Compose` para mejorar la experiencia de desarrollo.

La etiqueta `optional` se establece en `true` si queremos usar otras funciones del proyecto pero excluir
la compatibilidad con `Docker Compose`:

````xml
<!--Dependencia por defecto creada desde Spring Initializr-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
````

El proyecto se generó desde `Spring Initializr`, allí agregamos la dependencia de `Docker Compose Support`, al hacer
esto, en automático nos crea el archivo `compose.yml` para trabajar con `docker compose`.

Por defecto, al crear el proyecto con la dependencia antes mencionada, se crea el archivo `compose.yml` con el
siguiente contenido:

````yml
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - 'POSTGRES_DB=mydatabase'
      - 'POSTGRES_PASSWORD=secret'
      - 'POSTGRES_USER=myuser'
    ports:
      - '5432'  
````

Normalmente, ejecutamos `docker compose up` para iniciar y `docker compose down` para detener nuestros contenedores
basados en `compose.yml`. Ahora podemos delegar esos comandos de `Docker Compose a Spring Boot 3`. Mientras la
aplicación Spring Boot se inicia o se detiene, también administrará nuestros contenedores.

Además, tiene administración incorporada para múltiples servicios, como bases de datos `SQL, MongoDB, Cassandra, etc.`
Por lo tanto, es posible que no necesitemos clases de configuración o propiedades para duplicar en el archivo de
recursos de nuestra aplicación.

Finalmente, veremos que podemos usar este soporte con imágenes personalizadas de Docker y perfiles de Docker Compose.

`Spring Boot 3.1` detectará que hay un archivo Docker Compose presente y ejecutará `docker compose up` por usted antes
de conectarse a los servicios. Si los servicios ya se están ejecutando, también lo detectará y los utilizará. También
ejecutará `docker compose stop` cuando la aplicación se cierre. Atrás quedaron los días en los que los contenedores
Docker persistentes consumían su preciosa memoria.

Las imágenes iniciadas por Docker Compose se detectan y utilizan automáticamente para crear beans ConnectionDetails que
apunten a los servicios. Eso significa que no tiene que poner propiedades en su configuración, no tiene que recordar
cómo construir URL JDBC de PostgreSQL, etc.

Con `Spring Boot 3.1`, todo lo que necesita hacer es proporcionar el archivo `compose.yaml` y dejar que Spring Boot se
encargue del resto. ¡Simplemente funciona!

