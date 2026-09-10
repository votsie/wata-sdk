"""Пример: приём и проверка подписи вебхука WATA (только стандартная библиотека).

Запуск::

    pip install wata[webhooks]
    WATA_ACQUIRING_TOKEN=<jwt терминала эквайринга> python examples/webhook_server.py

Затем отправьте POST на http://localhost:8000/webhooks/wata с заголовком
`X-Signature` и телом события — SDK проверит подпись SHA512withRSA по ключу,
полученному через `GET /public-key`, и разберёт событие.
"""

from __future__ import annotations

import os
from http.server import BaseHTTPRequestHandler, HTTPServer

from wata import WataClient, WataWebhookError

client = WataClient(acquiring=os.environ.get("WATA_ACQUIRING_TOKEN", ""))


class WebhookHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:  # noqa: N802 — имя метода задано http.server
        content_length = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_length)  # сырое тело: bytes, не разобранный JSON
        signature = self.headers.get("X-Signature", "")

        try:
            is_valid = client.verify_webhook(raw_body, signature)
        except WataWebhookError as exc:
            print("Не удалось проверить подпись:", exc)
            self.send_response(400)
            self.end_headers()
            return

        if not is_valid:
            print("Подпись не совпала — запрос отклонён")
            self.send_response(401)
            self.end_headers()
            return

        event = client.parse_webhook(raw_body)
        print(
            f"Событие {event.kind}: заказ={event.order_id} статус={event.transaction_status} "
            f"сумма={event.amount} {event.currency}"
        )

        # Обработчик обязан быть идемпотентным: в реальном приложении здесь
        # нужно свериться с id транзакции/события в своей БД перед повторной
        # обработкой, и в любом случае вернуть 200 — иначе WATA будет
        # повторять доставку постоплатных/возвратных вебхуков до 32 часов.

        self.send_response(200)
        self.end_headers()

    def log_message(self, format: str, *args: object) -> None:  # noqa: A002 — сигнатура базового класса
        pass  # приглушаем стандартный лог http.server, у нас свой print выше


def main() -> None:
    server = HTTPServer(("0.0.0.0", 8000), WebhookHandler)
    print("Слушаю вебхуки WATA на http://0.0.0.0:8000/webhooks/wata ...")
    try:
        server.serve_forever()
    finally:
        client.close()


if __name__ == "__main__":
    main()
