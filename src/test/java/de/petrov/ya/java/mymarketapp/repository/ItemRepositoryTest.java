package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.MyMarketAppApplicationTests;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

class ItemRepositoryTest extends MyMarketAppApplicationTests {

    @Autowired
    ItemRepository itemRepository;

    @Test
    void findShowcase_returnsItemsFromSeed_withZeroCount() {
        var page = itemRepository.findShowcase(
                null,
                PageRequest.of(0, 10, Sort.by("id").ascending())
        );

        assertThat(page, notNullValue());
        assertThat("В seed должно быть не меньше 10 товаров", page.getTotalElements(), greaterThanOrEqualTo(10L));

        for (ItemDto dto : page.getContent()) {
            assertThat(dto.id(), greaterThan(0L));

            assertThat(dto.title(), notNullValue());
            assertThat(dto.title(), not(equalTo("")));

            assertThat(dto.description(), notNullValue());
            assertThat(dto.description(), not(equalTo("")));

            assertThat(dto.imgPath(), notNullValue());
            assertThat(dto.imgPath(), startsWith("/images/"));

            assertThat(dto.price(), greaterThanOrEqualTo(0L));

            assertThat(
                    "Seed не содержит cart_items → count должен быть 0",
                    dto.count(),
                    equalTo(0)
            );
        }
    }

    @Test
    void findShowcase_filtersByQuery_caseInsensitive() {
        var result = itemRepository.findShowcase(
                "coffee",
                PageRequest.of(0, 20)
        );

        List<ItemDto> content = result.getContent();
        assertThat(content, is(not(empty())));

        boolean found = content.stream()
                .map(ItemDto::title)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(t -> t.contains("coffee"));

        assertThat(
                "Должен быть хотя бы один товар с 'coffee' в title (seed содержит Coffee items)",
                found,
                is(true)
        );
    }

    @Test
    void findItemPage_returnsOptionalPresent_forExistingItem() {
        var firstItem = itemRepository.findAll(PageRequest.of(0, 1))
                .getContent()
                .getFirst();

        var dtoOpt = itemRepository.findItemPage(firstItem.getId());

        assertThat("findItemPage должен вернуть Optional.present для существующего id", dtoOpt.isPresent(), is(true));

        var dto = dtoOpt.get();
        assertThat(dto.id(), equalTo(firstItem.getId()));
        assertThat("Товар не в корзине → count = 0", dto.count(), equalTo(0));
    }

    @Test
    void findItemWithCount_returnsDto_forExistingItem() {
        var firstItem = itemRepository.findAll(PageRequest.of(0, 1))
                .getContent()
                .getFirst();

        ItemDto dto = itemRepository.findItemPage(firstItem.getId())
                .orElseThrow(() -> new AssertionError("Item not found: " + firstItem.getId()));

//        assertThat(dto, notNullValue());
        assertThat(dto.id(), equalTo(firstItem.getId()));
        assertThat("Товар не в корзине → count = 0", dto.count(), equalTo(0));
    }

    @Test
    void findCartItems_returnsEmptyList_whenCartIsEmpty() {
        List<ItemDto> cartItems = itemRepository.findCartItems();

        assertThat(
                "Seed не содержит cart_items → список должен быть пустым",
                cartItems,
                is(empty())
        );
    }
}