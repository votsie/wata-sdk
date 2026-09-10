import { Transport } from '../http.js';
import type { Currency, TransactionStatus } from '../types.js';

/**
 * Данные устройства плательщика для фрод-мониторинга. Точный набор
 * полей не документирован спецификацией — передавайте то, что
 * возвращает клиентский скрипт чекаута.
 */
export type DeviceData = Record<string, unknown>;

export interface ThreeDsData {
  url?: string;
  method?: string;
  parameters?: Record<string, string>;
}

export interface CardCryptoPaymentRequest {
  amount: number;
  currency: Currency;
  /**
   * Криптограмма карты. Формируется клиентским скриптом чекаута в
   * браузере плательщика — сервер мерчанта её не собирает и не
   * должен пытаться это делать.
   */
  cardCrypto: string;
  ip: string;
  returnUrl: string;
  deviceData: DeviceData;
}

export interface CardCryptoPaymentResult {
  transactionId: string;
  status: TransactionStatus;
  /** Если задан — нужен редирект или автосабмит формы 3DS. */
  threeDsData?: ThreeDsData;
}

export interface SbpPaymentRequest {
  /** Только рубли — поля валюты нет. */
  amount: number;
  ip: string;
  returnUrl: string;
  deviceData: DeviceData;
}

export interface SbpPaymentResult {
  transactionId: string;
  status: TransactionStatus;
  sbpLink: string;
}

export interface TPayPaymentRequest {
  amount: number;
  ip: string;
  returnUrl: string;
  deviceData: DeviceData;
}

export interface TPayPaymentResult {
  transactionId: string;
  status: TransactionStatus;
  tPayLink: string;
}

export class PaymentsApi {
  constructor(private readonly transport: Transport) {}

  /** Оплата картой по криптограмме. Не повторяется автоматически при сбое. */
  cardCrypto(request: CardCryptoPaymentRequest): Promise<CardCryptoPaymentResult> {
    return this.transport.request<CardCryptoPaymentResult>({
      method: 'POST',
      path: '/payments/card-crypto',
      body: request,
      idempotent: false,
    });
  }

  /** Оплата через СБП. Не повторяется автоматически при сбое. */
  sbp(request: SbpPaymentRequest): Promise<SbpPaymentResult> {
    return this.transport.request<SbpPaymentResult>({
      method: 'POST',
      path: '/payments/sbp',
      body: request,
      idempotent: false,
    });
  }

  /** Оплата через T-Pay. Не повторяется автоматически при сбое. */
  tPay(request: TPayPaymentRequest): Promise<TPayPaymentResult> {
    return this.transport.request<TPayPaymentResult>({
      method: 'POST',
      path: '/payments/tpay',
      body: request,
      idempotent: false,
    });
  }
}
