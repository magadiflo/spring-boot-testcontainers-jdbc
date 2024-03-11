package dev.magadiflo.testcontainers.app.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

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