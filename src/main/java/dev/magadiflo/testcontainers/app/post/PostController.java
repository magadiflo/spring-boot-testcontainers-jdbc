package dev.magadiflo.testcontainers.app.post;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/posts")
public class PostController {

    private static final Logger LOG = LoggerFactory.getLogger(PostDataLoader.class);
    private final PostRepository postRepository;

    public PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping
    public ResponseEntity<List<Post>> findAllPosts() {
        return ResponseEntity.ok(this.postRepository.findAll());
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Post> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(this.postRepository.findById(id).orElseThrow(PostNotFoundException::new));
    }

    @PostMapping
    public ResponseEntity<Post> savePost(@Valid @RequestBody Post post) {
        Post postDB = this.postRepository.save(post);
        URI uri = URI.create("/api/v1/posts/" + postDB.id());
        return ResponseEntity.created(uri).body(postDB);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Integer id, @RequestBody Post post) {
        return this.postRepository.findById(id)
                .map(postDB -> {

                    Post updatedPost = new Post(postDB.id(),
                            post.userId(),
                            post.title(),
                            post.body(),
                            post.version());
                    return ResponseEntity.ok(this.postRepository.save(updatedPost));
                })
                .orElseThrow(PostNotFoundException::new);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Integer id) {
        return this.postRepository.findById(id)
                .map(postDB -> {
                    this.postRepository.deleteById(postDB.id());
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseThrow(PostNotFoundException::new);
    }

}
