using Microsoft.Extensions.DependencyInjection;

namespace Wata.Sdk.DependencyInjection;

/// <summary>Регистрация WataClient в контейнере DI.</summary>
public static class ServiceCollectionExtensions
{
    private const string HttpClientName = "Wata.Sdk";

    /// <summary>
    /// Регистрирует <see cref="WataClient"/> как singleton поверх именованного
    /// <see cref="HttpClient"/> из <c>IHttpClientFactory</c> — это даёт переиспользование
    /// соединений и совместимость с политиками <c>Microsoft.Extensions.Http</c>
    /// (Polly и т.п.), если вы захотите добавить их поверх.
    /// </summary>
    public static IServiceCollection AddWata(this IServiceCollection services, Action<WataOptions> configure)
    {
        ArgumentNullException.ThrowIfNull(services);
        ArgumentNullException.ThrowIfNull(configure);

        var options = new WataOptions();
        configure(options);

        services.AddHttpClient(HttpClientName);

        services.AddSingleton(options);
        services.AddSingleton(provider =>
        {
            var httpClientFactory = provider.GetRequiredService<IHttpClientFactory>();
            var httpClient = httpClientFactory.CreateClient(HttpClientName);
            return new WataClient(httpClient, options);
        });

        return services;
    }
}
