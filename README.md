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
        <artifactId>spring-boot-starter-validation</artifactId>
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
apunten a los servicios. **Eso significa que no tiene que poner propiedades en su configuración, no tiene que recordar
cómo construir URL JDBC de PostgreSQL, etc.**

Con `Spring Boot 3.1`, todo lo que necesita hacer es proporcionar el archivo `compose.yaml` y dejar que Spring Boot se
encargue del resto. **¡Simplemente funciona!**

## Clases e interfaces iniciales

Creamos la entidad que estará mapeado a la tabla `posts` de la base de datos. En esta oportunidad usaremos un `record`:

````java

@Table(name = "posts")
public record Post(
        @Id
        Integer id,
        Integer userId,
        @NotBlank
        String title,
        @NotBlank
        String body,
        @Version
        Integer version) {
}
````

Creamos un repositorio que herede de la interfaz `ListCrudRepository`, quien es una interfaz para operaciones `CRUD`
genéricas en un repositorio para un tipo específico. Esta es una extensión de `CrudRepository` que devuelve `List` en
lugar de Iterable cuando corresponda.

````java
public interface PostRepository extends ListCrudRepository<Post, Integer> {
}
````

Creamos un record que contendrá una lista de `Post`. Usaremos el `Posts` para poder poblar la tabla en la base de datos:

````java
public record Posts(List<Post> posts) {
}
````

Finalmente, creamos una clase de componente que nos permitirá poblar la tabla `posts` en la base de datos. Los datos
estarán en un archivo `json` llamado `posts.json` ubicados en el directorio `resources/data`. Obtendremos los datos del
archivo `json` y lo convertiremos a un objeto del tipo `Posts`, quien finalmente será usado para persistir en la BD
usando el `postRepository`:

````java

@Component
public class PostDataLoader implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PostDataLoader.class);
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;

    public PostDataLoader(ObjectMapper objectMapper, PostRepository postRepository) {
        this.objectMapper = objectMapper;
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (this.postRepository.count() == 0) {
            String POSTS_JSON = "/data/posts.json";
            LOG.info("Cargando posts dentro de la base de datos desde JSON: {}", POSTS_JSON);

            try (InputStream inputStream = TypeReference.class.getResourceAsStream(POSTS_JSON)) {

                Posts posts = this.objectMapper.readValue(inputStream, Posts.class);
                this.postRepository.saveAll(posts.posts());

            } catch (IOException e) {
                throw new RuntimeException("Falló al leer datos del JSON", e);
            }
        }
    }
}
````

**DONDE**

- `InputStream`, esta clase abstracta es la superclase de todas las clases que representan un flujo de entrada de bytes.
  La clase abstracta `InputStream` declara los métodos para leer datos desde una fuente concreta y es la clase base de
  la mayor parte de los flujos de entrada en `java.io`, permite leer ficheros byte a byte.
- `TypeReference.class.getResourceAsStream()`, como parámetro recibe el nombre del `recurso deseado` y como valor de
  retorno devuelve un objeto `InputStream`; `null` si no se encuentra ningún recurso con ese nombre, el recurso está en
  un paquete que no está abierto al menos al módulo llamante, o el administrador de seguridad deniega el acceso al
  recurso.

## Configuraciones

Creamos el siguiente directorio y archivo `/resources/data/posts.json`. Dentro de él, colocaremos el siguiente
contenido:

````bash
{
  "posts": [
    {
      "userId": 1,
      "id": 1,
      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
      "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum est autem sunt rem eveniet architecto"
    },
    {
      "userId": 1,
      "id": 2,
      "title": "qui est esse",
      "body": "est rerum tempore vitae\nsequi sint nihil reprehenderit dolor beatae ea dolores neque\nfugiat blanditiis voluptate porro vel nihil molestiae ut reiciendis\nqui aperiam non debitis possimus qui neque nisi nulla"
    },
    {...}
  ]
}
````

En el `application.yml` agregamos las siguientes configuraciones:

````yml
spring:
  application:
    name: spring-boot-testcontainers-jdbc

  sql:
    init:
      mode: always
````

Recordar que la segunda configuración `spring.sql.init.mode=always` permite que el archivo `schema.sql` pueda
ejecutarse.

Ahora, el archivo `schema.sql` contendrá el `DDL` de nuestra tabla `posts`:

````sql
CREATE TABLE IF NOT EXISTS posts(
    id INT NOT NULL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(250) NOT NULL,
    body TEXT NOT NULL,
    version INT
);
````

Finalmente, nuestro archivo `compose.yml` quedará configurado de la siguiente manera:

````yaml
services:
  postgres:
    container_name: postgres
    image: postgres:15.2-alpine
    environment:
      POSTGRES_DB: db_testcontainers_jdbc
      POSTGRES_PASSWORD: magadiflo
      POSTGRES_USER: magadiflo
    ports:
      - 5433:5432
````

## Ejecutando aplicación

Como estamos usando la dependencia de `Docker Compose Support` es importante que `Docker` esté ejecutándose previamente,
ya que la dependencia hará uso de `docker compose` para poder levantar el contenedor que tenemos definido en el archivo
`compose.yaml`, además, al levantar el proyecto no solo se creará el contenedor de la base de datos en automático, sino
también se poblará con los datos definidos en el archivo `data/posts.json`.

Antes de ejecutar la aplicación vemos que no hay ningún contenedor ejecutándose:

````bash
$ docker container ls -a
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
````

Después de ejecutar la aplicación:

````bash
$ docker container ls -a
CONTAINER ID   IMAGE                  COMMAND                  CREATED          STATUS          PORTS                    NAMES
2046394862cf   postgres:15.2-alpine   "docker-entrypoint.s…"   13 seconds ago   Up 12 seconds   0.0.0.0:5433->5432/tcp   postgres
````

Si vemos el log generado en el IDE:

````bash
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] d.m.testcontainers.app.MainApplication   : Starting MainApplication using Java 21.0.1 with PID 11500 (M:\PROGRAMACION\DESARROLLO_JAVA_SPRING\02.youtube\15.dan_vega\spring-boot-testcontainers-jdbc\target\classes started by USUARIO in M:\PROGRAMACION\DESARROLLO_JAVA_SPRING\02.youtube\15.dan_vega\spring-boot-testcontainers-jdbc)
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] d.m.testcontainers.app.MainApplication   : No active profile set, falling back to 1 default profile: "default"
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] .s.b.d.c.l.DockerComposeLifecycleManager : Using Docker Compose file 'M:\PROGRAMACION\DESARROLLO_JAVA_SPRING\02.youtube\15.dan_vega\spring-boot-testcontainers-jdbc\compose.yaml'
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Network spring-boot-testcontainers-jdbc_default  Creating
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Network spring-boot-testcontainers-jdbc_default  Created
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Container postgres  Creating
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Container postgres  Created
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Container postgres  Starting
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Container postgres  Started
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Container postgres  Waiting
INFO 11500 --- [spring-boot-testcontainers-jdbc] [utReader-stderr] o.s.boot.docker.compose.core.DockerCli   :  Container postgres  Healthy
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JDBC repositories in DEFAULT mode.
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 118 ms. Found 1 JDBC repository interface.
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.19]
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 2728 ms
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@9cfc77
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path ''
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] d.m.testcontainers.app.MainApplication   : Started MainApplication in 29.787 seconds (process running for 30.772)
INFO 11500 --- [spring-boot-testcontainers-jdbc] [           main] d.m.t.app.post.PostDataLoader            : Cargando posts dentro de la base de datos desde JSON: /data/posts.json
````

Aunque vemos en el `log` el mensaje de que los datos se han cargado correctamente en la base de datos, vamos a
comprobarlo por nuestra cuenta si eso es cierto:

````bash
$ docker container exec -it postgres /bin/sh
/ # psql -U magadiflo -d db_testcontainers_jdbc
psql (15.2)
Type "help" for help.

db_testcontainers_jdbc=# \d
         List of relations
 Schema | Name  | Type  |   Owner
--------+-------+-------+-----------
 public | posts | table | magadiflo
(1 row)

db_testcontainers_jdbc=# SELECT * FROM posts WHERE id = 1;
 id | user_id |                                   title                                    |                        body                         | version
----+---------+----------------------------------------------------------------------------+-----------------------------------------------------+---------
  1 |       1 | sunt aut facere repellat provident occaecati excepturi optio reprehenderit | quia et suscipit                                   +|       0
    |         |                                                                            | suscipit recusandae consequuntur expedita et cum   +|
    |         |                                                                            | reprehenderit molestiae ut ut quas totam           +|
    |         |                                                                            | nostrum rerum est autem sunt rem eveniet architecto |
(1 row)
````

Si detenemos la aplicación, el contenedor en Docker quedará en `exited`:

````bash
$ docker container ls -a
CONTAINER ID   IMAGE                  COMMAND                  CREATED         STATUS                     PORTS     NAMES
2046394862cf   postgres:15.2-alpine   "docker-entrypoint.s…"   5 minutes ago   Exited (0) 4 seconds ago             postgres
````
