package wata

import (
	"context"
	"net/http"
	"net/url"
)

// -----------------------------------------------------------------------
// Steam
// -----------------------------------------------------------------------

// SteamService groups the Steam top-up endpoints. Every method requires
// Tokens.Steam — Steam always lives on a terminal of its own, separate from
// Stars, and calling it with any other product's token is rejected by the
// server, not just by this SDK.
type SteamService struct{ c *Client }

// QuoteByNetAmount returns the price the payer must pay for req.NetAmount to
// be credited to req.Account on Steam (GET /v3/steam/amount,
// acquiring-funded).
func (s *SteamService) QuoteByNetAmount(ctx context.Context, req SteamNetAmountQuery) (*SteamQuote, error) {
	return s.getQuote(ctx, "/v3/steam/amount", req.toQuery())
}

// QuoteByAmount returns the amount that will be credited to req.Account for
// a given payment amount and margin (GET /v3/steam/by-amount,
// acquiring-funded).
func (s *SteamService) QuoteByAmount(ctx context.Context, req SteamAmountQuery) (*SteamQuote, error) {
	return s.getQuote(ctx, "/v3/steam/by-amount", req.toQuery())
}

// CreateOrderByNetAmount creates an acquiring-funded Steam top-up order
// specifying the amount to be credited to the account (POST /v3/steam).
// This is a mutating request and is never retried automatically.
func (s *SteamService) CreateOrderByNetAmount(ctx context.Context, req CreateSteamOrderByNetAmountRequest) (*SteamOrder, error) {
	return s.createOrder(ctx, "/v3/steam", req)
}

// CreateOrderByAmount creates an acquiring-funded Steam top-up order
// specifying the payment amount and margin (POST /v3/steam/by-amount). This
// is a mutating request and is never retried automatically.
func (s *SteamService) CreateOrderByAmount(ctx context.Context, req CreateSteamOrderByAmountRequest) (*SteamOrder, error) {
	return s.createOrder(ctx, "/v3/steam/by-amount", req)
}

// GetOrder fetches an acquiring-funded Steam order by id (GET
// /v3/steam/order/{id}).
func (s *SteamService) GetOrder(ctx context.Context, id string) (*SteamOrder, error) {
	var out SteamOrder
	if err := s.c.do(ctx, productSteam, apiRequest{
		Method: http.MethodGet,
		Path:   "/v3/steam/order/" + url.PathEscape(id),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// DepositQuoteByNetAmount returns the deposit price (in USD) for
// req.NetAmount to be credited to req.Account on Steam (GET
// /v1/steam/deposit/price, deposit-funded).
func (s *SteamService) DepositQuoteByNetAmount(ctx context.Context, req SteamDepositPriceQuery) (*SteamQuote, error) {
	return s.getQuote(ctx, "/v1/steam/deposit/price", req.toQuery())
}

// DepositQuoteByPrice returns the net amount credited for a given deposit
// price (GET /v1/steam/deposit/netamount, deposit-funded).
func (s *SteamService) DepositQuoteByPrice(ctx context.Context, req SteamDepositNetAmountQuery) (*SteamQuote, error) {
	return s.getQuote(ctx, "/v1/steam/deposit/netamount", req.toQuery())
}

// CreateDepositOrderByNetAmount creates a deposit-funded Steam order
// specifying the amount to credit (POST /v1/steam/deposit). This is a
// mutating request and is never retried automatically.
func (s *SteamService) CreateDepositOrderByNetAmount(ctx context.Context, req CreateSteamDepositOrderByNetAmountRequest) (*SteamDepositOrder, error) {
	var out SteamDepositOrder
	if err := s.c.do(ctx, productSteam, apiRequest{
		Method: http.MethodPost, Path: "/v1/steam/deposit", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateDepositOrderByPrice creates a deposit-funded Steam order specifying
// the price to pay (POST /v1/steam/deposit/by-price). This is a mutating
// request and is never retried automatically.
func (s *SteamService) CreateDepositOrderByPrice(ctx context.Context, req CreateSteamDepositOrderByPriceRequest) (*SteamDepositOrder, error) {
	var out SteamDepositOrder
	if err := s.c.do(ctx, productSteam, apiRequest{
		Method: http.MethodPost, Path: "/v1/steam/deposit/by-price", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetDepositOrder fetches a deposit-funded order's status by id (GET
// /v1/deposit/order/{orderId}). This endpoint is shared across
// Steam/Top-Up/voucher deposit orders; it is exposed on each service for
// convenience, using that service's token.
func (s *SteamService) GetDepositOrder(ctx context.Context, orderID string) (*DepositOrder, error) {
	return getDepositOrder(ctx, s.c, productSteam, orderID)
}

// DepositBalance fetches the merchant's digital-goods deposit balance (GET
// /v1/deposit/balance) using the Steam token.
func (s *SteamService) DepositBalance(ctx context.Context) (*DepositBalance, error) {
	return getDepositBalance(ctx, s.c, productSteam)
}

func (s *SteamService) getQuote(ctx context.Context, path string, q url.Values) (*SteamQuote, error) {
	var out SteamQuote
	if err := s.c.do(ctx, productSteam, apiRequest{Method: http.MethodGet, Path: path, Query: q}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

func (s *SteamService) createOrder(ctx context.Context, path string, body any) (*SteamOrder, error) {
	var out SteamOrder
	if err := s.c.do(ctx, productSteam, apiRequest{Method: http.MethodPost, Path: path, Body: body, Mutating: true}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// -----------------------------------------------------------------------
// Telegram Stars
// -----------------------------------------------------------------------

// StarsService groups the Telegram Stars endpoints. Every method requires
// Tokens.Stars — Stars always lives on a terminal of its own, separate from
// Steam, and calling it with any other product's token is rejected by the
// server, not just by this SDK.
type StarsService struct{ c *Client }

// Price fetches the current Stars price and minimum order size for a
// Telegram username (GET /stars/price).
func (s *StarsService) Price(ctx context.Context, username string) (*StarsPrice, error) {
	q := url.Values{}
	q.Set("username", username)
	var out StarsPrice
	if err := s.c.do(ctx, productStars, apiRequest{
		Method: http.MethodGet, Path: "/stars/price", Query: q,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateOrder creates a Stars order (POST /stars). Count must be between 50
// and 50000 per SPEC.md; the API enforces this, not the SDK. If the order
// amount exceeds the auto-approval threshold, the returned order comes back
// with Status == StarsOrderStatusReview and is NOT fulfilled until
// ConfirmOrder is called.
//
// This is a mutating request and is never retried automatically.
func (s *StarsService) CreateOrder(ctx context.Context, req CreateStarsOrderRequest) (*StarsOrder, error) {
	var out StarsOrder
	if err := s.c.do(ctx, productStars, apiRequest{
		Method: http.MethodPost, Path: "/stars", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetOrder fetches a Stars order's status by id (GET /stars/order/{id}).
func (s *StarsService) GetOrder(ctx context.Context, id string) (*StarsOrder, error) {
	var out StarsOrder
	if err := s.c.do(ctx, productStars, apiRequest{
		Method: http.MethodGet, Path: "/stars/order/" + url.PathEscape(id),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// ConfirmOrder confirms a Stars order that is in the Review status (POST
// /stars/order/{id}/confirm), moving it Review -> Paid so it proceeds to
// fulfillment. This is a mutating request and is never retried
// automatically.
func (s *StarsService) ConfirmOrder(ctx context.Context, id string) (*StarsOrder, error) {
	var out StarsOrder
	if err := s.c.do(ctx, productStars, apiRequest{
		Method: http.MethodPost, Path: "/stars/order/" + url.PathEscape(id) + "/confirm", Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// RejectOrder rejects a Stars order that is in the Review status (POST
// /stars/order/{id}/reject), moving it Review -> Refunded. This is a
// mutating request and is never retried automatically.
func (s *StarsService) RejectOrder(ctx context.Context, id string) (*StarsOrder, error) {
	var out StarsOrder
	if err := s.c.do(ctx, productStars, apiRequest{
		Method: http.MethodPost, Path: "/stars/order/" + url.PathEscape(id) + "/reject", Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// -----------------------------------------------------------------------
// Top-Up
// -----------------------------------------------------------------------

// TopUpService groups the top-up endpoints. Every method requires
// Tokens.TopUp.
type TopUpService struct{ c *Client }

// ListOffers fetches the acquiring-funded top-up catalog (GET
// /v3/topup/all). SPEC.md documents this endpoint's response as a single
// category, unlike ListDepositOffers.
func (s *TopUpService) ListOffers(ctx context.Context) (*TopUpCategory, error) {
	var out TopUpCategory
	if err := s.c.do(ctx, productTopUp, apiRequest{Method: http.MethodGet, Path: "/v3/topup/all"}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateOrder creates an acquiring-funded top-up order (POST /v3/topup).
// This is a mutating request and is never retried automatically.
func (s *TopUpService) CreateOrder(ctx context.Context, req CreateTopUpOrderRequest) (*TopUpOrder, error) {
	var out TopUpOrder
	if err := s.c.do(ctx, productTopUp, apiRequest{
		Method: http.MethodPost, Path: "/v3/topup", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetOrder fetches an acquiring-funded top-up order by id (GET
// /v3/topup/orders/{id}).
func (s *TopUpService) GetOrder(ctx context.Context, id string) (*TopUpOrder, error) {
	var out TopUpOrder
	if err := s.c.do(ctx, productTopUp, apiRequest{
		Method: http.MethodGet, Path: "/v3/topup/orders/" + url.PathEscape(id),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// ListDepositOffers fetches the deposit-funded top-up catalog (GET
// /v1/deposit/topups), grouped into categories.
func (s *TopUpService) ListDepositOffers(ctx context.Context) (*TopUpDepositCatalog, error) {
	var out TopUpDepositCatalog
	if err := s.c.do(ctx, productTopUp, apiRequest{Method: http.MethodGet, Path: "/v1/deposit/topups"}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateDepositOrder creates a deposit-funded top-up order (POST
// /v1/deposit/topups). This is a mutating request and is never retried
// automatically.
func (s *TopUpService) CreateDepositOrder(ctx context.Context, req CreateTopUpDepositOrderRequest) (*TopUpDepositOrder, error) {
	var out TopUpDepositOrder
	if err := s.c.do(ctx, productTopUp, apiRequest{
		Method: http.MethodPost, Path: "/v1/deposit/topups", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetDepositOrder fetches a deposit-funded order's status by id (GET
// /v1/deposit/order/{orderId}), using the TopUp token.
func (s *TopUpService) GetDepositOrder(ctx context.Context, orderID string) (*DepositOrder, error) {
	return getDepositOrder(ctx, s.c, productTopUp, orderID)
}

// DepositBalance fetches the merchant's digital-goods deposit balance (GET
// /v1/deposit/balance) using the TopUp token.
func (s *TopUpService) DepositBalance(ctx context.Context) (*DepositBalance, error) {
	return getDepositBalance(ctx, s.c, productTopUp)
}

// -----------------------------------------------------------------------
// Vouchers
// -----------------------------------------------------------------------

// VouchersService groups the voucher endpoints. Every method requires
// Tokens.Vouchers.
type VouchersService struct{ c *Client }

// ListOffers fetches the acquiring-funded voucher catalog (GET
// /v3/vouchers/all). SPEC.md documents this endpoint's response as a single
// category, unlike ListDepositOffers.
func (s *VouchersService) ListOffers(ctx context.Context) (*VoucherCategory, error) {
	var out VoucherCategory
	if err := s.c.do(ctx, productVouchers, apiRequest{Method: http.MethodGet, Path: "/v3/vouchers/all"}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateOrder creates an acquiring-funded voucher order (POST /v3/vouchers).
// This is a mutating request and is never retried automatically. Voucher
// codes are not returned here — poll GetOrder; SPEC.md notes codes can
// appear with up to a 10-minute delay after payment.
func (s *VouchersService) CreateOrder(ctx context.Context, req CreateVoucherOrderRequest) (*VoucherOrder, error) {
	var out VoucherOrder
	if err := s.c.do(ctx, productVouchers, apiRequest{
		Method: http.MethodPost, Path: "/v3/vouchers", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetOrder fetches an acquiring-funded voucher order by id, including its
// codes once issued (GET /v3/vouchers/order/{id}).
func (s *VouchersService) GetOrder(ctx context.Context, id string) (*VoucherOrder, error) {
	var out VoucherOrder
	if err := s.c.do(ctx, productVouchers, apiRequest{
		Method: http.MethodGet, Path: "/v3/vouchers/order/" + url.PathEscape(id),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// ListDepositOffers fetches the deposit-funded voucher catalog (GET
// /v1/deposit/vouchers), grouped into categories.
func (s *VouchersService) ListDepositOffers(ctx context.Context) (*VoucherDepositCatalog, error) {
	var out VoucherDepositCatalog
	if err := s.c.do(ctx, productVouchers, apiRequest{Method: http.MethodGet, Path: "/v1/deposit/vouchers"}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateDepositOrder creates a deposit-funded voucher order (POST
// /v1/deposit/vouchers). This is a mutating request and is never retried
// automatically.
func (s *VouchersService) CreateDepositOrder(ctx context.Context, req CreateVoucherDepositOrderRequest) (*VoucherDepositOrder, error) {
	var out VoucherDepositOrder
	if err := s.c.do(ctx, productVouchers, apiRequest{
		Method: http.MethodPost, Path: "/v1/deposit/vouchers", Body: req, Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetDepositOrder fetches a deposit-funded order's status by id (GET
// /v1/deposit/order/{orderId}), using the Vouchers token.
func (s *VouchersService) GetDepositOrder(ctx context.Context, orderID string) (*DepositOrder, error) {
	return getDepositOrder(ctx, s.c, productVouchers, orderID)
}

// DepositBalance fetches the merchant's digital-goods deposit balance (GET
// /v1/deposit/balance) using the Vouchers token.
func (s *VouchersService) DepositBalance(ctx context.Context) (*DepositBalance, error) {
	return getDepositBalance(ctx, s.c, productVouchers)
}

// -----------------------------------------------------------------------
// shared helpers
// -----------------------------------------------------------------------

func getDepositOrder(ctx context.Context, c *Client, p product, orderID string) (*DepositOrder, error) {
	var out DepositOrder
	if err := c.do(ctx, p, apiRequest{
		Method: http.MethodGet, Path: "/v1/deposit/order/" + url.PathEscape(orderID),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

func getDepositBalance(ctx context.Context, c *Client, p product) (*DepositBalance, error) {
	var out DepositBalance
	if err := c.do(ctx, p, apiRequest{Method: http.MethodGet, Path: "/v1/deposit/balance"}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}
