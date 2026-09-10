/**
 * Иерархия ошибок WATA SDK (спецификация, раздел 7).
 *
 * Каждая ошибка несёт HTTP-статус (если применимо), путь запроса и,
 * если он есть, код ошибки WATA (`PL_*`, `TRA_*`, `ORD_*`, `STM_*`,
 * `STR_*`, `TPP_*`, `VCR_*`). Секреты (токен, тело с картой) в текст
 * ошибки никогда не попадают.
 */

export interface WataErrorOptions {
  httpStatus?: number;
  path?: string;
  wataCode?: string;
  cause?: unknown;
}

/** Базовая ошибка SDK. */
export class WataError extends Error {
  readonly httpStatus?: number;
  readonly path?: string;
  readonly wataCode?: string;

  constructor(message: string, options: WataErrorOptions = {}) {
    super(message, options.cause !== undefined ? { cause: options.cause } : undefined);
    this.name = new.target.name;
    this.httpStatus = options.httpStatus;
    this.path = options.path;
    this.wataCode = options.wataCode;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Ошибка конфигурации: не задан токен нужного продукта, неверное
 * окружение, недопустимая дата баланса и т. п. Всегда возникает
 * ДО сетевого вызова.
 */
export class WataConfigError extends WataError {}

/** 401/403: токен истёк, отозван, принадлежит другому терминалу либо запрос идёт с несогласованного IP. */
export class WataAuthError extends WataError {}

/** 429: лимит запросов исчерпан. */
export class WataRateLimitError extends WataError {
  /** Сколько миллисекунд подождать перед повтором, если сервер это сообщил. */
  readonly retryAfterMs?: number;
  /** Момент времени, когда можно повторить запрос, если сервер это сообщил. */
  readonly retryAt?: Date;

  constructor(message: string, options: WataErrorOptions & { retryAfterMs?: number; retryAt?: Date } = {}) {
    super(message, options);
    this.retryAfterMs = options.retryAfterMs;
    this.retryAt = options.retryAt;
  }
}

/** 4xx с телом `{error: {code, message, details, validationErrors}}`. */
export class WataApiError extends WataError {
  readonly details?: unknown;
  readonly validationErrors?: unknown;

  constructor(
    message: string,
    options: WataErrorOptions & { details?: unknown; validationErrors?: unknown } = {},
  ) {
    super(message, options);
    this.details = options.details;
    this.validationErrors = options.validationErrors;
  }
}

/** 5xx после исчерпания всех повторов. */
export class WataServerError extends WataError {}

/** Таймаут или обрыв соединения. */
export class WataNetworkError extends WataError {}

/** Подпись вебхука не сошлась, либо не удалось получить публичный ключ. */
export class WataWebhookError extends WataError {}

/**
 * Коды ошибок WATA, приходящие в поле `error.code`.
 *
 * Список неполный по своей природе: платформа может добавить код без изменения
 * версии SDK. Поэтому это не enum, а справочник известных значений —
 * `WataApiError.wataCode` всегда содержит исходную строку, даже незнакомую.
 */
export const WataErrorCode = {
  // Платёжные ссылки
  LINK_NOT_FOUND: 'PL_1001',
  LINK_INVALID: 'PL_1002',
  LINK_EXPIRED: 'PL_1003',

  // Шифрование карточных данных
  CRYPTO_INVALID: 'CRY_1001',

  // Транзакции: валидация и создание — диапазон TRA_1001..TRA_1019
  // Возвраты — TRA_1101..TRA_1103
  // Отказы шлюза и эмитента — TRA_2001..TRA_2999
  REFUND_INVALID_AMOUNT: 'TRA_1101',
  REFUND_INSUFFICIENT_FUNDS: 'TRA_1102',
  REFUND_PENDING_EXISTS: 'TRA_1103',

  // Цифровые товары
  ORDER_ERROR_FIRST: 'ORD_1001',
  ORDER_ERROR_LAST: 'ORD_1007',
  STEAM_ERROR_FIRST: 'STM_1001',
  STEAM_ERROR_LAST: 'STM_1004',
  STARS_ERROR_FIRST: 'STR_1001',
  STARS_ERROR_LAST: 'STR_1004',
  TOPUP_ERROR_FIRST: 'TPP_1001',
  TOPUP_ERROR_LAST: 'TPP_1004',
  VOUCHER_ERROR_FIRST: 'VCR_1001',
  VOUCHER_ERROR_LAST: 'VCR_1003',
} as const;

/** Семейство, к которому относится код ошибки WATA. */
export type WataErrorFamily =
  | 'payment-link'
  | 'crypto'
  | 'transaction'
  | 'refund'
  | 'order'
  | 'steam'
  | 'stars'
  | 'topup'
  | 'voucher'
  | 'unknown';

/**
 * Определяет семейство по префиксу кода.
 * Полезно, чтобы обработать целую группу отказов, не перечисляя каждый код.
 */
export function wataErrorFamily(code: string | undefined): WataErrorFamily {
  if (!code) return 'unknown';
  if (code.startsWith('PL_')) return 'payment-link';
  if (code.startsWith('CRY_')) return 'crypto';
  if (code.startsWith('TRA_11')) return 'refund';
  if (code.startsWith('TRA_')) return 'transaction';
  if (code.startsWith('ORD_')) return 'order';
  if (code.startsWith('STM_')) return 'steam';
  if (code.startsWith('STR_')) return 'stars';
  if (code.startsWith('TPP_')) return 'topup';
  if (code.startsWith('VCR_')) return 'voucher';
  return 'unknown';
}
