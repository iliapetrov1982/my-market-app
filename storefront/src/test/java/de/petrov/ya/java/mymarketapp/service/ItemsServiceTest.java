package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemsServiceTest {

    @Mock
    ItemQueryRepository itemQueryRepository;

    @Mock
    ItemCacheService itemCacheService;

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
        var content = List.of(
                dto(1, "A", 100, 0),
                dto(2, "B", 200, 0),
                dto(3, "C", 300, 0),
                dto(4, "D", 400, 0)
        );

        when(itemQueryRepository.findShowcase(eq(""), eq(ItemsSort.NO), anyInt(), anyInt()))
                .thenReturn(Flux.fromIterable(content));
        when(itemQueryRepository.countShowcase(eq("")))
                .thenReturn(Mono.just(4L));

        StepVerifier.create(itemsService.getItemsPage("   ", ItemsSort.NO, 0, 999))
                .assertNext(page -> {
                    verify(itemQueryRepository)
                            .findShowcase(eq(""), eq(ItemsSort.NO), limitCaptor.capture(), offsetCaptor.capture());

                    assertThat(limitCaptor.getValue(), equalTo(5));
                    assertThat(offsetCaptor.getValue(), equalTo(0));

                    assertThat(page.items().size(), equalTo(2));

                    assertThat(page.items().get(0).get(0).id(), equalTo(1L));
                    assertThat(page.items().get(0).get(1).id(), equalTo(2L));
                    assertThat(page.items().get(0).get(2).id(), equalTo(3L));

                    assertThat(page.items().get(1).get(0).id(), equalTo(4L));
                    assertThat(page.items().get(1).get(1).id(), equalTo(-1L));
                    assertThat(page.items().get(1).get(2).id(), equalTo(-1L));

                    Paging paging = page.paging();
                    assertThat(paging.pageSize(), equalTo(5));
                    assertThat(paging.pageNumber(), equalTo(1));
                    assertThat(paging.hasPrevious(), is(false));
                    assertThat(paging.hasNext(), is(false));

                    assertThat(page.search(), equalTo(""));
                    assertThat(page.sort(), equalTo(ItemsSort.NO.name()));
                })
                .verifyComplete();

        verify(itemQueryRepository).countShowcase(eq(""));
        verifyNoMoreInteractions(itemQueryRepository);
        verifyNoInteractions(itemCacheService);
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
        verifyNoInteractions(itemCacheService);
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
        verifyNoInteractions(itemCacheService);
    }

    @Test
    void getItemsPage_whenRequestedPageBeyondTotal_returnsLastPage() {
        when(itemQueryRepository.findShowcase(eq("q"), eq(ItemsSort.NO), anyInt(), anyInt()))
                .thenReturn(Flux.empty())
                .thenReturn(Flux.just(
                        dto(11, "L1", 1100, 0),
                        dto(12, "L2", 1200, 0)
                ));

        when(itemQueryRepository.countShowcase(eq("q")))
                .thenReturn(Mono.just(12L));

        StepVerifier.create(itemsService.getItemsPage("q", ItemsSort.NO, 99, 5))
                .assertNext(page -> {
                    assertThat(page.paging().pageNumber(), equalTo(3));
                    assertThat(page.items().get(0).get(0).id(), equalTo(11L));
                    assertThat(page.items().get(0).get(1).id(), equalTo(12L));
                })
                .verifyComplete();

        ArgumentCaptor<Integer> offset = ArgumentCaptor.forClass(Integer.class);
        verify(itemQueryRepository, times(2))
                .findShowcase(eq("q"), eq(ItemsSort.NO), eq(5), offset.capture());

        assertThat(offset.getAllValues().get(0), equalTo(490));
        assertThat(offset.getAllValues().get(1), equalTo(10));

        verify(itemQueryRepository, times(2)).countShowcase(eq("q"));
        verifyNoMoreInteractions(itemQueryRepository);
        verifyNoInteractions(itemCacheService);
    }
}