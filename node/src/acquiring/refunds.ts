import { WataConfigError } from '../errors.js';
import { Transport } from '../http.js';
import type { TransactionKind, TransactionStatus } from '../types.js';

export interface CreateRefundRequest {
  /** uuid исходной транзакции. Она должна быть в статусе `Paid`. */
  originalTransactionId: string;
  /**
   * Сумма возврата. Валюта берётся из исходной транзакции — поля
   * "причина" не существует. Должна быть больше нуля, не больше
   * доступного остатка и не более двух знаков после запятой.
   */
  amount: number;
}

export interface RefundResult {
  transactionId: string;
  originalTransactionId: string;
  transactionStatus: TransactionStatus;
  /** Всегда `Refund`. */
  kind: TransactionKind;
  errorCode?: string;
  errorDescription?: string;
}

export class RefundsApi {
  constructor(private readonly transport: Transport) {}

  /**
   * Создать возврат. Недоступно на терминалах с продуктом «Цифровые
   * товары + Эквайринг». Не повторяется автоматически при сбое.
   */
  create(request: CreateRefundRequest): Promise<RefundResult> {
    if (!(request.amount > 0)) {
      throw new WataConfigError('Сумма возврата должна быть больше нуля.');
    }
    return this.transport.request<RefundResult>({
      method: 'POST',
      path: '/transactions/refunds',
      body: request,
      idempotent: false,
    });
  }
}
