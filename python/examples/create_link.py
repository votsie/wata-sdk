"""Пример: создание платёжной ссылки.

Запуск::

    WATA_ACQUIRING_TOKEN=<jwt терминала эквайринга> python examples/create_link.py
"""

from __future__ import annotations

import os

from wata import WataClient


def main() -> None:
    token = os.environ["WATA_ACQUIRING_TOKEN"]

    with WataClient(acquiring=token) as client:
        link = client.acquiring.links.create(
            amount=1500,
            currency="RUB",
            description="Тестовый заказ",
            order_id="demo-order-1",
            success_redirect_url="https://shop.example/success",
            fail_redirect_url="https://shop.example/fail",
        )
        print("Ссылка создана:", link.url)
        print("ID:", link.id)
        print("Статус:", link.status)


if __name__ == "__main__":
    main()
