import { Transport } from '../http.js';
import type { DepositOrderStatusResult, DigitalGoodsOrderStatus } from '../types.js';

export type VoucherCatalogField = Record<string, unknown>;

export interface VoucherProduct {
  id: string;
  name: string;
  price: number;
  minPrice?: number;
  isAvailable: boolean;
  stock?: number;
}

export interface VoucherCategory {
  categoryId: string;
  categoryName: string;
  type: string;
  fields: VoucherCatalogField[];
  vouchers: VoucherProduct[];
}

export interface VoucherCatalog {
  categories: VoucherCategory[];
  /** Исходное тело ответа сервера — на случай недокументированных полей. */
  raw: unknown;
}

export interface CreateVoucherOrderRequest {
  voucherId: string;
  amount: number;
  count: number;
  orderId: string;
  email: string;
  description: string;
}

export interface VoucherOrderCreated {
  orderId: string;
  amount: number;
  commission: number;
  orderPrice: number;
  voucherId: string;
  count: number;
  email: string;
  paymentLink: string;
  raw: unknown;
}

export interface VoucherOrderStatus {
  orderId: string;
  status: DigitalGoodsOrderStatus;
  /** Коды ваучеров. Появляются здесь же — отдельного эндпоинта выдачи нет — и могут прийти с задержкой до 10 минут. */
  vouchers?: string[];
  amount: number;
  orderPrice: number;
  voucherId: string;
  count: number;
  email: string;
  raw: unknown;
}

export interface DepositVoucherProduct {
  id: string;
  name: string;
  price: number;
  stock?: number;
  isAvailable: boolean;
}

export interface DepositVoucherCategory {
  id: string;
  name: string;
  products: DepositVoucherProduct[];
}

export interface DepositVoucherCatalog {
  categories: DepositVoucherCategory[];
  raw: unknown;
}

export interface CreateDepositVoucherOrderRequest {
  voucherId: string;
  categoryId: string;
  count: number;
  orderId: string;
  email: string;
}

export interface DepositVoucherOrderCreated {
  orderId: string;
  voucherId: string;
  categoryId: string;
  count: number;
  price: number;
  status: DigitalGoodsOrderStatus;
  /** Коды ваучеров, могут прийти с задержкой до 10 минут. */
  codes?: string[];
  email: string;
  raw: unknown;
}

class VouchersDepositApi {
  constructor(private readonly transport: Transport) {}

  /** Каталог ваучеров, доступных для оплаты с депозита мерчанта. */
  all(): Promise<DepositVoucherCatalog> {
    return this.transport.requestWithRaw<DepositVoucherCatalog>({ method: 'GET', path: '/v1/deposit/vouchers' });
  }

  /** Создать заказ ваучера, списав средства с депозита мерчанта. Не повторяется при сбое. */
  create(request: CreateDepositVoucherOrderRequest): Promise<DepositVoucherOrderCreated> {
    return this.transport.requestWithRaw<DepositVoucherOrderCreated>({
      method: 'POST',
      path: '/v1/deposit/vouchers',
      body: request,
      idempotent: false,
    });
  }

  /** Статус депозитного заказа (общий для Steam/Top-Up/Vouchers). Коды ваучера могут прийти с задержкой до 10 минут. */
  order(orderId: string): Promise<DepositOrderStatusResult> {
    return this.transport.requestWithRaw<DepositOrderStatusResult>({
      method: 'GET',
      path: `/v1/deposit/order/${encodeURIComponent(orderId)}`,
    });
  }
}

/** Продукт "Ваучеры" (собственный терминал, собственный токен). */
export class VouchersApi {
  readonly deposit: VouchersDepositApi;

  constructor(private readonly transport: Transport) {
    this.deposit = new VouchersDepositApi(transport);
  }

  /** Каталог ваучеров, доступных для оплаты покупателем (эквайринг). */
  async all(): Promise<VoucherCatalog> {
    const categories = await this.transport.request<VoucherCategory[]>({ method: 'GET', path: '/v3/vouchers/all' });
    return { categories, raw: categories };
  }

  /** Создать заказ ваучера, оплачиваемый покупателем. Не повторяется при сбое. */
  create(request: CreateVoucherOrderRequest): Promise<VoucherOrderCreated> {
    return this.transport.requestWithRaw<VoucherOrderCreated>({
      method: 'POST',
      path: '/v3/vouchers',
      body: request,
      idempotent: false,
    });
  }

  /** Статус заказа ваучера, оплачиваемого покупателем. Коды появляются в этом же ответе, могут прийти с задержкой до 10 минут. */
  order(id: string): Promise<VoucherOrderStatus> {
    return this.transport.requestWithRaw<VoucherOrderStatus>({
      method: 'GET',
      path: `/v3/vouchers/order/${encodeURIComponent(id)}`,
    });
  }
}
