"""Продукты «Цифровые товары» (`dg-api.wata.pro`): Steam, Stars, Top-Up, ваучеры."""

from __future__ import annotations

from .stars import AsyncStarsAPI, StarsAPI
from .steam import AsyncSteamAPI, SteamAPI
from .topup import AsyncTopupAPI, TopupAPI
from .vouchers import AsyncVouchersAPI, VouchersAPI

__all__ = [
    "StarsAPI",
    "AsyncStarsAPI",
    "SteamAPI",
    "AsyncSteamAPI",
    "TopupAPI",
    "AsyncTopupAPI",
    "VouchersAPI",
    "AsyncVouchersAPI",
]
