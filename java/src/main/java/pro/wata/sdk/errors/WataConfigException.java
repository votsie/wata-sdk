package pro.wata.sdk.errors;

/**
 * Ошибка конфигурации SDK, обнаруженная <b>до</b> сетевого вызова: не задан
 * токен нужного продукта, запрошено недопустимое окружение (например sandbox
 * для цифровых товаров) или передана недопустимая дата баланса.
 */
public class WataConfigException extends WataException {

    public WataConfigException(String message) {
        super(message);
    }
}
