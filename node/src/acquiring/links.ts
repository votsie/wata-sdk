import { Transport, mapQuery } from '../http.js';
import type { Currency, PagedResult, PaymentLinkStatus, PaymentLinkType, SubscriptionInterval } from '../types.js';

export interface CreateLinkSubscription {
  period: number;
  interval: SubscriptionInterval;
  maxPeriods: number;
  amount: number;
  startDate?: string;
}

export interface CreateLinkRequest {
  amount: number;
  currency: Currency;
  description?: string;
  orderId?: string;
  successRedirectUrl?: string;
  failRedirectUrl?: string;
  /** ISO 8601. По умолчанию сервер выставляет 3 дня, допустимо от 10 минут до 30 дней. */
  expirationDateTime?: string;
  /** По умолчанию `OneTime`. */
  type?: PaymentLinkType;
  isArbitraryAmountAllowed?: boolean;
  /** Не более 6 подсказок, каждая не меньше `amount`. SDK не проверяет это жёстко. */
  arbitraryAmountPrompts?: number[];
  email?: string;
  phone?: string;
  username?: string;
  userId?: string;
  subscription?: CreateLinkSubscription;
}

export interface PaymentLink {
  id: string;
  amount: number;
  currency: Currency;
  status: PaymentLinkStatus;
  url: string;
  terminalName: string;
  terminalPublicId: string;
  creationTime: string;
  type: PaymentLinkType;
  orderId?: string;
  expirationDateTime?: string;
  isArbitraryAmountAllowed?: boolean;
  arbitraryAmounts?: number[];
}

export interface SearchLinksQuery {
  orderId?: string;
  creationTimeFrom?: string;
  creationTimeTo?: string;
  amountFrom?: number;
  amountTo?: number;
  currencies?: Currency[];
  statuses?: PaymentLinkStatus[];
  /** `orderId` | `creationTime` | `amount`, с необязательным суффиксом ` desc`. */
  sorting?: string;
  skipCount?: number;
  /** По умолчанию 10, максимум 1000. */
  maxResultCount?: number;
}

const SEARCH_LINKS_KEY_MAP: Record<string, string> = {
  orderId: 'OrderId',
  creationTimeFrom: 'CreationTimeFrom',
  creationTimeTo: 'CreationTimeTo',
  amountFrom: 'AmountFrom',
  amountTo: 'AmountTo',
  currencies: 'Currencies',
  statuses: 'Statuses',
  sorting: 'Sorting',
  skipCount: 'SkipCount',
  maxResultCount: 'MaxResultCount',
};

export class LinksApi {
  constructor(private readonly transport: Transport) {}

  /** Создать платёжную ссылку. Не повторяется автоматически при сбое — повтор создаст вторую ссылку. */
  create(request: CreateLinkRequest): Promise<PaymentLink> {
    return this.transport.request<PaymentLink>({
      method: 'POST',
      path: '/links',
      body: request,
      idempotent: false,
    });
  }

  /** Постраничный поиск ссылок. */
  search(query: SearchLinksQuery = {}): Promise<PagedResult<PaymentLink>> {
    return this.transport.request<PagedResult<PaymentLink>>({
      method: 'GET',
      path: '/links',
      query: mapQuery(query, SEARCH_LINKS_KEY_MAP),
    });
  }

  /** Ссылка по идентификатору. */
  get(id: string): Promise<PaymentLink> {
    return this.transport.request<PaymentLink>({ method: 'GET', path: `/links/${encodeURIComponent(id)}` });
  }
}
