package wata

import (
	"context"
	"fmt"
	"net/http"
	"net/url"
	"time"
)

// AcquiringService groups the H2H acquiring endpoints (base path
// /api/h2h): payment links, transactions, refunds, balance and direct
// payments. Every method requires Tokens.Acquiring to have been set on the
// Client; otherwise it returns a *ConfigError before making any request.
type AcquiringService struct {
	c *Client
}

// CreateLink creates a payment link (POST /api/h2h/links). This is a
// mutating request and is never retried automatically, even on a network
// error or 5xx: a blind retry could create a second link.
func (s *AcquiringService) CreateLink(ctx context.Context, req CreateLinkRequest) (*PaymentLink, error) {
	var out PaymentLink
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method:   http.MethodPost,
		Path:     "/links",
		Body:     req,
		Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// SearchLinks searches payment links (GET /api/h2h/links). This endpoint
// uses simple skip/take pagination, not the cursor pagination transactions
// use — see SearchLinksParams.
func (s *AcquiringService) SearchLinks(ctx context.Context, params SearchLinksParams) (*LinkPage, error) {
	var out LinkPage
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method: http.MethodGet,
		Path:   "/links",
		Query:  params.toQuery(),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// GetLink fetches a single payment link by id (GET /api/h2h/links/{id}).
func (s *AcquiringService) GetLink(ctx context.Context, id string) (*PaymentLink, error) {
	var out PaymentLink
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method: http.MethodGet,
		Path:   "/links/" + url.PathEscape(id),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// SearchTransactions fetches one page of transactions (GET
// /api/h2h/v2/transactions). For iterating every matching transaction
// across pages, prefer Transactions, which threads the cursor fields for
// you — manual paging with this method requires copying NextCursorID,
// NextCursorDate and NextCursorAmount into the next call's
// SearchTransactionsParams yourself, and forgetting CursorDate silently
// breaks the second page (SPEC.md section 4).
func (s *AcquiringService) SearchTransactions(ctx context.Context, params SearchTransactionsParams) (*TransactionPage, error) {
	var out TransactionPage
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method: http.MethodGet,
		Path:   "/v2/transactions",
		Query:  params.toQuery(),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// Transactions returns an iterator over every transaction matching params,
// fetching additional pages as needed and threading CursorID, CursorAmount
// and CursorDate automatically. See TransactionIterator.
func (s *AcquiringService) Transactions(ctx context.Context, params SearchTransactionsParams) *TransactionIterator {
	return newTransactionIterator(ctx, s, params)
}

// GetTransaction fetches a single transaction by id (GET
// /api/h2h/transactions/{id}).
func (s *AcquiringService) GetTransaction(ctx context.Context, id string) (*Transaction, error) {
	var out Transaction
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method: http.MethodGet,
		Path:   "/transactions/" + url.PathEscape(id),
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// Refund issues a refund (POST /api/h2h/transactions/refunds). There is no
// "reason" field in the WATA API; the currency is taken from the original
// transaction. This is a mutating request and is never retried
// automatically. Refunds are unavailable on terminals combining digital
// goods and acquiring, and the original transaction must be in the Paid
// status — WATA enforces both server-side; this SDK does not duplicate
// that validation.
func (s *AcquiringService) Refund(ctx context.Context, req RefundRequest) (*RefundResponse, error) {
	var out RefundResponse
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method:   http.MethodPost,
		Path:     "/transactions/refunds",
		Body:     req,
		Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// Balance fetches the terminal balance for a given date (GET
// /api/h2h/finance/balance). SPEC.md requires the date to be today or
// yesterday in UTC; this method validates that locally and returns a
// *ConfigError before making any network call if it is not, rather than
// letting the server reject it.
func (s *AcquiringService) Balance(ctx context.Context, date time.Time) (*BalanceResponse, error) {
	if err := validateBalanceDate(date); err != nil {
		return nil, err
	}
	q := url.Values{}
	q.Set("Date", date.UTC().Format("2006-01-02"))

	var out BalanceResponse
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method: http.MethodGet,
		Path:   "/finance/balance",
		Query:  q,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

func validateBalanceDate(date time.Time) error {
	today := time.Now().UTC().Truncate(24 * time.Hour)
	yesterday := today.AddDate(0, 0, -1)
	d := date.UTC().Truncate(24 * time.Hour)
	if !d.Equal(today) && !d.Equal(yesterday) {
		return newConfigError(fmt.Sprintf(
			"недопустимая дата баланса %s: разрешены только сегодняшняя (%s) и вчерашняя (%s) даты по UTC",
			d.Format("2006-01-02"), today.Format("2006-01-02"), yesterday.Format("2006-01-02"),
		))
	}
	return nil
}

// PayCardCrypto submits a card-crypto direct payment (POST
// /api/h2h/payments/card-crypto). CardCrypto in the request must come from
// WATA's client-side checkout script running in the payer's browser — the
// merchant server does not (and cannot) construct it. This is a mutating
// request and is never retried automatically.
func (s *AcquiringService) PayCardCrypto(ctx context.Context, req CardCryptoRequest) (*CardCryptoResponse, error) {
	var out CardCryptoResponse
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method:   http.MethodPost,
		Path:     "/payments/card-crypto",
		Body:     req,
		Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// PaySBP submits an SBP direct payment (POST /api/h2h/payments/sbp). SBP
// has no currency field — payments are always RUB. This is a mutating
// request and is never retried automatically.
func (s *AcquiringService) PaySBP(ctx context.Context, req SbpRequest) (*SbpResponse, error) {
	var out SbpResponse
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method:   http.MethodPost,
		Path:     "/payments/sbp",
		Body:     req,
		Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// PayTPay submits a T-Pay direct payment (POST /api/h2h/payments/tpay).
// This is a mutating request and is never retried automatically.
func (s *AcquiringService) PayTPay(ctx context.Context, req TPayRequest) (*TPayResponse, error) {
	var out TPayResponse
	if err := s.c.do(ctx, productAcquiring, apiRequest{
		Method:   http.MethodPost,
		Path:     "/payments/tpay",
		Body:     req,
		Mutating: true,
	}, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

// PublicKey fetches the current webhook public key in PEM form (GET
// /api/h2h/public-key, called without an Authorization header — the one
// endpoint in the API that requires none). Most callers should use
// Client.VerifyWebhook instead, which fetches and caches this
// automatically.
func (s *AcquiringService) PublicKey(ctx context.Context) (string, error) {
	return s.c.fetchPublicKeyPEM(ctx)
}
