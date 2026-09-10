package pro.wata.sdk.model;

/**
 * Поле и направление сортировки для поисковых запросов, например
 * {@code orderId desc}. Направление по возрастанию не добавляет суффикса.
 */
public final class Sorting {

    private final String wireValue;

    private Sorting(String wireValue) {
        this.wireValue = wireValue;
    }

    public static Sorting asc(String field) {
        return new Sorting(field);
    }

    public static Sorting desc(String field) {
        return new Sorting(field + " desc");
    }

    /** Значение, отправляемое в query-параметре {@code Sorting}. */
    public String wireValue() {
        return wireValue;
    }

    @Override
    public String toString() {
        return wireValue;
    }
}
