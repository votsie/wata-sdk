import { Transport } from '../http.js';
import type { DepositOrderStatusResult, DigitalGoodsOrderStatus } from '../types.js';

/** Значение дополнительного поля заказа, требуемого конкретной позицией каталога (например, идентификатор игрового аккаунта). */
export type TopupCatalogField = Record<string, unknown>;

export interface TopupProduct {
  id: string;
  name: string;
  price: number;
  minPrice?: number;
  isAvailable: boolean;
}

export interface TopupCategory {
  categoryId: string;
  categoryName: string;
  type: string;
  fields: TopupCatalogField[];
  products: TopupProduct[];
}

export interface TopupCatalog {
  categories: TopupCategory[];
  /** Исходное тело ответа сервера — на случай недокументированных полей. */
  raw: unknown;
}

export interface CreateTopupOrderRequest {
  topupId: string;
  amount: number;
  orderId: string;
  /** Значения полей, требуемых позицией каталога — см. `fields` в `TopupCategory`. */
  fields: Record<string, unknown>;
  email: string;
  description: string;
}

export interface TopupOrderCreated {
  orderId: string;
  amount: number;
  commission: number;
  orderPrice: number;
  topupId: string;
  email: string;
  paymentLink: string;
  raw: unknown;
}

export interface TopupOrderStatus {
  orderId: string;
  status: DigitalGoodsOrderStatus;
  amount: number;
  orderPrice: number;
  topupId: string;
  email: string;
  paymentLink?: string;
  raw: unknown;
}

export interface DepositTopupProduct {
  id: string;
  name: string;
  price: number;
  isAvailable: boolean;
}

export interface DepositTopupCategory {
  id: string;
  name: string;
  fields: TopupCatalogField[];
  products: DepositTopupProduct[];
}

export interface DepositTopupCatalog {
  categories: DepositTopupCategory[];
  raw: unknown;
}

export interface CreateDepositTopupOrderRequest {
  topupId: string;
  categoryId: string;
  orderId: string;
  email: string;
  /** Не всякой позиции каталога нужны дополнительные поля. */
  fields?: Record<string, unknown>;
}

export interface DepositTopupOrderCreated {
  orderId: string;
  topupId: string;
  categoryId: string;
  price: number;
  status: DigitalGoodsOrderStatus;
  fields?: Record<string, unknown>;
  email: string;
  raw: unknown;
}

class TopupDepositApi {
  constructor(private readonly transport: Transport) {}

  /** Каталог пополнений, доступных для оплаты с депозита мерчанта. */
  all(): Promise<DepositTopupCatalog> {
    return this.transport.requestWithRaw<DepositTopupCatalog>({ method: 'GET', path: '/v1/deposit/topups' });
  }

  /** Создать заказ пополнения, списав средства с депозита мерчанта. Не повторяется при сбое. */
  create(request: CreateDepositTopupOrderRequest): Promise<DepositTopupOrderCreated> {
    return this.transport.requestWithRaw<DepositTopupOrderCreated>({
      method: 'POST',
      path: '/v1/deposit/topups',
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

/** Продукт "Top-Up" (собственный терминал, собственный токен). */
export class TopupApi {
  readonly deposit: TopupDepositApi;

  constructor(private readonly transport: Transport) {
    this.deposit = new TopupDepositApi(transport);
  }

  /** Каталог пополнений, доступных для оплаты покупателем (эквайринг). */
  async all(): Promise<TopupCatalog> {
    const categories = await this.transport.request<TopupCategory[]>({ method: 'GET', path: '/v3/topup/all' });
    return { categories, raw: categories };
  }

  /** Создать заказ пополнения, оплачиваемый покупателем. Не повторяется при сбое. */
  create(request: CreateTopupOrderRequest): Promise<TopupOrderCreated> {
    return this.transport.requestWithRaw<TopupOrderCreated>({
      method: 'POST',
      path: '/v3/topup',
      body: request,
      idempotent: false,
    });
  }

  /** Статус заказа пополнения, оплачиваемого покупателем. */
  order(id: string): Promise<TopupOrderStatus> {
    return this.transport.requestWithRaw<TopupOrderStatus>({
      method: 'GET',
      path: `/v3/topup/orders/${encodeURIComponent(id)}`,
    });
  }
}
