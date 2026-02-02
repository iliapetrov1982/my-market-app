package de.petrov.ya.java.mymarketapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "cart_items")

@Setter
@Getter
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {
    @Id
    @Column(name = "item_id")
    private Long itemId;

    @MapsId
    @JoinColumn(name = "item_id")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
