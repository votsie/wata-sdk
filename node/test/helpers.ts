import type { WataFetch } from '../src/http.js';

export interface RecordedCall {
  url: string;
  method: string;
  headers: Record<string, string>;
  body?: string;
}

export interface MockResponseSpec {
  status: number;
  body?: unknown;
  headers?: Record<string, string>;
}

export type MockHandler = (
  call: RecordedCall,
  attemptIndex: number,
) => MockResponseSpec | Promise<MockResponseSpec>;

/**
 * Создаёт fetch-подобную функцию для внедрения в `WataClient({ fetch })`,
 * записывающую все вызовы и отвечающую по заданному обработчику.
 * Даёт более чистые и быстрые тесты, чем поднятие undici MockAgent.
 */
export function createMockFetch(handler: MockHandler): { fetchImpl: WataFetch; calls: RecordedCall[] } {
  const calls: RecordedCall[] = [];

  const fetchImpl = (async (input: string | URL | Request, init: RequestInit = {}) => {
    const url = typeof input === 'string' ? input : input.toString();
    const headers: Record<string, string> = {};
    new Headers(init.headers).forEach((value, key) => {
      headers[key] = value;
    });
    const call: RecordedCall = {
      url,
      method: init.method ?? 'GET',
      headers,
      body: typeof init.body === 'string' ? init.body : undefined,
    };
    calls.push(call);

    const attemptIndex = calls.filter((c) => c.url === call.url && c.method === call.method).length - 1;
    const spec = await handler(call, attemptIndex);
    const bodyText = spec.body === undefined ? '' : JSON.stringify(spec.body);
    return new Response(bodyText, { status: spec.status, headers: spec.headers });
  }) as WataFetch;

  return { fetchImpl, calls };
}

/** Обработчик, который всегда возвращает один и тот же ответ. */
export function constantHandler(spec: MockResponseSpec): MockHandler {
  return () => spec;
}
