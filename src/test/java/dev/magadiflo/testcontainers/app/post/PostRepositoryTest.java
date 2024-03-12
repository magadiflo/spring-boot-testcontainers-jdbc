package dev.magadiflo.testcontainers.app.post;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

@Testcontainers
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:15.2-alpine");

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        List<Post> posts = List.of(
                new Post(1, 1, "Hello World!", "This is my first post", null),
                new Post(2, 1, "Hi", "How are you?", null)
        );
        this.postRepository.saveAll(posts);
    }

    @Test
    void connectionEstablished() {
        Assertions.assertTrue(POSTGRES_CONTAINER.isCreated());
        Assertions.assertTrue(POSTGRES_CONTAINER.isRunning());
    }

    @Test
    void shouldReturnPostByTitle() {
        Post post = this.postRepository.findByTitle("Hello World!");
        Assertions.assertNotNull(post);
    }

    @Test
    void shouldDeletePostById() {
        int postId = 1;

        Optional<Post> postOptional = this.postRepository.findById(postId);
        Assertions.assertTrue(postOptional.isPresent());

        this.postRepository.deleteById(postId);

        postOptional = this.postRepository.findById(postId);
        Assertions.assertTrue(postOptional.isEmpty());
    }

    @Test
    void shouldDeleteAllPosts() {
        this.postRepository.deleteAll();
        Assertions.assertEquals(0, this.postRepository.count());
    }
}