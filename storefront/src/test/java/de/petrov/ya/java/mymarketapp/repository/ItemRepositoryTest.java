package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.MyMarketAppApplicationTests;
import de.petrov.ya.java.mymarketapp.entity.Item;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

class ItemRepositoryTest extends MyMarketAppApplicationTests {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void cleanCartOnly() {
        // items — из seed, не трогаем
        cartItemRepository.deleteAll().block();
    }

    @Test
    void seed_hasAtLeast10Items_andFieldsAreValid() {
        List<Item> items = itemRepository.findAll()
                .collectList()
                .block();

        assertThat(items, notNullValue());
        assertThat("В seed должно быть не меньше 10 товаров", items.size(), greaterThanOrEqualTo(10));

        for (Item it : items) {
            assertThat(it.getId(), notNullValue());
            assertThat(it.getId(), greaterThanOrEqualTo(1L));

            assertThat(it.getTitle(), notNullValue());
            assertThat(it.getDescription(), notNullValue());
            assertThat(it.getImgPath(), notNullValue());
            assertThat("imgPath должен начинаться с /images/", it.getImgPath(), startsWith("/images/"));

            assertThat(it.getPrice(), notNullValue());
            assertThat(it.getPrice(), greaterThanOrEqualTo(0L));

            // createdAt в БД default now(), а в entity может прийти — проверим что не null
            assertThat(it.getCreatedAt(), notNullValue());
        }
    }

    @Test
    void seed_containsCoffeeItem_caseInsensitive_inMemory() {
        // репозиторий не умеет query -> проверяем seed через фильтр в памяти
        Boolean found = itemRepository.findAll()
                .map(Item::getTitle)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .any(t -> t.contains("coffee"))
                .block();

        assertThat("Seed должен содержать хотя бы один товар с 'coffee' в title", found, is(true));
    }

    @Test
    void findById_returnsItem_forExistingItem() {
        Item first = itemRepository.findAll()
                .next()
                .block();
        assertThat(first, notNullValue());
        assertThat(first.getId(), notNullValue());

        Item loaded = itemRepository.findById(first.getId()).block();
        assertThat(loaded, notNullValue());
        assertThat(loaded.getId(), is(first.getId()));
    }

    @Test
    void save_and_delete_roundtrip_works() {
        Item created = new Item("Tmp", "Tmp desc", "/images/tmp.png", 123L);

        Item saved = itemRepository.save(created).block();
        assertThat(saved, notNullValue());
        assertThat(saved.getId(), notNullValue());

        Long countAfterSave = itemRepository.count().block();
        assertThat(countAfterSave, notNullValue());
        assertThat(countAfterSave, greaterThanOrEqualTo(1L));

        itemRepository.deleteById(saved.getId()).block();

        Boolean exists = itemRepository.findById(saved.getId())
                .hasElement()
                .block();
        assertThat(exists, is(false));
    }

    @Test
    void cart_isEmpty_byDefault_inIntegrationContext() {
        Long cartCount = cartItemRepository.count().block();
        assertThat(cartCount, is(0L));
    }
}
