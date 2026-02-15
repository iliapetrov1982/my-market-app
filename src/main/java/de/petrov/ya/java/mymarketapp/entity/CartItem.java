package de.petrov.ya.java.mymarketapp.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Table(name = "cart_items")
@Setter
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem implements Persistable<Long> {
    @Id
    @Column("item_id")
    private Long itemId;

    @Column("quantity")
    private int quantity;

    @Transient
    private boolean isNew = false;

    public CartItem(Long item, int quantity) {
        this.itemId = item;
        this.quantity = quantity;
    }

    public static CartItem newRow(long itemId, int quantity) {
        CartItem ci = new CartItem(itemId, quantity);
        ci.isNew = true;
        return ci;
    }

    @Override
    public Long getId() {
        return itemId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
