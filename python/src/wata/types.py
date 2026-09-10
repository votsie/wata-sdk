"""Enum'ы и модели данных WATA SDK.

Все модели — как эквайринга, так и цифровых товаров (Steam/Stars/Top-Up/
ваучеры) — полностью типизированы согласно `docs/SPEC.md` (раздел 10 даёт
точные имена полей Digital Goods). При этом каждая модель ответа Digital
Goods дополнительно хранит исходное тело ответа в поле `raw`: документация
WATA покрывает не все возвращаемые поля, и без `raw` интегратор не смог бы
добраться до недокументированного значения, не дожидаясь новой версии SDK.

Неизвестное значение enum, пришедшее с сервера, не ломает разбор ответа:
исходная строка сохраняется, а не отбрасывается (см. :class:`_ExtensibleStrEnum`).
"""

from __future__ import annotations

import dataclasses
import enum
from typing import Any, TypeVar, Union, get_args, get_origin, get_type_hints

# Модуль стандартной библиотеки, а не соседний types.py: файл пакета называется
# так же, и абсолютный импорт здесь обязателен.
try:  # Python 3.10+
    from types import UnionType  # type: ignore[attr-defined]
except ImportError:  # pragma: no cover — на поддерживаемых версиях недостижимо
    UnionType = None  # type: ignore[assignment]

from ._casing import snake_to_camel

# --------------------------------------------------------------------------
# Enum'ы, устойчивые к новым значениям сервера
# --------------------------------------------------------------------------


class _ExtensibleStrEnum(str, enum.Enum):
    """`str, Enum` c сохранением незнакомых значений сервера.

    API живой и может прислать значение enum, которого ещё нет в SDK. Вместо
    исключения при разборе мы динамически регистрируем псевдо-член с исходной
    строкой в качестве значения — данные не теряются, интеграция не падает.
    """

    @classmethod
    def _missing_(cls, value: object) -> "_ExtensibleStrEnum | None":
        if not isinstance(value, str):
            return None
        pseudo = str.__new__(cls, value)
        pseudo._name_ = f"UNKNOWN_{value}"
        pseudo._value_ = value
        cls._value2member_map_[value] = pseudo
        return pseudo

    @property
    def is_known(self) -> bool:
        return not self._name_.startswith("UNKNOWN_")


class Currency(_ExtensibleStrEnum):
    RUB = "RUB"
    USD = "USD"
    EUR = "EUR"
    # Присутствует в схеме API, но не подтверждён документацией WATA.
    GBP = "GBP"


class TransactionStatus(_ExtensibleStrEnum):
    CREATED = "Created"
    PENDING = "Pending"
    PAID = "Paid"
    DECLINED = "Declined"


class TransactionKind(_ExtensibleStrEnum):
    PAYMENT = "Payment"
    REFUND = "Refund"


class TransactionType(_ExtensibleStrEnum):
    CARD_CRYPTO = "CardCrypto"
    SBP = "SBP"
    TPAY = "TPay"


class PaymentLinkStatus(_ExtensibleStrEnum):
    OPENED = "Opened"
    CLOSED = "Closed"


class PaymentLinkType(_ExtensibleStrEnum):
    ONE_TIME = "OneTime"
    MANY_TIME = "ManyTime"


class SubscriptionInterval(_ExtensibleStrEnum):
    TEST = "Test"
    WEEK = "Week"
    MONTH = "Month"


class DgOrderStatus(_ExtensibleStrEnum):
    """Статус заказа цифровых товаров (Steam, Top-Up, ваучеры)."""

    PENDING = "Pending"
    PAID = "Paid"
    SUCCESS = "Success"
    FAIL = "Fail"


class StarsOrderStatus(_ExtensibleStrEnum):
    """Статус заказа Telegram Stars.

    Заказы дороже порога модерации попадают в ``REVIEW`` и не выполняются,
    пока их явно не подтвердят через ``confirm``/``reject``.
    """

    PENDING = "Pending"
    REVIEW = "Review"
    PAID = "Paid"
    REFUNDED = "Refunded"
    SUCCESS = "Success"
    FAIL = "Fail"


class DepositOrderStatus(_ExtensibleStrEnum):
    PENDING = "Pending"
    SUCCESS = "Success"
    FAIL = "Fail"


class DepositOrderType(_ExtensibleStrEnum):
    """Тип депозитного заказа — `type` в ответе `GET /v1/deposit/order/{orderId}`."""

    STEAM = "Steam"
    TOPUP = "TopUp"
    VOUCHERS = "Vouchers"


# --------------------------------------------------------------------------
# Обвязка dataclass <-> JSON WATA (camelCase)
# --------------------------------------------------------------------------

T = TypeVar("T")


def _unwrap_optional(tp: Any) -> Any:
    """Разворачивает ``X | None`` в ``X``.

    Проверяются обе формы объединения. До Python 3.14 запись ``X | None``
    давала ``types.UnionType``, а ``Optional[X]`` — ``typing.Union``, и это
    разные значения ``get_origin``. Учитывать только одну форму значит тихо
    не привести значение к enum на части версий Python — ошибка проявится
    у пользователя, а не в тестах разработчика.
    """

    origin = get_origin(tp)
    if origin is Union or (UnionType is not None and origin is UnionType):
        args = [a for a in get_args(tp) if a is not type(None)]
        if len(args) == 1:
            return args[0]
    return tp


def _coerce_value(value: Any, field_type: Any) -> Any:
    if value is None:
        return None
    field_type = _unwrap_optional(field_type)
    origin = get_origin(field_type)
    if origin in (list, tuple):
        (item_type,) = get_args(field_type) or (Any,)
        return [_coerce_value(v, item_type) for v in value]
    if isinstance(field_type, type) and issubclass(field_type, enum.Enum):
        return field_type(value)
    if dataclasses.is_dataclass(field_type) and isinstance(value, dict):
        return model_from_api(field_type, value)
    return value


def _encode_value(value: Any) -> Any:
    if isinstance(value, enum.Enum):
        return value.value
    if dataclasses.is_dataclass(value):
        return model_to_api(value)
    if isinstance(value, (list, tuple)):
        return [_encode_value(v) for v in value]
    return value


def model_from_api(cls: type[T], data: dict[str, Any]) -> T:
    """Строит dataclass ``cls`` из JSON-объекта WATA (camelCase -> snake_case).

    Ключи, которых нет среди полей ``cls``, молча игнорируются — это делает
    разбор устойчивым к новым полям, добавленным сервером.
    """

    hints = get_type_hints(cls)
    kwargs: dict[str, Any] = {}
    for f in dataclasses.fields(cls):
        api_key = f.metadata.get("api_name", snake_to_camel(f.name))
        if api_key not in data:
            continue
        kwargs[f.name] = _coerce_value(data[api_key], hints[f.name])
    return cls(**kwargs)


def model_from_api_with_raw(cls: type[T], data: dict[str, Any]) -> T:
    """Как :func:`model_from_api`, но дополнительно сохраняет исходное тело в `raw`.

    Используется моделями Digital Goods (Steam/Stars/Top-Up/ваучеры/депозит):
    документация WATA по этим ручкам не описывает все поля ответа, поэтому
    каждая такая модель обязана хранить сырое тело — иначе недокументированное
    поле было бы недоступно интегратору вплоть до новой версии SDK. `cls`
    обязан иметь поле `raw: dict[str, Any]` с значением по умолчанию.
    """

    instance = model_from_api(cls, data)
    instance.raw = dict(data)  # type: ignore[attr-defined]
    return instance


def model_to_api(obj: Any) -> dict[str, Any]:
    """Сериализует dataclass в JSON-совместимый словарь camelCase.

    Значения ``None`` не включаются в результат — WATA API не принимает
    пустые/`null` поля в теле запроса.
    """

    result: dict[str, Any] = {}
    for f in dataclasses.fields(obj):
        value = getattr(obj, f.name)
        if value is None:
            continue
        api_key = f.metadata.get("api_name", snake_to_camel(f.name))
        result[api_key] = _encode_value(value)
    return result


def clean(data: dict[str, Any]) -> dict[str, Any]:
    """Убирает `None`/пустые значения из тела запроса или query-параметров."""

    cleaned: dict[str, Any] = {}
    for key, value in data.items():
        if value is None:
            continue
        if value in ("", [], {}):
            continue
        cleaned[key] = value
    return cleaned


# --------------------------------------------------------------------------
# Эквайринг — модели
# --------------------------------------------------------------------------


@dataclasses.dataclass(slots=True)
class Subscription:
    period: int
    interval: SubscriptionInterval
    max_periods: int
    amount: float
    start_date: str | None = None


@dataclasses.dataclass(slots=True)
class PaymentLink:
    id: str
    amount: float
    currency: Currency
    status: PaymentLinkStatus
    url: str
    terminal_name: str | None = None
    terminal_public_id: str | None = None
    creation_time: str | None = None
    type: PaymentLinkType | None = None
    order_id: str | None = None
    expiration_date_time: str | None = None
    is_arbitrary_amount_allowed: bool | None = None
    arbitrary_amounts: list[float] | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "PaymentLink":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class PaymentLinkPage:
    items: list[PaymentLink]
    total_count: int

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "PaymentLinkPage":
        return cls(
            items=[PaymentLink.from_api(item) for item in data.get("items", [])],
            total_count=data.get("totalCount", 0),
        )


@dataclasses.dataclass(slots=True)
class Transaction:
    """Транзакция эквайринга.

    SPEC.md не перечисляет исчерпывающий список полей ответа `/transactions`
    (кроме явно упомянутого расхождения имени комиссии: `totalCommission` в
    ответе транзакции против `commission` в вебхуке). Набор полей ниже —
    наиболее вероятный по аналогии с `PaymentLink` и вебхуком; неизвестные
    дополнительные поля сервера игнорируются, а не ломают разбор.
    """

    id: str
    amount: float | None = None
    currency: Currency | None = None
    status: TransactionStatus | None = None
    kind: TransactionKind | None = None
    type: TransactionType | None = None
    order_id: str | None = None
    payment_link_id: str | None = None
    creation_time: str | None = None
    payment_time: str | None = None
    error_code: str | None = None
    error_description: str | None = None
    total_commission: float | None = None
    email: str | None = None
    terminal_public_id: str | None = None
    terminal_name: str | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "Transaction":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class TransactionPage:
    items: list[Transaction]
    has_next_page: bool
    next_cursor_id: str | None = None
    next_cursor_date: str | None = None
    next_cursor_amount: float | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "TransactionPage":
        return cls(
            items=[Transaction.from_api(item) for item in data.get("items", [])],
            has_next_page=bool(data.get("hasNextPage", False)),
            next_cursor_id=data.get("nextCursorId"),
            next_cursor_date=data.get("nextCursorDate"),
            next_cursor_amount=data.get("nextCursorAmount"),
        )


@dataclasses.dataclass(slots=True)
class RefundResult:
    transaction_id: str
    original_transaction_id: str
    transaction_status: TransactionStatus
    kind: TransactionKind
    error_code: str | None = None
    error_description: str | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "RefundResult":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class Balance:
    terminal_public_id: str
    date: str
    balance: float
    currency: Currency

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "Balance":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class ThreeDsData:
    url: str
    method: str
    parameters: dict[str, Any] = dataclasses.field(default_factory=dict)

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "ThreeDsData":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class CardCryptoPaymentResult:
    """Ответ `POST /payments/card-crypto`.

    Криптограмму карты формирует скрипт чекаута в браузере плательщика —
    сервер мерчанта её не собирает и не видит номер карты. `three_ds_data`
    заполнен, если банк потребовал прохождение 3-D Secure (редирект или
    автосабмит формы).
    """

    id: str | None = None
    amount: float | None = None
    currency: Currency | None = None
    status: TransactionStatus | None = None
    order_id: str | None = None
    three_ds_data: ThreeDsData | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "CardCryptoPaymentResult":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class SbpPaymentResult:
    """Ответ `POST /payments/sbp`. Валюта всегда RUB, отдельного поля нет."""

    sbp_link: str
    id: str | None = None
    amount: float | None = None
    status: TransactionStatus | None = None
    order_id: str | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "SbpPaymentResult":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class TPayPaymentResult:
    """Ответ `POST /payments/tpay`."""

    t_pay_link: str
    id: str | None = None
    amount: float | None = None
    status: TransactionStatus | None = None
    order_id: str | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "TPayPaymentResult":
        return model_from_api(cls, data)


# --------------------------------------------------------------------------
# Вебхуки
# --------------------------------------------------------------------------


@dataclasses.dataclass(slots=True)
class PayerData:
    payer_id: str | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "PayerData":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class WebhookEvent:
    """Разобранное событие вебхука (после успешной проверки подписи)."""

    transaction_type: TransactionType | None = None
    kind: TransactionKind | None = None
    id: str | None = None
    transaction_id: str | None = None
    original_transaction_id: str | None = None
    terminal_public_id: str | None = None
    transaction_status: TransactionStatus | None = None
    error_code: str | None = None
    error_description: str | None = None
    terminal_name: str | None = None
    amount: float | None = None
    currency: Currency | None = None
    order_id: str | None = None
    order_description: str | None = None
    commission: float | None = None
    payment_time: str | None = None
    email: str | None = None
    payment_link_id: str | None = None
    payer_data: PayerData | None = None

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "WebhookEvent":
        return model_from_api(cls, data)


# --------------------------------------------------------------------------
# Цифровые товары — гибкая модель ответа
# --------------------------------------------------------------------------


@dataclasses.dataclass(slots=True)
class DepositBalance:
    """Ответ `GET /v1/deposit/balance`."""

    total_balance: float
    frozen_balance: float
    available_balance: float

    @classmethod
    def from_api(cls, data: dict[str, Any]) -> "DepositBalance":
        return model_from_api(cls, data)


@dataclasses.dataclass(slots=True)
class DgObject:
    """Гибкий ответ цифровых товаров (Steam/Stars/Top-Up/ваучеры/депозит).

    SPEC.md не фиксирует полный список полей JSON для этих ручек (в отличие
    от эквайринга). Чтобы не терять данные и не изобретать несуществующие
    имена полей, ответ хранится как есть в ``raw`` (ключи — исходные,
    camelCase), а типизированные поля (``status`` и т.п.) вычисляются поверх
    него. Любое поле доступно и по snake_case-имени через атрибут:
    ``order.order_id`` эквивалентно ``order.raw["orderId"]``.
    """

    raw: dict[str, Any]
    _status_enum: type[enum.Enum] | None = dataclasses.field(default=None, repr=False)

    @classmethod
    def from_api(cls, data: dict[str, Any], *, status_enum: type[enum.Enum] | None = None) -> "DgObject":
        return cls(raw=dict(data), _status_enum=status_enum)

    @property
    def status(self) -> Any:
        value = self.raw.get("status")
        if value is None:
            return None
        if self._status_enum is not None:
            return self._status_enum(value)
        return value

    def get(self, key: str, default: Any = None) -> Any:
        return self.raw.get(key, default)

    def __getitem__(self, key: str) -> Any:
        return self.raw[key]

    def __getattr__(self, item: str) -> Any:
        if item.startswith("_"):
            raise AttributeError(item)
        camel = snake_to_camel(item)
        if camel in self.raw:
            return self.raw[camel]
        if item in self.raw:
            return self.raw[item]
        raise AttributeError(item)
