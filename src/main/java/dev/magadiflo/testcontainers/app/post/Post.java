package dev.magadiflo.testcontainers.app.post;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

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
