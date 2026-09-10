/**
 * WataClient: собирает набор токенов по продуктам в отдельные,
 * физически изолированные друг от друга HTTP-клиенты.
 *
 * Токен WATA выпускается на терминал, а не на аккаунт, и Stars/Steam
 * всегда живут на отдельных терминалах. Поэтому:
 *
 * - Токены передаются по одному на продукт: `acquiring`, `stars`,
 *   `steam`, `topup`, `vouchers`. Все необязательны.
 * - Каждый продукт использует свой собственный `Transport` со своим
 *   токеном — токены между продуктами никогда не переиспользуются.
 * - Обращение к продукту без токена бросает `WataConfigError` ДО
 *   сетевого вызова, называя недостающий токен.
 * - Это же проверяется и на уровне типов: если конкретный вызов
 *   `new WataClient({...})` не включает, скажем, `stars`, то
 *   TypeScript типизирует `client.stars` как `never` — обращение к
 *   любому методу на нём является ошибкой компиляции, а не только
 *   рантайма.
 */

import { AcquiringApi } from './acquiring/index.js';
import { StarsApi } from './digital-goods/stars.js';
import { SteamApi } from './digital-goods/steam.js';
import { TopupApi } from './digital-goods/topup.js';
import { VouchersApi } from './digital-goods/vouchers.js';
import { WataConfigError } from './errors.js';
import { Transport, type WataFetch } from './http.js';
import type { Environment } from './types.js';
import { WebhooksApi } from './webhooks.js';

const ACQUIRING_BASE_URLS: Record<Environment, string> = {
  production: 'https://api.wata.pro',
  sandbox: 'https://api-sandbox.wata.pro',
};

/** Песочница для цифровых товаров не документирована и не существует — только `production`. */
const DIGITAL_GOODS_BASE_URL = 'https://dg-api.wata.pro';

const DEFAULT_TIMEOUT_MS = 60_000;
const DEFAULT_MAX_RETRIES = 3;

export interface WataClientConfig {
  /** Токен терминала эквайринга (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи. */
  acquiring?: string;
  /** Токен терминала Telegram Stars. Всегда отдельный терминал от Steam. */
  stars?: string;
  /** Токен терминала Steam. Всегда отдельный терминал от Stars. */
  steam?: string;
  /** Токен терминала Top-Up. */
  topup?: string;
  /** Токен терминала ваучеров. */
  vouchers?: string;
  /** По умолчанию `production`. Для цифровых товаров `sandbox` недоступен. */
  environment?: Environment;
  /** Таймаут ответа API в миллисекундах. По умолчанию 60000 (1 минута — лимит самого API). */
  timeoutMs?: number;
  /** Число попыток для идемпотентных (GET) запросов при сетевой ошибке/5xx. По умолчанию 3. */
  maxRetries?: number;
  /** Внедряемая реализация `fetch` — используется в тестах на моках HTTP. По умолчанию — глобальный `fetch`. */
  fetch?: WataFetch;
}

function missingTokenError(field: keyof WataClientConfig, label: string): WataConfigError {
  return new WataConfigError(
    `Токен продукта "${String(field)}" (${label}) не задан. Передайте его при создании клиента: ` +
      `new WataClient({ ${String(field)}: "<jwt терминала>" }).`,
  );
}

/**
 * Клиент WATA API. Дженерик `T` захватывает literal-тип переданной
 * конфигурации, чтобы геттеры продуктов без токена типизировались как
 * `never` — обращение к их методам не скомпилируется.
 */
export class WataClient<T extends WataClientConfig = WataClientConfig> {
  readonly environment: Environment;
  readonly webhooks: WebhooksApi;

  #acquiring?: AcquiringApi;
  #stars?: StarsApi;
  #steam?: SteamApi;
  #topup?: TopupApi;
  #vouchers?: VouchersApi;

  constructor(config: T) {
    const environment: Environment = config.environment ?? 'production';
    if (environment !== 'production' && environment !== 'sandbox') {
      throw new WataConfigError(`Неизвестное окружение: "${String(environment)}". Допустимо: "production" | "sandbox".`);
    }

    const timeoutMs = config.timeoutMs ?? DEFAULT_TIMEOUT_MS;
    const maxRetries = config.maxRetries ?? DEFAULT_MAX_RETRIES;
    const fetchImpl = config.fetch ?? globalThis.fetch;
    if (!fetchImpl) {
      throw new WataConfigError(
        'Глобальная функция fetch недоступна. Передайте её явно через опцию `fetch` или используйте Node.js 18+.',
      );
    }

    const hasDigitalGoodsToken = Boolean(config.steam || config.stars || config.topup || config.vouchers);
    if (environment === 'sandbox' && hasDigitalGoodsToken) {
      throw new WataConfigError(
        'Песочница недоступна для цифровых товаров (Steam, Stars, Top-Up, Vouchers) — отдельного адреса песочницы для них нет. ' +
          'Используйте environment: "production" для этих токенов, либо не передавайте их вместе с environment: "sandbox".',
      );
    }

    this.environment = environment;

    const acquiringBaseUrl = ACQUIRING_BASE_URLS[environment];
    const makeTransport = (baseUrl: string, token?: string) =>
      new Transport({ baseUrl, token, timeoutMs, maxRetries, fetchImpl });

    if (config.acquiring) this.#acquiring = new AcquiringApi(makeTransport(acquiringBaseUrl, config.acquiring));
    if (config.steam) this.#steam = new SteamApi(makeTransport(DIGITAL_GOODS_BASE_URL, config.steam));
    if (config.stars) this.#stars = new StarsApi(makeTransport(DIGITAL_GOODS_BASE_URL, config.stars));
    if (config.topup) this.#topup = new TopupApi(makeTransport(DIGITAL_GOODS_BASE_URL, config.topup));
    if (config.vouchers) this.#vouchers = new VouchersApi(makeTransport(DIGITAL_GOODS_BASE_URL, config.vouchers));

    // Публичный ключ вебхуков (GET /api/h2h/public-key) не требует авторизации,
    // поэтому вебхуки доступны независимо от наличия токена эквайринга.
    this.webhooks = new WebhooksApi(makeTransport(acquiringBaseUrl, undefined));
  }

  /** Продукт "Эквайринг" (H2H). */
  get acquiring(): T['acquiring'] extends string ? AcquiringApi : never {
    if (!this.#acquiring) throw missingTokenError('acquiring', 'эквайринга');
    return this.#acquiring as never;
  }

  /** Продукт "Telegram Stars". Терминал всегда отдельный от Steam. */
  get stars(): T['stars'] extends string ? StarsApi : never {
    if (!this.#stars) throw missingTokenError('stars', 'Telegram Stars');
    return this.#stars as never;
  }

  /** Продукт "Steam". Терминал всегда отдельный от Stars. */
  get steam(): T['steam'] extends string ? SteamApi : never {
    if (!this.#steam) throw missingTokenError('steam', 'Steam');
    return this.#steam as never;
  }

  /** Продукт "Top-Up". */
  get topup(): T['topup'] extends string ? TopupApi : never {
    if (!this.#topup) throw missingTokenError('topup', 'Top-Up');
    return this.#topup as never;
  }

  /** Продукт "Ваучеры". */
  get vouchers(): T['vouchers'] extends string ? VouchersApi : never {
    if (!this.#vouchers) throw missingTokenError('vouchers', 'ваучеров');
    return this.#vouchers as never;
  }
}
