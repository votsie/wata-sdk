// Пример: создание платёжной ссылки и приём вебхука с проверкой подписи.
//
// Запуск: dotnet run --project examples/CreateLinkAndWebhook -- <ACQUIRING_TOKEN>
//
// Без токена пример всё равно запустится и покажет, как SDK ведёт себя при
// отсутствии токена (WataConfigurationException до сетевого вызова), а также
// поднимет локальный HTTP-листенер, принимающий вебхуки на http://localhost:8080/webhook.

using System.Net;
using Wata.Sdk;
using Wata.Sdk.Acquiring.Models;
using Wata.Sdk.Errors;
using Wata.Sdk.Models;

var acquiringToken = args.Length > 0 ? args[0] : Environment.GetEnvironmentVariable("WATA_ACQUIRING_TOKEN");

using var httpClient = new HttpClient();
var client = new WataClient(httpClient, new WataOptions
{
    AcquiringToken = acquiringToken,
    Environment = WataEnvironment.Sandbox,
});

Console.WriteLine("== Создание платёжной ссылки ==");
try
{
    var link = await client.Acquiring.CreateLinkAsync(new CreatePaymentLinkRequest(
        Amount: 150m,
        Currency: Currency.Rub,
        Description: "Тестовый заказ из примера SDK",
        OrderId: Guid.NewGuid().ToString()));

    Console.WriteLine($"Ссылка создана: {link.Url}");
}
catch (WataConfigurationException ex)
{
    Console.WriteLine($"Токен не задан — это ожидаемо для демонстрации: {ex.Message}");
}
catch (WataException ex)
{
    Console.WriteLine($"Ошибка WATA API: {ex.GetType().Name}: {ex.Message} (HTTP {ex.HttpStatusCode}, код {ex.WataErrorCode})");
}

Console.WriteLine();
Console.WriteLine("== Приём вебхука ==");
Console.WriteLine("Слушаю http://localhost:8080/webhook/ (Ctrl+C для выхода)...");

using var listener = new HttpListener();
listener.Prefixes.Add("http://localhost:8080/webhook/");
listener.Start();

using var cts = new CancellationTokenSource();
Console.CancelKeyPress += (_, e) =>
{
    e.Cancel = true;
    cts.Cancel();
};

try
{
    while (!cts.IsCancellationRequested)
    {
        var contextTask = listener.GetContextAsync();
        var completed = await Task.WhenAny(contextTask, Task.Delay(Timeout.Infinite, cts.Token));
        if (completed != contextTask)
            break;

        var context = await contextTask;
        await HandleWebhookAsync(context, client, cts.Token);
    }
}
catch (OperationCanceledException)
{
    // штатное завершение по Ctrl+C
}
finally
{
    listener.Stop();
}

static async Task HandleWebhookAsync(HttpListenerContext context, WataClient client, CancellationToken cancellationToken)
{
    // КЛЮЧЕВОЙ МОМЕНТ: подпись проверяется по сырому телу запроса, byte[]/string —
    // никогда по уже распарсенному объекту. Читаем Request.InputStream ДО любого
    // разбора JSON (в ASP.NET Core это означало бы чтение Request.Body до
    // модель-байндинга — см. README).
    using var reader = new MemoryStream();
    await context.Request.InputStream.CopyToAsync(reader, cancellationToken);
    var rawBody = reader.ToArray();

    var signature = context.Request.Headers["X-Signature"];

    context.Response.StatusCode = (int)HttpStatusCode.OK;

    if (string.IsNullOrEmpty(signature))
    {
        Console.WriteLine("Вебхук без подписи — игнорирую.");
        context.Response.Close();
        return;
    }

    try
    {
        var isValid = await client.Webhooks.VerifyAsync(rawBody, signature, cancellationToken);
        if (!isValid)
        {
            Console.WriteLine("Подпись вебхука НЕ прошла проверку — тело отклонено.");
            context.Response.Close();
            return;
        }

        var evt = client.Webhooks.Parse(rawBody);
        Console.WriteLine($"Вебхук принят: {evt.Kind} {evt.TransactionType}, статус {evt.TransactionStatus}, сумма {evt.Amount} {evt.Currency}");

        // Обработчик обязан быть идемпотентным: WATA повторяет постоплатные и
        // возвратные вебхуки до 32 часов, пока не получит 200.
    }
    catch (WataWebhookException ex)
    {
        Console.WriteLine($"Ошибка проверки вебхука: {ex.Message}");
    }
    finally
    {
        context.Response.Close();
    }
}
