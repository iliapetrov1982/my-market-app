package de.petrov.ya.java.mymarketapp.entity.order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Table(name = "order_items")
@Setter
@Getter
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class OrderItem implements Persistable<OrderItemId> {

    @Id
    private OrderItemId id;

    @Column("title")
    private String title;

    @Column("price")
    private Long price;

    @Column("quantity")
    private Integer quantity;

    @Transient
    private boolean isNew = false;

    // этот конструктор используется при чтении из БД -> сущность НЕ новая
    @PersistenceCreator
    public OrderItem(OrderItemId id, String title, Long price, Integer quantity) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.isNew = false;
    }

    // этот конструктор ты используешь при создании -> сущность НОВАЯ
    public OrderItem(Long orderId, Long itemId, String title, Long price, Integer quantity) {
        this.id = new OrderItemId(orderId, itemId);
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.isNew = true;
    }

    @Override
    public OrderItemId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Long getOrderId() {
        return id == null ? null : id.getOrderId();
    }

    public Long getItemId() {
        return id == null ? null : id.getItemId();
    }
}