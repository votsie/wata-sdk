"""Python SDK для платёжной системы WATA (эквайринг и цифровые товары).

Быстрый старт::

    from wata import WataClient

    client = WataClient(acquiring="<jwt терминала эквайринга>")
    link = client.acquiring.links.create(amount=1000, currency="RUB")
    print(link.url)

Подробности — в README.md и `docs/SPEC.md` в корне репозитория.
"""

from __future__ import annotations

from .client import AsyncWataClient, WataClient
from .errors import (
    WataApiError,
    WataAuthError,
    WataConfigError,
    WataError,
    WataNetworkError,
    WataRateLimitError,
    WataServerError,
    WataWebhookError,
)
from .types import (
    Balance,
    CardCryptoPaymentResult,
    Currency,
    DepositBalance,
    DepositOrderStatus,
    DgObject,
    DgOrderStatus,
    PaymentLink,
    PaymentLinkPage,
    PaymentLinkStatus,
    PaymentLinkType,
    PayerData,
    RefundResult,
    SbpPaymentResult,
    StarsOrderStatus,
    Subscription,
    SubscriptionInterval,
    ThreeDsData,
    TPayPaymentResult,
    Transaction,
    TransactionKind,
    TransactionPage,
    TransactionStatus,
    TransactionType,
    WebhookEvent,
)

__version__ = "0.1.0"

__all__ = [
    "__version__",
    "WataClient",
    "AsyncWataClient",
    # ошибки
    "WataError",
    "WataConfigError",
    "WataAuthError",
    "WataRateLimitError",
    "WataApiError",
    "WataServerError",
    "WataNetworkError",
    "WataWebhookError",
    # enum'ы
    "Currency",
    "TransactionStatus",
    "TransactionKind",
    "TransactionType",
    "PaymentLinkStatus",
    "PaymentLinkType",
    "SubscriptionInterval",
    "DgOrderStatus",
    "StarsOrderStatus",
    "DepositOrderStatus",
    # модели
    "PaymentLink",
    "PaymentLinkPage",
    "Transaction",
    "TransactionPage",
    "RefundResult",
    "Balance",
    "DepositBalance",
    "ThreeDsData",
    "CardCryptoPaymentResult",
    "SbpPaymentResult",
    "TPayPaymentResult",
    "Subscription",
    "PayerData",
    "WebhookEvent",
    "DgObject",
]
