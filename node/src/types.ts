/**
 * Общие типы и enum'ы WATA SDK.
 *
 * Enum'ы объявлены как "открытые" строковые объединения (`OpenEnum`):
 * известные значения дают автодополнение, но неизвестное значение,
 * пришедшее с сервера, не ломает разбор ответа — оно просто остаётся
 * строкой. Это прямое требование спецификации (раздел 6): API живой
 * и может добавить новое значение enum'а раньше, чем выйдет новая
 * версия SDK.
 */

/** Строковый union, открытый для значений, которых ещё нет в списке известных. */
export type OpenEnum<Known extends string> = Known | (string & {});

export type Environment = 'production' | 'sandbox';

/** ISO-код валюты. GBP присутствует в схеме WATA, но не подтверждён документацией. */
export type Currency = OpenEnum<'RUB' | 'USD' | 'EUR' | 'GBP'>;

export type TransactionStatus = OpenEnum<'Created' | 'Pending' | 'Paid' | 'Declined'>;

export type TransactionKind = OpenEnum<'Payment' | 'Refund'>;

export type TransactionType = OpenEnum<'CardCrypto' | 'SBP' | 'TPay'>;

export type PaymentLinkStatus = OpenEnum<'Opened' | 'Closed'>;

export type PaymentLinkType = OpenEnum<'OneTime' | 'ManyTime'>;

export type SubscriptionInterval = OpenEnum<'Test' | 'Week' | 'Month'>;

/** Статус заказа цифровых товаров (Steam / Top-Up / Vouchers). */
export type DigitalGoodsOrderStatus = OpenEnum<'Pending' | 'Paid' | 'Success' | 'Fail'>;

/**
 * Статус заказа Telegram Stars.
 *
 * Заказы дороже порога, установленного мерчантом, переходят в статус
 * `Review` и НЕ выполняются автоматически — их нужно явно подтвердить
 * через `client.stars.confirm(id)`, иначе выдачи не будет никогда.
 */
export type StarsOrderStatus = OpenEnum<'Pending' | 'Review' | 'Paid' | 'Refunded' | 'Success' | 'Fail'>;

export type DepositOrderStatus = OpenEnum<'Pending' | 'Success' | 'Fail'>;

/** Продукт, к которому относится депозитный заказ. */
export type DepositOrderType = OpenEnum<'Steam' | 'TopUp' | 'Vouchers'>;

/** Ответ на постраничный (не курсорный) поиск. */
export interface PagedResult<T> {
  items: T[];
  totalCount: number;
}

/** Баланс депозита мерчанта для продуктов "оплата с депозита". */
export interface DepositBalance {
  totalBalance: number;
  frozenBalance: number;
  availableBalance: number;
  /** Исходное тело ответа сервера — на случай недокументированных полей. */
  raw: unknown;
}

/**
 * Статус депозитного заказа — общий эндпоинт `GET /v1/deposit/order/{orderId}`
 * для Steam, Top-Up и ваучеров, оплачиваемых с депозита мерчанта.
 */
export interface DepositOrderStatusResult {
  orderId: string;
  price: number;
  status: DepositOrderStatus;
  type: DepositOrderType;
  /** Детали заказа, специфичные для продукта. Может быть пустым. */
  details?: unknown;
  /** Исходное тело ответа сервера — на случай недокументированных полей. */
  raw: unknown;
}
