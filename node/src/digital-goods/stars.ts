import { WataConfigError } from '../errors.js';
import { Transport, mapQuery } from '../http.js';
import type { StarsOrderStatus } from '../types.js';

const MIN_COUNT = 50;
const MAX_COUNT = 50000;

export interface StarsPriceQuery {
  /** Telegram username получателя. */
  username: string;
}

export interface StarsPrice {
  starPrice: number;
  minPrice: number;
  raw: unknown;
}

export interface CreateStarsOrderRequest {
  username: string;
  /** Количество звёзд, от 50 до 50000. */
  count: number;
  amount: number;
  description: string;
  orderId: string;
}

export interface StarsOrderCreated {
  orderId: string;
  username: string;
  count: number;
  amount: number;
  price: number;
  commission: number;
  description: string;
  paymentLink: string;
  raw: unknown;
}

export interface StarsOrderStatusResult {
  /**
   * Статус заказа. Заказы дороже порога (порядка 2000 ₽) попадают в
   * статус `Review` и НЕ будут выполнены сами по себе — их нужно явно
   * подтвердить через `confirm()`, иначе покупатель никогда не
   * получит звёзды.
   */
  status: StarsOrderStatus;
  username: string;
  count: number;
  amount: number;
  description?: string;
  creationTime?: string;
  raw: unknown;
}

function assertValidCount(count: number): void {
  if (!Number.isFinite(count) || count < MIN_COUNT || count > MAX_COUNT) {
    throw new WataConfigError(`Количество звёзд должно быть от ${MIN_COUNT} до ${MAX_COUNT}, получено: ${count}.`);
  }
}

/** Продукт "Telegram Stars" (собственный терминал, собственный токен, отдельный от Steam). */
export class StarsApi {
  constructor(private readonly transport: Transport) {}

  /** Стоимость звезды и минимально допустимая сумма для указанного получателя. */
  price(query: StarsPriceQuery): Promise<StarsPrice> {
    return this.transport.requestWithRaw<StarsPrice>({ method: 'GET', path: '/stars/price', query: mapQuery(query, {}) });
  }

  /**
   * Создать заказ. Не повторяется автоматически при сбое. Заказы
   * дороже порога попадут в статус `Review` — см. `confirm()`/`reject()`.
   */
  create(request: CreateStarsOrderRequest): Promise<StarsOrderCreated> {
    assertValidCount(request.count);
    return this.transport.requestWithRaw<StarsOrderCreated>({
      method: 'POST',
      path: '/stars',
      body: request,
      idempotent: false,
    });
  }

  /** Статус заказа. */
  order(id: string): Promise<StarsOrderStatusResult> {
    return this.transport.requestWithRaw<StarsOrderStatusResult>({
      method: 'GET',
      path: `/stars/order/${encodeURIComponent(id)}`,
    });
  }

  /** Подтвердить заказ, находящийся в статусе `Review` (переводит его в `Paid`), чтобы он был выполнен. */
  confirm(id: string): Promise<StarsOrderStatusResult> {
    return this.transport.requestWithRaw<StarsOrderStatusResult>({
      method: 'POST',
      path: `/stars/order/${encodeURIComponent(id)}/confirm`,
      idempotent: false,
    });
  }

  /** Отклонить заказ, находящийся в статусе `Review` (переводит его в `Refunded`). */
  reject(id: string): Promise<StarsOrderStatusResult> {
    return this.transport.requestWithRaw<StarsOrderStatusResult>({
      method: 'POST',
      path: `/stars/order/${encodeURIComponent(id)}/reject`,
      idempotent: false,
    });
  }
}
