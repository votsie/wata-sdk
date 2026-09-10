package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Постраничный результат поиска (offset-пагинация), например поиск ссылок. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResult<T>(List<T> items, long totalCount) {
}
