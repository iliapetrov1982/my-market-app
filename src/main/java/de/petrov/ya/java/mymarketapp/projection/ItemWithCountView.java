package de.petrov.ya.java.mymarketapp.projection;

import de.petrov.ya.java.mymarketapp.entity.Item;

public interface ItemWithCountView {
    Item getItem();
    Integer getCount();
}
