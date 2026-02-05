package de.petrov.ya.java.mymarketapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Entity
@Table(name = "items")

@Setter
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "img_path", nullable = false, length = 512)
    private String imgPath;

    @Column(nullable = false)
    private Long price;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Item(String title, String description, String imgPath, Long price) {
        this.title = title;
        this.description = description;
        this.imgPath = imgPath;
        this.price = price;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
