package dev.magadiflo.testcontainers.app.post;

import org.springframework.data.repository.ListCrudRepository;

public interface PostRepository extends ListCrudRepository<Post, Integer> {
    Post findByTitle(String title);
}
