/**
 * HTTP-транспорт: заголовки, ретраи, таймаут, разбор ошибок.
 *
 * Зависимостей нет: используется глобальный `fetch` (в Node 20+ это
 * реализация на базе undici, включённая в рантайм). Для тестов на
 * моках функция fetch внедряется через опцию `fetch` конструктора
 * `WataClient` — это даёт более чистые и быстрые тесты, чем поднятие
 * `undici.MockAgent`, и не требует дополнительной зависимости.
 */

import {
  WataApiError,
  WataAuthError,
  WataNetworkError,
  WataRateLimitError,
  WataServerError,
  WataError,
} from './errors.js';

export type WataFetch = typeof fetch;

export interface TransportOptions {
  baseUrl: string;
  token?: string;
  timeoutMs: number;
  maxRetries: number;
  fetchImpl: WataFetch;
}

export type HttpMethod = 'GET' | 'POST';

export interface RequestOptions {
  method: HttpMethod;
  path: string;
  query?: Record<string, unknown>;
  body?: unknown;
  /** `false` — не добавлять заголовок Authorization (например, публичный ключ вебхуков). */
  auth?: boolean;
  /**
   * Можно ли повторять запрос при сетевой ошибке/5xx. По умолчанию —
   * только для GET. Изменяющие запросы (создание ссылки/платежа/
   * возврата) по умолчанию НЕ повторяются: повтор может создать
   * второй платёж.
   */
  idempotent?: boolean;
}

interface WataErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: unknown;
    validationErrors?: unknown;
  };
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Убирает `undefined`/`null` из query-параметров и сериализует массивы как повторяющиеся ключи. */
export function buildQueryString(query?: Record<string, unknown>): string {
  if (!query) return '';
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null) continue;
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item === undefined || item === null) continue;
        params.append(key, String(item));
      }
    } else if (value instanceof Date) {
      params.append(key, value.toISOString());
    } else {
      params.append(key, String(value));
    }
  }
  const serialized = params.toString();
  return serialized ? `?${serialized}` : '';
}

/** Рекурсивно убирает `undefined`/`null` поля из тела запроса перед сериализацией в JSON. */
function pruneNullish(value: unknown): unknown {
  if (value === null || value === undefined) return undefined;
  if (Array.isArray(value)) return value.map(pruneNullish);
  if (typeof value === 'object') {
    if (value instanceof Date) return value.toISOString();
    const out: Record<string, unknown> = {};
    for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
      const pruned = pruneNullish(item);
      if (pruned !== undefined) out[key] = pruned;
    }
    return out;
  }
  return value;
}

/**
 * Строит query-объект с ключами в PascalCase, ожидаемом сервером WATA,
 * из объекта с камелкейс-полями SDK. Отсутствующие (undefined/null)
 * значения не попадают в результат.
 */
export function mapQuery<P extends object>(params: P, keyMap: Record<string, string>): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(params as Record<string, unknown>)) {
    if (value === undefined || value === null) continue;
    out[keyMap[key] ?? key] = value;
  }
  return out;
}

export class Transport {
  constructor(private readonly options: TransportOptions) {}

  /**
   * Как `request()`, но добавляет к разобранному ответу поле `raw` с
   * исходным телом целиком. Используется моделями цифровых товаров:
   * документация WATA покрывает не все возвращаемые поля, и без
   * сохранения исходного JSON интегратор не смог бы добраться до
   * недокументированного значения, не дожидаясь новой версии SDK.
   *
   * Ожидает, что сервер отвечает JSON-объектом (не массивом/примитивом);
   * для эндпоинтов, возвращающих массив, оборачивайте результат `request()` вручную.
   */
  async requestWithRaw<Res extends { raw: unknown }>(request: RequestOptions): Promise<Res> {
    const parsed = (await this.request<Record<string, unknown>>(request)) ?? {};
    return { ...parsed, raw: parsed } as Res;
  }

  async request<Res>(request: RequestOptions): Promise<Res> {
    const useAuth = request.auth !== false;
    const url = this.options.baseUrl + request.path + buildQueryString(request.query);
    const allowRetry = request.idempotent ?? request.method === 'GET';
    const maxAttempts = allowRetry ? Math.max(1, this.options.maxRetries) : 1;

    let attempt = 0;
    for (;;) {
      attempt++;
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), this.options.timeoutMs);
      try {
        const headers: Record<string, string> = {
          Accept: 'application/json',
        };
        if (request.body !== undefined) headers['Content-Type'] = 'application/json';
        if (useAuth) headers.Authorization = `Bearer ${this.options.token}`;

        const init: RequestInit = {
          method: request.method,
          headers,
          signal: controller.signal,
        };
        if (request.body !== undefined) {
          init.body = JSON.stringify(pruneNullish(request.body));
        }

        const response = await this.options.fetchImpl(url, init);

        if (response.status === 429) {
          throw await this.toRateLimitError(response, request.path);
        }
        if (response.status === 401 || response.status === 403) {
          throw await this.toAuthError(response, request.path);
        }
        if (response.status >= 500) {
          const serverError = await this.toServerError(response, request.path);
          if (attempt < maxAttempts) {
            await this.backoff(attempt);
            continue;
          }
          throw serverError;
        }
        if (response.status >= 400) {
          throw await this.toApiError(response, request.path);
        }

        return await this.parseBody<Res>(response);
      } catch (err) {
        if (err instanceof WataError) throw err;
        const networkError = new WataNetworkError(this.describeNetworkError(err), {
          path: request.path,
          cause: err,
        });
        if (allowRetry && attempt < maxAttempts) {
          await this.backoff(attempt);
          continue;
        }
        throw networkError;
      } finally {
        clearTimeout(timer);
      }
    }
  }

  private describeNetworkError(err: unknown): string {
    if (err instanceof Error && err.name === 'AbortError') {
      return `Превышен таймаут ожидания ответа (${this.options.timeoutMs} мс).`;
    }
    return `Сетевая ошибка при обращении к WATA API: ${err instanceof Error ? err.message : String(err)}`;
  }

  private async backoff(attempt: number): Promise<void> {
    const base = 200 * 2 ** (attempt - 1);
    const jitter = Math.random() * base * 0.5;
    await sleep(base + jitter);
  }

  private async parseBody<Res>(response: Response): Promise<Res> {
    if (response.status === 204) return undefined as Res;
    const text = await response.text();
    if (!text) return undefined as Res;
    return JSON.parse(text) as Res;
  }

  private async readErrorBody(response: Response): Promise<WataErrorBody | undefined> {
    try {
      const text = await response.text();
      return text ? (JSON.parse(text) as WataErrorBody) : undefined;
    } catch {
      return undefined;
    }
  }

  private async toApiError(response: Response, path: string): Promise<WataApiError> {
    const body = await this.readErrorBody(response);
    const message = body?.error?.message ?? `WATA API вернул ошибку ${response.status} для ${path}.`;
    return new WataApiError(message, {
      httpStatus: response.status,
      path,
      wataCode: body?.error?.code,
      details: body?.error?.details,
      validationErrors: body?.error?.validationErrors,
    });
  }

  private async toAuthError(response: Response, path: string): Promise<WataAuthError> {
    const body = await this.readErrorBody(response);
    const message =
      body?.error?.message ??
      'Ошибка авторизации: токен недействителен, отозван, принадлежит другому терминалу либо запрос выполнен с несогласованного IP.';
    return new WataAuthError(message, { httpStatus: response.status, path, wataCode: body?.error?.code });
  }

  private async toServerError(response: Response, path: string): Promise<WataServerError> {
    return new WataServerError(`WATA API вернул серверную ошибку ${response.status} для ${path}.`, {
      httpStatus: response.status,
      path,
    });
  }

  private async toRateLimitError(response: Response, path: string): Promise<WataRateLimitError> {
    const retryAfterHeader = response.headers.get('Retry-After');
    let retryAfterMs: number | undefined;
    let retryAt: Date | undefined;
    if (retryAfterHeader) {
      const seconds = Number(retryAfterHeader);
      if (Number.isFinite(seconds)) {
        retryAfterMs = seconds * 1000;
        retryAt = new Date(Date.now() + retryAfterMs);
      } else {
        const date = new Date(retryAfterHeader);
        if (!Number.isNaN(date.getTime())) retryAt = date;
      }
    }
    const suffix = retryAt ? ` Повторите после ${retryAt.toISOString()}.` : '';
    return new WataRateLimitError(`Превышен лимит запросов (429) для ${path}.${suffix}`, {
      httpStatus: 429,
      path,
      retryAfterMs,
      retryAt,
    });
  }
}
