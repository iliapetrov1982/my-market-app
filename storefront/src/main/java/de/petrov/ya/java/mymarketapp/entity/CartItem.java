package de.petrov.ya.java.mymarketapp.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Позиция корзины.
 *
 * PK в БД — составной (item_id, username), но Spring Data R2DBC
 * работает с одним @Id. В качестве @Id оставляем item_id,
 * а username используем как дополнительное поле фильтрации.
 * Для корректного UPSERT используем флаг {@code isNew}.
 */
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

    @Column("username")
    private String username;

    @Transient
    private boolean isNew = false;

    public CartItem(Long itemId, int quantity, String username) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.username = username;
    }

    /**
     * Фабричный метод для создания новой строки (INSERT).
     */
    public static CartItem newRow(long itemId, int quantity, String username) {
        CartItem ci = new CartItem(itemId, quantity, username);
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
