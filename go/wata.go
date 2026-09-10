// Package wata is an unofficial Go SDK for the WATA payment platform
// (acquiring / H2H and digital goods: Telegram Stars, Steam, Top-Up,
// vouchers). It depends only on the Go standard library.
//
// A WATA token is issued per terminal, not per merchant account, and the
// terminal is never sent in a request — the server infers it from the
// token. Telegram Stars and Steam always live on separate terminals of
// their own, which means separate tokens: calling Stars with a Steam token
// is rejected by the server. Client is built around this: it holds an
// independent set of tokens, one per product, and refuses to call a
// product whose token was not supplied — see Tokens and New.
package wata

import (
	"context"
	"crypto/rsa"
	"fmt"
	"net/http"
	"sync"
	"time"
)

// SDKVersion is the version of this SDK, also used as the default
// User-Agent suffix.
const SDKVersion = "0.1.0"

// Environment selects between WATA's production and sandbox endpoints.
type Environment string

const (
	// Production is the default environment.
	Production Environment = "production"
	// Sandbox uses api-sandbox.wata.pro for acquiring. Digital goods (Stars,
	// Steam, Top-Up, vouchers) have no documented sandbox: requesting
	// Sandbox and then calling a digital-goods method returns a ConfigError
	// instead of silently falling back to production.
	Sandbox Environment = "sandbox"
)

// Tokens holds one JWT per WATA product. All fields are optional — build a
// Client with any subset of them. Calling a product whose token is empty
// returns a *ConfigError before any network call is made, naming the
// missing token.
//
// Tokens are never shared between products by this SDK. If a single
// merchant terminal happens to combine acquiring and digital goods, the
// caller may deliberately pass the same token in multiple fields — that is
// the caller's informed choice, not something the SDK infers.
type Tokens struct {
	Acquiring string
	Stars     string
	Steam     string
	TopUp     string
	Vouchers  string
}

// product identifies which WATA product a request targets. It is used
// internally to select the token, base URL and path prefix for a request.
type product int

const (
	productAcquiring product = iota
	productStars
	productSteam
	productTopUp
	productVouchers
)

func (p product) tokenFieldName() string {
	switch p {
	case productAcquiring:
		return "Acquiring"
	case productStars:
		return "Stars"
	case productSteam:
		return "Steam"
	case productTopUp:
		return "TopUp"
	case productVouchers:
		return "Vouchers"
	default:
		return "?"
	}
}

func (p product) pathPrefix() string {
	if p == productAcquiring {
		return "/api/h2h"
	}
	return "/api"
}

const (
	acquiringProductionBaseURL = "https://api.wata.pro"
	acquiringSandboxBaseURL    = "https://api-sandbox.wata.pro"
	digitalGoodsBaseURL        = "https://dg-api.wata.pro"
)

// Client is a WATA API client. Build one with New. A Client is safe for
// concurrent use by multiple goroutines.
type Client struct {
	tokens      Tokens
	environment Environment
	httpClient  *http.Client
	timeout     time.Duration
	maxRetries  int

	// baseURLOverrides lets tests point a product at an httptest.Server
	// instead of the real WATA hosts. Not exported: production callers
	// configure the environment, not raw URLs, so the client can keep the
	// acquiring/digital-goods URL split and the sandbox-availability check
	// correct on their behalf.
	baseURLOverrides map[product]string

	keyMu     sync.Mutex
	cachedKey *rsa.PublicKey

	// Acquiring exposes the H2H acquiring API: payment links, transactions,
	// refunds, balance and direct payments. Requires Tokens.Acquiring.
	Acquiring *AcquiringService
	// Stars exposes the Telegram Stars API. Requires Tokens.Stars — this is
	// always a different terminal (and therefore a different token) than
	// Steam, even if the same merchant owns both.
	Stars *StarsService
	// Steam exposes the Steam top-up API. Requires Tokens.Steam.
	Steam *SteamService
	// TopUp exposes the top-up (mobile/services) API. Requires Tokens.TopUp.
	TopUp *TopUpService
	// Vouchers exposes the voucher API. Requires Tokens.Vouchers.
	Vouchers *VouchersService
}

// Option configures a Client constructed by New.
type Option func(*Client)

// WithHTTPClient overrides the *http.Client used for all requests. Useful
// for custom transports (proxies, TLS config, instrumentation). Do not set
// http.Client.Timeout on it if you also want WithTimeout's per-request
// timeout to apply cleanly — the SDK applies its own timeout via context
// per request.
func WithHTTPClient(h *http.Client) Option {
	return func(c *Client) {
		if h != nil {
			c.httpClient = h
		}
	}
}

// WithTimeout sets the per-request timeout. SPEC.md documents the API's own
// response timeout as 60 seconds; that is also this SDK's default.
func WithTimeout(d time.Duration) Option {
	return func(c *Client) {
		if d > 0 {
			c.timeout = d
		}
	}
}

// WithEnvironment selects Production (default) or Sandbox.
func WithEnvironment(e Environment) Option {
	return func(c *Client) {
		c.environment = e
	}
}

// WithMaxRetries overrides the number of attempts made for retryable
// requests (idempotent GETs on network errors or 5xx responses). The
// default is 3. Set to 1 to disable retries entirely. This never affects
// mutating requests (creating a link, payment or refund), which are never
// retried automatically regardless of this setting — see the Mutating
// field of internal request specs and SPEC.md section 3.
func WithMaxRetries(n int) Option {
	return func(c *Client) {
		if n > 0 {
			c.maxRetries = n
		}
	}
}

// withBaseURLOverride is unexported: it exists only so this package's own
// tests can redirect a product at an httptest.Server. SDK consumers select
// an Environment instead.
func withBaseURLOverride(p product, url string) Option {
	return func(c *Client) {
		if c.baseURLOverrides == nil {
			c.baseURLOverrides = make(map[product]string)
		}
		c.baseURLOverrides[p] = url
	}
}

// New builds a Client from a set of per-product tokens and options.
func New(tokens Tokens, opts ...Option) *Client {
	c := &Client{
		tokens:      tokens,
		environment: Production,
		httpClient:  &http.Client{},
		timeout:     60 * time.Second,
		maxRetries:  3,
	}
	for _, opt := range opts {
		opt(c)
	}
	c.Acquiring = &AcquiringService{c: c}
	c.Stars = &StarsService{c: c}
	c.Steam = &SteamService{c: c}
	c.TopUp = &TopUpService{c: c}
	c.Vouchers = &VouchersService{c: c}
	return c
}

// tokenFor returns the token configured for p, or a *ConfigError naming the
// missing token if none was configured.
func (c *Client) tokenFor(p product) (string, error) {
	var t string
	switch p {
	case productAcquiring:
		t = c.tokens.Acquiring
	case productStars:
		t = c.tokens.Stars
	case productSteam:
		t = c.tokens.Steam
	case productTopUp:
		t = c.tokens.TopUp
	case productVouchers:
		t = c.tokens.Vouchers
	}
	if t == "" {
		name := p.tokenFieldName()
		return "", newConfigError(fmt.Sprintf(
			"токен продукта %q не задан: передайте Tokens{%s: \"...\"} при вызове wata.New — "+
				"обращение к этому продукту невозможно без собственного токена терминала",
			name, name,
		))
	}
	return t, nil
}

// baseURL resolves the base host for p, honoring environment and any test
// override. It returns a *ConfigError, before any network call, if the
// caller asked for Sandbox on a digital-goods product: WATA does not
// document a sandbox for digital goods, and silently falling back to
// production would be worse than refusing.
func (c *Client) baseURL(p product) (string, error) {
	if override, ok := c.baseURLOverrides[p]; ok {
		return override, nil
	}
	if p == productAcquiring {
		if c.environment == Sandbox {
			return acquiringSandboxBaseURL, nil
		}
		return acquiringProductionBaseURL, nil
	}
	if c.environment == Sandbox {
		return "", newConfigError(fmt.Sprintf(
			"песочница недоступна для продукта %q: WATA не документирует sandbox-контур для цифровых товаров "+
				"(Stars/Steam/TopUp/Vouchers) — используйте wata.Production для этого клиента, "+
				"либо отдельный Client с Environment=Sandbox только для Acquiring",
			p.tokenFieldName(),
		))
	}
	return digitalGoodsBaseURL, nil
}

// GetPublicKeyPEM fetches the current webhook public key (GET
// /api/h2h/public-key) directly, bypassing the cache used internally by
// VerifyWebhook. Most callers should use VerifyWebhook instead, which
// fetches and caches this automatically, scoped to the client's
// Environment. This is the one endpoint in the whole API called without an
// Authorization header, so it works even on a Client with no
// Tokens.Acquiring configured.
func (c *Client) GetPublicKeyPEM(ctx context.Context) (string, error) {
	return c.fetchPublicKeyPEM(ctx)
}
