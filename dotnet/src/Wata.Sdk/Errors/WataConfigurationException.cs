namespace Wata.Sdk.Errors;

/// <summary>
/// Ошибка конфигурации SDK: не задан токен нужного продукта, недопустимое окружение
/// (например, sandbox для цифровых товаров) или некорректная дата баланса.
/// Всегда бросается ДО сетевого вызова.
/// </summary>
public sealed class WataConfigurationException : WataException
{
    public WataConfigurationException(string message)
        : base(message)
    {
    }
}
