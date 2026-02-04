package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ItemsServiceTest {

    @Mock
    ItemRepository itemRepository;

    @InjectMocks
    ItemsService itemsService;

    @Captor
    ArgumentCaptor<Pageable> pageableCaptor;

    private static ItemDto dto(long id, String title, long price, int count) {
        return new ItemDto(id, title, "desc-" + id, "/images/" + id + ".png", price, count);
    }

    @Test
    void getItemsPage_normalizesSearchAndPageParams_andBuildsRowsOfThree_withPlaceholders() {
        // Arrange:
        // pageSize invalid -> default 5
        // pageNumber <= 0 -> 1
        // search trimmed + blank -> treated as null for repository call
        var content = List.of(
                dto(1, "A", 100, 0),
                dto(2, "B", 200, 0),
                dto(3, "C", 300, 0),
                dto(4, "D", 400, 0) // 4 items => rows: [A,B,C], [D,placeholder,placeholder]
        );

        when(itemRepository.findShowcase(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content, Page.empty().getPageable(), 4));

        // Act
        var page = itemsService.getItemsPage("   ", ItemsSort.NO, 0, 999);

        // Assert: repository called with q = null (blank -> null)
        verify(itemRepository, times(1)).findShowcase(isNull(), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber(), equalTo(0)); // safePageNumber=1 => 0-based = 0
        assertThat(used.getPageSize(), equalTo(5));   // normalized to default 5

        Sort sort = used.getSort();
        assertThat(sort.getOrderFor("id"), notNullValue());
        assertThat(Objects.requireNonNull(sort.getOrderFor("id")).getDirection(), equalTo(Sort.Direction.ASC));

        // items -> rows of 3 with placeholders
        assertThat(page.items().size(), equalTo(2));

        // first row: 3 real items
        assertThat(page.items().getFirst().getFirst().id(), equalTo(1L));
        assertThat(page.items().get(0).get(1).id(), equalTo(2L));
        assertThat(page.items().get(0).get(2).id(), equalTo(3L));

        // second row: 1 real + 2 placeholders
        assertThat(page.items().get(1).get(0).id(), equalTo(4L));
        assertThat(page.items().get(1).get(1).id(), equalTo(-1L));
        assertThat(page.items().get(1).get(2).id(), equalTo(-1L));

        // paging + echo fields
        Paging paging = page.paging();
        assertThat(paging.pageSize(), equalTo(5));
        assertThat(paging.pageNumber(), equalTo(1));
        assertThat(paging.hasPrevious(), is(false));
        assertThat(paging.hasNext(), is(false)); // total=4 with pageSize=5 -> single page

        assertThat(page.search(), equalTo(""));          // safeSearch is trimmed
        assertThat(page.sort(), equalTo(ItemsSort.NO.name()));
    }

    @Test
    void getItemsPage_usesProperSort_ALPHA() {
        when(itemRepository.findShowcase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto(1, "A", 100, 0))));

        itemsService.getItemsPage("x", ItemsSort.ALPHA, 1, 5);

        verify(itemRepository).findShowcase(eq("x"), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();

        assertThat(used.getSort().getOrderFor("title"), notNullValue());
        assertThat(Objects.requireNonNull(used.getSort().getOrderFor("title")).getDirection(), equalTo(Sort.Direction.ASC));

        assertThat(used.getSort().getOrderFor("id"), notNullValue());
        assertThat(Objects.requireNonNull(used.getSort().getOrderFor("id")).getDirection(), equalTo(Sort.Direction.ASC));
    }

    @Test
    void getItemsPage_usesProperSort_PRICE() {
        when(itemRepository.findShowcase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto(1, "A", 100, 0))));

        itemsService.getItemsPage("x", ItemsSort.PRICE, 1, 5);

        verify(itemRepository).findShowcase(eq("x"), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();

        assertThat(used.getSort().getOrderFor("price"), notNullValue());
        assertThat(Objects.requireNonNull(used.getSort().getOrderFor("price")).getDirection(), equalTo(Sort.Direction.ASC));

        assertThat(used.getSort().getOrderFor("id"), notNullValue());
        assertThat(Objects.requireNonNull(used.getSort().getOrderFor("id")).getDirection(), equalTo(Sort.Direction.ASC));
    }

    @Test
    void getItemsPage_whenRequestedPageBeyondTotal_returnsLastPage() {
        // 1) первый ответ репозитория: принудительно говорим "totalPages=3"
        @SuppressWarnings("unchecked")
        Page<ItemDto> firstCallPage = (Page<ItemDto>) mock(Page.class);
        when(firstCallPage.getTotalPages()).thenReturn(3);

        // 2) второй ответ: настоящая последняя страница (контент важен)
        var lastCallPage = new PageImpl<>(
                List.of(dto(11, "L1", 1100, 0), dto(12, "L2", 1200, 0)),
                PageRequest.of(2, 5),
                12
        );

        when(itemRepository.findShowcase(eq("q"), any(Pageable.class)))
                .thenReturn(firstCallPage)
                .thenReturn(lastCallPage);

        // Act
        var result = itemsService.getItemsPage("q", ItemsSort.NO, 99, 5);

        // Assert: было 2 вызова
        verify(itemRepository, times(2)).findShowcase(eq("q"), pageableCaptor.capture());

        var used = pageableCaptor.getAllValues();
        assertThat(used.get(0).getPageNumber(), equalTo(98)); // 99-1
        assertThat(used.get(1).getPageNumber(), equalTo(2));  // last page: 3-1

        assertThat(result.paging().pageNumber(), equalTo(3));
        assertThat(result.items().get(0).get(0).id(), equalTo(11L));
        assertThat(result.items().get(0).get(1).id(), equalTo(12L));
    }


    @Test
    void getItem_returnsDto_whenPresent() {
        ItemDto dto = dto(10, "OK", 1000, 0);
        when(itemRepository.findItemPage(10L)).thenReturn(Optional.of(dto));

        ItemDto result = itemsService.getItem(10L);

        assertThat(result, notNullValue());
        assertThat(result.id(), equalTo(10L));
        verify(itemRepository).findItemPage(10L);
    }

    @Test
    void getItem_throwsIllegalArgumentException_whenNotFound() {
        when(itemRepository.findItemPage(404L)).thenReturn(Optional.empty());

        assertThat(
                "Должно быть выброшено IllegalArgumentException при отсутствии item",
                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                        () -> itemsService.getItem(404L)
                ).getMessage(),
                containsString("Item not found: 404")
        );
    }

    @Test
    void getItemPage_returnsDto_whenRepositoryReturnsNonNull() {
        ItemDto dto = dto(7, "X", 700, 2);
        when(itemRepository.findItemWithCount(7L)).thenReturn(dto);

        ItemDto result = itemsService.getItemPage(7L);

        assertThat(result.id(), equalTo(7L));
        assertThat(result.count(), equalTo(2));
        verify(itemRepository).findItemWithCount(7L);
    }

    @Test
    void getItemPage_throwsEntityNotFoundException_whenRepositoryReturnsNull() {
        when(itemRepository.findItemWithCount(999L)).thenReturn(null);

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> itemsService.getItemPage(999L)
        );

        assertThat(ex.getMessage(), containsString("Item not found: 999"));
    }
}