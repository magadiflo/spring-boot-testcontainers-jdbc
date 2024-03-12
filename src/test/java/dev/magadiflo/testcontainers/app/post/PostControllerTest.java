package dev.magadiflo.testcontainers.app.post;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

/**
 * Prueba de integración
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostControllerTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:15.2-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void connectionEstablished() {
        Assertions.assertTrue(POSTGRES_CONTAINER.isCreated());
        Assertions.assertTrue(POSTGRES_CONTAINER.isRunning());
    }

    @Test
    void shouldFindAllPosts() {
        Post[] posts = this.restTemplate.getForObject("/api/v1/posts", Post[].class);
        Assertions.assertEquals(100, posts.length);
    }

    @Test
    void shouldFindPostWhenValidPostId() {
        ResponseEntity<Post> response = this.restTemplate.exchange("/api/v1/posts/{id}", HttpMethod.GET, null, Post.class, Collections.singletonMap("id", 1));
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    void shouldThrowNotFoundWhenInvalidPostId() {
        ResponseEntity<Post> response = this.restTemplate.exchange("/api/v1/posts/{id}", HttpMethod.GET, null, Post.class, Collections.singletonMap("id", 500));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldCreateNewPostWhenPostIsValid() {
        Post post = new Post(101, 1, "Post 101", "Body 101", null);

        ResponseEntity<Post> response = this.restTemplate.exchange("/api/v1/posts", HttpMethod.POST, new HttpEntity<>(post), Post.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(101, response.getBody().id());
        Assertions.assertEquals(1, response.getBody().userId());
        Assertions.assertEquals("Post 101", response.getBody().title());
        Assertions.assertEquals("Body 101", response.getBody().body());
    }

    @Test
    void shouldNotCreateNewPostWhenValidationFails() {
        Post post = new Post(101, 1, " ", null, null);

        ResponseEntity<Post> response = this.restTemplate.exchange("/api/v1/posts", HttpMethod.POST, new HttpEntity<>(post), Post.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldUpdatePostWhenPostIsValid() {
        ResponseEntity<Post> responseGet = this.restTemplate.exchange("/api/v1/posts/{id}", HttpMethod.GET, null, Post.class, Collections.singletonMap("id", 99));
        Assertions.assertEquals(HttpStatus.OK, responseGet.getStatusCode());
        Assertions.assertNotNull(responseGet.getBody());

        Post postToUpdate = new Post(null, responseGet.getBody().userId(), "New post title #1", "New post body #1", responseGet.getBody().version());
        ResponseEntity<Post> responsePut = this.restTemplate.exchange("/api/v1/posts/{id}", HttpMethod.PUT, new HttpEntity<>(postToUpdate), Post.class, Collections.singletonMap("id", 99));

        Assertions.assertEquals(HttpStatus.OK, responsePut.getStatusCode());
        Assertions.assertNotNull(responsePut.getBody());

        Assertions.assertEquals(99, responsePut.getBody().id());
        Assertions.assertEquals(postToUpdate.title(), responsePut.getBody().title());
        Assertions.assertEquals(postToUpdate.body(), responsePut.getBody().body());
        Assertions.assertEquals(postToUpdate.version() + 1, responsePut.getBody().version());
    }

    @Test
    void shouldDeleteWithValidId() {
        ResponseEntity<Void> response = this.restTemplate.exchange("/api/v1/posts/{id}", HttpMethod.DELETE, null, Void.class, Collections.singletonMap("id", 80));
        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}