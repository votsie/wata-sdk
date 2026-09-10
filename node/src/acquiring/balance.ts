import { WataConfigError } from '../errors.js';
import { Transport } from '../http.js';
import type { Currency } from '../types.js';

export interface TerminalBalance {
  terminalPublicId: string;
  date: string;
  balance: number;
  currency: Currency;
}

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/**
 * Проверяет, что дата баланса — сегодняшняя или вчерашняя по UTC.
 * Бросает `WataConfigError` до сетевого вызова, если это не так.
 */
export function assertValidBalanceDate(date: string | Date): string {
  const iso = typeof date === 'string' ? date : toIsoDate(date);
  if (!DATE_PATTERN.test(iso)) {
    throw new WataConfigError(`Некорректный формат даты баланса: "${iso}". Ожидается формат YYYY-MM-DD.`);
  }

  const now = new Date();
  const today = toIsoDate(now);
  const yesterday = toIsoDate(new Date(now.getTime() - 24 * 60 * 60 * 1000));

  if (iso !== today && iso !== yesterday) {
    throw new WataConfigError(
      `Дата баланса допустима только сегодняшняя (${today}) или вчерашняя (${yesterday}) по UTC, получено: "${iso}".`,
    );
  }

  return iso;
}

export class BalanceApi {
  constructor(private readonly transport: Transport) {}

  /** Баланс терминала на дату. Допустимы только сегодня и вчера по UTC — проверяется локально. */
  get(date: string | Date): Promise<TerminalBalance> {
    const validatedDate = assertValidBalanceDate(date);
    return this.transport.request<TerminalBalance>({
      method: 'GET',
      path: '/finance/balance',
      query: { Date: validatedDate },
    });
  }
}
