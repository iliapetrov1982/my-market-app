package de.petrov.ya.java.mymarketapp.entity.order;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "orders")
@Setter
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    private Long id;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("total_sum")
    private Long totalSum;

    @Column("username")
    private String username;

    /**
     * В БД это хранится в таблице order_items.
     * Здесь поле только для удобства (DTO/рендеринга), Spring Data его не сохраняет.
     */
    @Transient
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long totalSum, String username) {
        this.totalSum = totalSum;
        this.username = username;
        this.createdAt = OffsetDateTime.now();
    }
}
