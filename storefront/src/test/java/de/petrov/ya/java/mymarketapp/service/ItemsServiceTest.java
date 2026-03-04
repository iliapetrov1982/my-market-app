package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.exception.EntityNotFoundException;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemsServiceTest {

    @Mock
    ItemQueryRepository itemQueryRepository;

    @InjectMocks
    ItemsService itemsService;

    @Captor
    ArgumentCaptor<Integer> limitCaptor;

    @Captor
    ArgumentCaptor<Integer> offsetCaptor;

    private static ItemDto dto(long id, String title, long price, int count) {
        return new ItemDto(id, title, "desc-" + id, "/images/" + id + ".png", price, count);
    }

    @Test
    void getItemsPage_normalizesSearchAndPageParams_andBuildsRowsOfThree_withPlaceholders() {
        // Arrange:
        // pageSize invalid -> default 5
        // pageNumber <= 0 -> 1
        // search blank -> "" in SQL-layer
        var content = List.of(
                dto(1, "A", 100, 0),
                dto(2, "B", 200, 0),
                dto(3, "C", 300, 0),
                dto(4, "D", 400, 0) // 4 items => rows: [A,B,C], [D,placeholder,placeholder]
        );

        when(itemQueryRepository.findShowcase(eq(""), eq(ItemsSort.NO), anyInt(), anyInt()))
                .thenReturn(Flux.fromIterable(content));
        when(itemQueryRepository.countShowcase(eq("")))
                .thenReturn(Mono.just(4L));

        // Act + Assert
        StepVerifier.create(itemsService.getItemsPage("   ", ItemsSort.NO, 0, 999))
                .assertNext(page -> {
                    // repo params: limit/offset
                    verify(itemQueryRepository).findShowcase(eq(""), eq(ItemsSort.NO), limitCaptor.capture(), offsetCaptor.capture());
                    assertThat(limitCaptor.getValue(), equalTo(5));
                    assertThat(offsetCaptor.getValue(), equalTo(0));

                    // items -> rows of 3 with placeholders
                    assertThat(page.items().size(), equalTo(2));

                    assertThat(page.items().get(0).get(0).id(), equalTo(1L));
                    assertThat(page.items().get(0).get(1).id(), equalTo(2L));
                    assertThat(page.items().get(0).get(2).id(), equalTo(3L));

                    assertThat(page.items().get(1).get(0).id(), equalTo(4L));
                    assertThat(page.items().get(1).get(1).id(), equalTo(-1L));
                    assertThat(page.items().get(1).get(2).id(), equalTo(-1L));

                    // paging + echo fields
                    Paging paging = page.paging();
                    assertThat(paging.pageSize(), equalTo(5));
                    assertThat(paging.pageNumber(), equalTo(1));
                    assertThat(paging.hasPrevious(), is(false));
                    assertThat(paging.hasNext(), is(false)); // total=4 with pageSize=5 -> single page

                    assertThat(page.search(), equalTo(""));          // safeSearch
                    assertThat(page.sort(), equalTo(ItemsSort.NO.name()));
                })
                .verifyComplete();

        verify(itemQueryRepository).countShowcase(eq(""));
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItemsPage_passesSort_ALPHA_toSql() {
        when(itemQueryRepository.findShowcase(eq("x"), eq(ItemsSort.ALPHA), anyInt(), anyInt()))
                .thenReturn(Flux.just(dto(1, "A", 100, 0)));
        when(itemQueryRepository.countShowcase(eq("x")))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(itemsService.getItemsPage("x", ItemsSort.ALPHA, 1, 5))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemQueryRepository).findShowcase(eq("x"), eq(ItemsSort.ALPHA), eq(5), eq(0));
        verify(itemQueryRepository).countShowcase(eq("x"));
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItemsPage_passesSort_PRICE_toSql() {
        when(itemQueryRepository.findShowcase(eq("x"), eq(ItemsSort.PRICE), anyInt(), anyInt()))
                .thenReturn(Flux.just(dto(1, "A", 100, 0)));
        when(itemQueryRepository.countShowcase(eq("x")))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(itemsService.getItemsPage("x", ItemsSort.PRICE, 1, 5))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemQueryRepository).findShowcase(eq("x"), eq(ItemsSort.PRICE), eq(5), eq(0));
        verify(itemQueryRepository).countShowcase(eq("x"));
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItemsPage_whenRequestedPageBeyondTotal_returnsLastPage() {
        // pageSize=5, total=12 => totalPages=3.
        // requested pageNumber=99 => reload lastPage=3 (0-based=2) => offset=10

        // 1) first load: any data (может быть пусто) + total=12
        when(itemQueryRepository.findShowcase(eq("q"), eq(ItemsSort.NO), anyInt(), anyInt()))
                .thenReturn(Flux.empty()) // first call
                .thenReturn(Flux.just(dto(11, "L1", 1100, 0), dto(12, "L2", 1200, 0))); // second call (last page)

        when(itemQueryRepository.countShowcase(eq("q")))
                .thenReturn(Mono.just(12L)); // will be subscribed twice

        StepVerifier.create(itemsService.getItemsPage("q", ItemsSort.NO, 99, 5))
                .assertNext(page -> {
                    assertThat(page.paging().pageNumber(), equalTo(3)); // last page number in UI (1-based)

                    // last page content we returned:
                    assertThat(page.items().get(0).get(0).id(), equalTo(11L));
                    assertThat(page.items().get(0).get(1).id(), equalTo(12L));
                })
                .verifyComplete();

        // Verify offsets of both calls:
        ArgumentCaptor<Integer> offset = ArgumentCaptor.forClass(Integer.class);
        verify(itemQueryRepository, times(2)).findShowcase(eq("q"), eq(ItemsSort.NO), eq(5), offset.capture());
        assertThat(offset.getAllValues().get(0), equalTo(490)); // (99-1)*5
        assertThat(offset.getAllValues().get(1), equalTo(10));  // (3-1)*5

        verify(itemQueryRepository, times(2)).countShowcase(eq("q"));
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItem_emitsDto_whenPresent() {
        ItemDto dto = dto(10, "OK", 1000, 0);
        when(itemQueryRepository.findItemPage(10L)).thenReturn(Mono.just(dto));

        StepVerifier.create(itemsService.getItem(10L))
                .assertNext(it -> {
                    assertThat(it.id(), equalTo(10L));
                    assertThat(it.title(), equalTo("OK"));
                })
                .verifyComplete();

        verify(itemQueryRepository).findItemPage(10L);
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItem_emitsIllegalArgumentException_whenNotFound() {
        when(itemQueryRepository.findItemPage(404L)).thenReturn(Mono.empty());

        StepVerifier.create(itemsService.getItem(404L))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex, instanceOf(IllegalArgumentException.class));
                    assertThat(ex.getMessage(), containsString("Item not found: 404"));
                })
                .verify();

        verify(itemQueryRepository).findItemPage(404L);
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItemPage_emitsDto_whenPresent() {
        ItemDto dto = dto(7, "X", 700, 2);
        when(itemQueryRepository.findItemPage(7L)).thenReturn(Mono.just(dto));

        StepVerifier.create(itemsService.getItemPage(7L))
                .assertNext(it -> {
                    assertThat(it.id(), equalTo(7L));
                    assertThat(it.count(), equalTo(2));
                })
                .verifyComplete();

        verify(itemQueryRepository).findItemPage(7L);
        verifyNoMoreInteractions(itemQueryRepository);
    }

    @Test
    void getItemPage_emitsEntityNotFoundException_whenNotFound() {
        when(itemQueryRepository.findItemPage(999L)).thenReturn(Mono.empty());

        StepVerifier.create(itemsService.getItemPage(999L))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex, instanceOf(EntityNotFoundException.class));
                    assertThat(ex.getMessage(), containsString("Item not found: 999"));
                })
                .verify();

        verify(itemQueryRepository).findItemPage(999L);
        verifyNoMoreInteractions(itemQueryRepository);
    }
}
