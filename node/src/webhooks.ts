/**
 * Проверка подписи вебхуков H2H и разбор события.
 *
 * Подпись передаётся в заголовке `X-Signature` (base64), алгоритм —
 * SHA512withRSA (RSA PKCS#1 v1.5 + SHA-512), НЕ SHA-256. Проверяется
 * СЫРОЕ тело запроса, до разбора JSON: пересборка JSON меняет порядок
 * ключей и ломает подпись. Поэтому `verify()` принимает тело только
 * как строку/байты, но не как разобранный объект — это осознанное
 * ограничение типов, а не недосмотр.
 */

import { createVerify } from 'node:crypto';
import { WataWebhookError } from './errors.js';
import { Transport } from './http.js';
import type { Currency, TransactionKind, TransactionStatus, TransactionType } from './types.js';

/** Тело вебхука, попадающее в SDK, может быть только сырыми байтами или строкой — не объектом. */
export type RawWebhookBody = string | Uint8Array;

export interface WataWebhookEvent {
  transactionType?: TransactionType;
  kind?: TransactionKind;
  id?: string;
  transactionId?: string;
  originalTransactionId?: string;
  terminalPublicId?: string;
  transactionStatus?: TransactionStatus;
  errorCode?: string;
  errorDescription?: string;
  terminalName?: string;
  amount?: number;
  currency?: Currency;
  orderId?: string;
  orderDescription?: string;
  /** Комиссия. В ответе GET /transactions/{id} то же значение называется `totalCommission`. */
  commission?: number;
  paymentTime?: string;
  email?: string;
  paymentLinkId?: string;
  payerData?: { payerId?: string };
}

interface PublicKeyResponse {
  value: string;
}

function toBuffer(body: RawWebhookBody): Buffer {
  return typeof body === 'string' ? Buffer.from(body, 'utf8') : Buffer.from(body);
}

export class WebhooksApi {
  #cachedKeyPem?: string;

  constructor(private readonly transport: Transport) {}

  /**
   * Возвращает PEM публичного ключа для проверки подписи, кэшируя его
   * на время жизни клиента (кэш привязан к окружению, так как у
   * боевого контура и песочницы разные ключи, а транспорт создаётся
   * отдельно на каждое окружение).
   */
  async getPublicKey(options: { forceRefresh?: boolean } = {}): Promise<string> {
    if (this.#cachedKeyPem && !options.forceRefresh) return this.#cachedKeyPem;
    try {
      const response = await this.transport.request<PublicKeyResponse>({
        method: 'GET',
        path: '/public-key',
        auth: false,
      });
      if (!response?.value) {
        throw new WataWebhookError('Ответ /public-key не содержит поле "value".');
      }
      this.#cachedKeyPem = response.value;
      return this.#cachedKeyPem;
    } catch (err) {
      if (err instanceof WataWebhookError) throw err;
      throw new WataWebhookError('Не удалось получить публичный ключ для проверки подписи вебхука.', {
        cause: err,
      });
    }
  }

  /**
   * Проверяет подпись `X-Signature` над сырым телом запроса.
   *
   * @param rawBody сырое тело запроса как строка или байты — НИКОГДА не передавайте сюда результат `JSON.parse`
   * @param signatureBase64 значение заголовка `X-Signature`
   * @param keyPem необязательный PEM-ключ; если не задан, ключ будет получен и закэширован автоматически
   */
  async verify(rawBody: RawWebhookBody, signatureBase64: string, keyPem?: string): Promise<boolean> {
    const key = keyPem ?? (await this.getPublicKey());
    return verifySignature(rawBody, signatureBase64, key);
  }

  /**
   * Разбирает тело вебхука в типизированное событие. Не проверяет
   * подпись — вызывайте `verify()` отдельно (как правило, до `parse()`).
   */
  parse(rawBody: RawWebhookBody): WataWebhookEvent {
    const text = typeof rawBody === 'string' ? rawBody : Buffer.from(rawBody).toString('utf8');
    return JSON.parse(text) as WataWebhookEvent;
  }
}

function verifySignature(rawBody: RawWebhookBody, signatureBase64: string, keyPem: string): boolean {
  try {
    const verifier = createVerify('RSA-SHA512');
    verifier.update(toBuffer(rawBody));
    verifier.end();
    return verifier.verify(keyPem, Buffer.from(signatureBase64, 'base64'));
  } catch {
    return false;
  }
}
