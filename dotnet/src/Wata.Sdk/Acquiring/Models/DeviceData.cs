namespace Wata.Sdk.Acquiring.Models;

/// <summary>
/// Данные устройства плательщика, требуемые для прямых платежей (антифрод-профиль
/// 3-D Secure). Состав полей — по OpenAPI-контракту WATA; заполняйте по данным,
/// собранным клиентским скриптом чекаута в браузере плательщика.
/// </summary>
public sealed record DeviceData(
    string? UserAgent = null,
    string? BrowserAcceptHeader = null,
    string? BrowserLanguage = null,
    string? BrowserColorDepth = null,
    string? BrowserScreenHeight = null,
    string? BrowserScreenWidth = null,
    string? BrowserTz = null,
    bool? BrowserJavaEnabled = null);
