import { Transport, mapQuery } from '../http.js';
import type { DepositOrderStatusResult, DigitalGoodsOrderStatus } from '../types.js';

/**
 * Steam различает несколько денежных величин, которые легко перепутать:
 * - `netAmount` — сумма, которая должна зачислиться на аккаунт Steam;
 * - `amount` — сумма, которую платит покупатель (эквайринг);
 * - `price` — цена в валюте поставщика Steam (в депозитных методах — в долларах).
 *
 * Комиссия и курс делают их разными, поэтому SDK называет их явно, а не
 * общим словом "сумма".
 */

export interface SteamAmountQuoteQuery {
  /** Аккаунт (логин) Steam. */
  account: string;
  /** Желаемая сумма зачисления на аккаунт. */
  netAmount: number;
}

export interface SteamAmountQuote {
  price: number;
  minPrice: number;
  steamRate: number;
  /** Исходное тело ответа сервера — на случай недокументированных полей. */
  raw: unknown;
}

export interface CreateSteamOrderRequest {
  account: string;
  amount: number;
  netAmount: number;
  description: string;
  orderId: string;
  successRedirectUrl?: string;
  failRedirectUrl?: string;
}

export interface SteamOrderCreated {
  orderId: string;
  amount: number;
  price: number;
  minPrice: number;
  commission: number;
  steamRate: number;
  paymentLink: string;
  raw: unknown;
}

export interface SteamByAmountQuoteQuery {
  /** Сумма, которую готов заплатить покупатель. */
  amount: number;
  margin: number;
  account: string;
}

export interface SteamByAmountQuote {
  netAmount: number;
  price: number;
  steamRate: number;
  raw: unknown;
}

export interface CreateSteamOrderByAmountRequest {
  account: string;
  amount: number;
  margin: number;
  description: string;
  orderId: string;
}

export interface SteamOrderByAmountCreated {
  orderId: string;
  amount: number;
  netAmount: number;
  price: number;
  commission: number;
  margin: number;
  steamRate: number;
  paymentLink: string;
  raw: unknown;
}

export interface SteamOrderStatus {
  orderId: string;
  amount: number;
  status: DigitalGoodsOrderStatus;
  successRedirectUrl?: string;
  failRedirectUrl?: string;
  raw: unknown;
}

export interface SteamDepositPriceQuery {
  account: string;
  netAmount: number;
}

export interface SteamDepositPriceQuote {
  /** Цена списания с депозита, в долларах. */
  price: number;
  netAmount: number;
  steamRate: number;
  raw: unknown;
}

export interface CreateSteamDepositRequest {
  account: string;
  netAmount: number;
  description: string;
  orderId: string;
}

export interface SteamDepositOrderCreated {
  orderId: string;
  account: string;
  price: number;
  netAmount: number;
  steamRate: number;
  raw: unknown;
}

export interface SteamDepositNetAmountQuery {
  account: string;
  /** Сумма списания с депозита, в долларах. */
  price: number;
}

export interface SteamDepositNetAmountQuote {
  price: number;
  netAmount: number;
  steamRate: number;
  raw: unknown;
}

export interface CreateSteamDepositByPriceRequest {
  account: string;
  /** Сумма списания с депозита, в долларах. */
  price: number;
  description: string;
  orderId: string;
}

class SteamDepositApi {
  constructor(private readonly transport: Transport) {}

  /** Сколько зачислится на аккаунт за заданную сумму списания с депозита (в долларах). */
  price(query: SteamDepositPriceQuery): Promise<SteamDepositPriceQuote> {
    return this.transport.requestWithRaw<SteamDepositPriceQuote>({
      method: 'GET',
      path: '/v1/steam/deposit/price',
      query: mapQuery(query, {}),
    });
  }

  /** Сколько будет списано с депозита за желаемую сумму зачисления. */
  netAmount(query: SteamDepositNetAmountQuery): Promise<SteamDepositNetAmountQuote> {
    return this.transport.requestWithRaw<SteamDepositNetAmountQuote>({
      method: 'GET',
      path: '/v1/steam/deposit/netamount',
      query: mapQuery(query, {}),
    });
  }

  /** Создать заказ пополнения Steam с депозита мерчанта, указывая желаемое зачисление. Не повторяется при сбое. */
  create(request: CreateSteamDepositRequest): Promise<SteamDepositOrderCreated> {
    return this.transport.requestWithRaw<SteamDepositOrderCreated>({
      method: 'POST',
      path: '/v1/steam/deposit',
      body: request,
      idempotent: false,
    });
  }

  /** Создать заказ пополнения Steam с депозита мерчанта, указывая сумму списания. Не повторяется при сбое. */
  createByPrice(request: CreateSteamDepositByPriceRequest): Promise<SteamDepositOrderCreated> {
    return this.transport.requestWithRaw<SteamDepositOrderCreated>({
      method: 'POST',
      path: '/v1/steam/deposit/by-price',
      body: request,
      idempotent: false,
    });
  }

  /** Статус депозитного заказа (общий для Steam/Top-Up/Vouchers). */
  order(orderId: string): Promise<DepositOrderStatusResult> {
    return this.transport.requestWithRaw<DepositOrderStatusResult>({
      method: 'GET',
      path: `/v1/deposit/order/${encodeURIComponent(orderId)}`,
    });
  }
}

/** Продукт "Steam" (собственный терминал, собственный токен, отдельный от Stars). */
export class SteamApi {
  readonly deposit: SteamDepositApi;

  constructor(private readonly transport: Transport) {
    this.deposit = new SteamDepositApi(transport);
  }

  /** Оценка платежа по желаемой сумме зачисления (`netAmount`) — оплата покупателем. */
  amount(query: SteamAmountQuoteQuery): Promise<SteamAmountQuote> {
    return this.transport.requestWithRaw<SteamAmountQuote>({
      method: 'GET',
      path: '/v3/steam/amount',
      query: mapQuery(query, {}),
    });
  }

  /** Создать заказ оплаты Steam покупателем, указывая желаемое зачисление. Не повторяется при сбое. */
  create(request: CreateSteamOrderRequest): Promise<SteamOrderCreated> {
    return this.transport.requestWithRaw<SteamOrderCreated>({
      method: 'POST',
      path: '/v3/steam',
      body: request,
      idempotent: false,
    });
  }

  /** Оценка зачисления по сумме, которую готов заплатить покупатель (`amount`), с учётом `margin`. */
  byAmount(query: SteamByAmountQuoteQuery): Promise<SteamByAmountQuote> {
    return this.transport.requestWithRaw<SteamByAmountQuote>({
      method: 'GET',
      path: '/v3/steam/by-amount',
      query: mapQuery(query, {}),
    });
  }

  /** Создать заказ оплаты Steam покупателем, указывая сумму платежа и `margin`. Не повторяется при сбое. */
  createByAmount(request: CreateSteamOrderByAmountRequest): Promise<SteamOrderByAmountCreated> {
    return this.transport.requestWithRaw<SteamOrderByAmountCreated>({
      method: 'POST',
      path: '/v3/steam/by-amount',
      body: request,
      idempotent: false,
    });
  }

  /** Статус заказа оплаты Steam покупателем. */
  order(id: string): Promise<SteamOrderStatus> {
    return this.transport.requestWithRaw<SteamOrderStatus>({
      method: 'GET',
      path: `/v3/steam/order/${encodeURIComponent(id)}`,
    });
  }
}
