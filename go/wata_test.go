package wata

import (
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha512"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"
)

// -----------------------------------------------------------------------
// 1. Missing product token -> ConfigError, before any network call.
// -----------------------------------------------------------------------

func TestMissingToken_ReturnsConfigErrorWithoutNetworkCall(t *testing.T) {
	called := false
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		called = true
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	c := New(Tokens{}, withBaseURLOverride(productAcquiring, srv.URL))

	_, err := c.Acquiring.CreateLink(context.Background(), CreateLinkRequest{Amount: 100, Currency: CurrencyRUB})
	if err == nil {
		t.Fatal("expected an error when Tokens.Acquiring is empty")
	}
	var cfgErr *ConfigError
	if !errors.As(err, &cfgErr) {
		t.Fatalf("expected *ConfigError, got %T: %v", err, err)
	}
	if called {
		t.Fatal("expected no network call to be made when the token is missing")
	}
}

func TestMissingToken_EachProductNamesItsOwnToken(t *testing.T) {
	c := New(Tokens{}) // no tokens at all

	cases := []struct {
		name string
		call func() error
	}{
		{"acquiring", func() error {
			_, err := c.Acquiring.GetLink(context.Background(), "x")
			return err
		}},
		{"stars", func() error {
			_, err := c.Stars.GetOrder(context.Background(), "x")
			return err
		}},
		{"steam", func() error {
			_, err := c.Steam.GetOrder(context.Background(), "x")
			return err
		}},
		{"topup", func() error {
			_, err := c.TopUp.GetOrder(context.Background(), "x")
			return err
		}},
		{"vouchers", func() error {
			_, err := c.Vouchers.GetOrder(context.Background(), "x")
			return err
		}},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			err := tc.call()
			var cfgErr *ConfigError
			if !errors.As(err, &cfgErr) {
				t.Fatalf("expected *ConfigError, got %T: %v", err, err)
			}
		})
	}
}

// A terminal configured only for Steam must not be usable for Stars, and
// vice versa: this is the platform's core access-control property, not a
// nice-to-have.
func TestSteamTokenDoesNotUnlockStars(t *testing.T) {
	c := New(Tokens{Steam: "steam-token-only"})
	_, err := c.Stars.GetOrder(context.Background(), "x")
	var cfgErr *ConfigError
	if !errors.As(err, &cfgErr) {
		t.Fatalf("expected *ConfigError when calling Stars with only a Steam token, got %T: %v", err, err)
	}
}

// -----------------------------------------------------------------------
// 2. Webhook signature verification: success and forgery.
// -----------------------------------------------------------------------

func generateTestKey(t *testing.T) (*rsa.PrivateKey, string) {
	t.Helper()
	priv, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("generate key: %v", err)
	}
	der, err := x509.MarshalPKIXPublicKey(&priv.PublicKey)
	if err != nil {
		t.Fatalf("marshal public key: %v", err)
	}
	pemBytes := pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: der})
	return priv, string(pemBytes)
}

func sign(t *testing.T, priv *rsa.PrivateKey, body []byte) string {
	t.Helper()
	hash := sha512.Sum512(body)
	sig, err := rsa.SignPKCS1v15(rand.Reader, priv, crypto.SHA512, hash[:])
	if err != nil {
		t.Fatalf("sign: %v", err)
	}
	return base64.StdEncoding.EncodeToString(sig)
}

func TestVerifyWebhook_ValidSignature(t *testing.T) {
	priv, pubPEM := generateTestKey(t)
	body := []byte(`{"id":"evt_1","transactionStatus":"Paid"}`)
	sig := sign(t, priv, body)

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/h2h/public-key" {
			t.Fatalf("unexpected path %s", r.URL.Path)
		}
		if r.Header.Get("Authorization") != "" {
			t.Fatal("public-key endpoint must be called without an Authorization header")
		}
		json.NewEncoder(w).Encode(map[string]string{"value": pubPEM})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "unused"}, withBaseURLOverride(productAcquiring, srv.URL))

	ok, err := c.VerifyWebhook(context.Background(), body, sig)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !ok {
		t.Fatal("expected signature to verify")
	}
}

func TestVerifyWebhook_ForgedSignatureFails(t *testing.T) {
	priv, pubPEM := generateTestKey(t)
	body := []byte(`{"id":"evt_1","transactionStatus":"Paid"}`)
	sig := sign(t, priv, body)

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		json.NewEncoder(w).Encode(map[string]string{"value": pubPEM})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "unused"}, withBaseURLOverride(productAcquiring, srv.URL))

	tampered := []byte(`{"id":"evt_1","transactionStatus":"Declined"}`)
	ok, err := c.VerifyWebhook(context.Background(), tampered, sig)
	if err != nil {
		t.Fatalf("a mismatched signature should not itself be an error, got: %v", err)
	}
	if ok {
		t.Fatal("expected a tampered body to fail verification")
	}
}

func TestVerifyWebhook_KeyCachedPerClient(t *testing.T) {
	priv, pubPEM := generateTestKey(t)
	body := []byte(`{"id":"evt_1"}`)
	sig := sign(t, priv, body)

	fetches := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fetches++
		json.NewEncoder(w).Encode(map[string]string{"value": pubPEM})
	}))
	defer srv.Close()

	c := New(Tokens{}, withBaseURLOverride(productAcquiring, srv.URL))

	for i := 0; i < 3; i++ {
		ok, err := c.VerifyWebhook(context.Background(), body, sig)
		if err != nil || !ok {
			t.Fatalf("iteration %d: ok=%v err=%v", i, ok, err)
		}
	}
	if fetches != 1 {
		t.Fatalf("expected the public key to be fetched once and cached, got %d fetches", fetches)
	}
}

func TestVerifyWebhookWithKey_ExplicitKeyBypassesNetwork(t *testing.T) {
	priv, pubPEM := generateTestKey(t)
	body := []byte(`{"id":"evt_1"}`)
	sig := sign(t, priv, body)

	ok, err := VerifyWebhookWithKey(body, sig, pubPEM)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !ok {
		t.Fatal("expected signature to verify")
	}
}

func TestParseWebhook(t *testing.T) {
	body := []byte(`{"id":"evt_1","transactionStatus":"Paid","amount":100.5,"currency":"RUB","commission":1.5}`)
	ev, err := ParseWebhook(body)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if ev.ID != "evt_1" || ev.TransactionStatus != TransactionStatusPaid || ev.Amount != 100.5 || ev.Currency != CurrencyRUB {
		t.Fatalf("unexpected parsed event: %+v", ev)
	}
	if string(ev.Raw) != string(body) {
		t.Fatalf("expected Raw to capture the exact payload")
	}
}

// -----------------------------------------------------------------------
// 3. Cursor pagination across two pages.
// -----------------------------------------------------------------------

func TestTransactionIterator_PagesThroughCursor(t *testing.T) {
	page1 := TransactionPage{
		Items:        []Transaction{{ID: "tx1"}, {ID: "tx2"}},
		HasNextPage:  true,
		NextCursorID: "tx2",
	}
	nextAmount := 42.0
	nextDate := time.Date(2026, 1, 2, 0, 0, 0, 0, time.UTC)
	page1.NextCursorAmount = &nextAmount
	page1.NextCursorDate = &nextDate

	page2 := TransactionPage{
		Items:       []Transaction{{ID: "tx3"}},
		HasNextPage: false,
	}

	var gotSecondCallCursor url.Values
	requestNum := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requestNum++
		w.Header().Set("Content-Type", "application/json")
		if requestNum == 1 {
			if r.URL.Query().Get("CursorId") != "" {
				t.Fatal("first request should not carry a cursor")
			}
			json.NewEncoder(w).Encode(page1)
			return
		}
		gotSecondCallCursor = r.URL.Query()
		json.NewEncoder(w).Encode(page2)
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))

	it := c.Acquiring.Transactions(context.Background(), SearchTransactionsParams{})
	var ids []string
	for it.Next() {
		ids = append(ids, it.Transaction().ID)
	}
	if err := it.Err(); err != nil {
		t.Fatalf("unexpected iterator error: %v", err)
	}
	if fmt.Sprint(ids) != fmt.Sprint([]string{"tx1", "tx2", "tx3"}) {
		t.Fatalf("unexpected ids: %v", ids)
	}
	if requestNum != 2 {
		t.Fatalf("expected exactly 2 page requests, got %d", requestNum)
	}
	if gotSecondCallCursor.Get("CursorId") != "tx2" {
		t.Fatalf("expected second request to carry CursorId=tx2, got %q", gotSecondCallCursor.Get("CursorId"))
	}
	if gotSecondCallCursor.Get("CursorAmount") == "" {
		t.Fatal("expected second request to carry CursorAmount")
	}
	if gotSecondCallCursor.Get("CursorDate") == "" {
		t.Fatal("expected second request to carry CursorDate — omitting it is exactly the bug SPEC.md warns about")
	}
}

// -----------------------------------------------------------------------
// 4. Parsing an API error with a WATA error code.
// -----------------------------------------------------------------------

func TestAPIError_ParsesWataErrorCode(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]any{
			"error": map[string]any{
				"code":    "TRA_001",
				"message": "исходная транзакция не найдена",
				"validationErrors": []map[string]string{
					{"property": "amount", "message": "must be positive"},
				},
			},
		})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))
	_, err := c.Acquiring.Refund(context.Background(), RefundRequest{OriginalTransactionID: "x", Amount: 1})

	var apiErr *APIError
	if !errors.As(err, &apiErr) {
		t.Fatalf("expected *APIError, got %T: %v", err, err)
	}
	if apiErr.Err.Code != "TRA_001" {
		t.Fatalf("expected code TRA_001, got %q", apiErr.Err.Code)
	}
	if apiErr.Err.HTTPStatus != http.StatusBadRequest {
		t.Fatalf("expected HTTP 400, got %d", apiErr.Err.HTTPStatus)
	}
	if len(apiErr.ValidationErrors) != 1 || apiErr.ValidationErrors[0].Property != "amount" {
		t.Fatalf("expected validation errors to be parsed, got %+v", apiErr.ValidationErrors)
	}
}

func TestAuthError_On401(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]any{"error": map[string]string{"code": "AUTH_001", "message": "token revoked"}})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))
	_, err := c.Acquiring.GetLink(context.Background(), "id")

	var authErr *AuthError
	if !errors.As(err, &authErr) {
		t.Fatalf("expected *AuthError, got %T: %v", err, err)
	}
}

func TestRateLimitError_On429WithRetryAfter(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Retry-After", "30")
		w.WriteHeader(http.StatusTooManyRequests)
		json.NewEncoder(w).Encode(map[string]any{"error": map[string]string{"message": "slow down"}})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))
	_, err := c.Acquiring.GetLink(context.Background(), "id")

	var rlErr *RateLimitError
	if !errors.As(err, &rlErr) {
		t.Fatalf("expected *RateLimitError, got %T: %v", err, err)
	}
	if rlErr.RetryAfter == nil || *rlErr.RetryAfter != 30*time.Second {
		t.Fatalf("expected RetryAfter=30s, got %v", rlErr.RetryAfter)
	}
}

// -----------------------------------------------------------------------
// 5. Mutating requests are never retried, even on 5xx / network failure.
// -----------------------------------------------------------------------

func TestMutatingRequest_NotRetriedOn5xx(t *testing.T) {
	attempts := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": map[string]string{"message": "boom"}})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL), WithMaxRetries(5))
	_, err := c.Acquiring.CreateLink(context.Background(), CreateLinkRequest{Amount: 10, Currency: CurrencyRUB})

	var srvErr *ServerError
	if !errors.As(err, &srvErr) {
		t.Fatalf("expected *ServerError, got %T: %v", err, err)
	}
	if attempts != 1 {
		t.Fatalf("expected exactly 1 attempt for a mutating request, got %d", attempts)
	}
}

func TestGetRequest_RetriesOn5xxThenSucceeds(t *testing.T) {
	attempts := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		if attempts < 3 {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]any{"error": map[string]string{"message": "boom"}})
			return
		}
		json.NewEncoder(w).Encode(PaymentLink{ID: "link_1"})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL), WithMaxRetries(5))
	link, err := c.Acquiring.GetLink(context.Background(), "id")
	if err != nil {
		t.Fatalf("expected eventual success, got error: %v", err)
	}
	if link.ID != "link_1" {
		t.Fatalf("unexpected link: %+v", link)
	}
	if attempts != 3 {
		t.Fatalf("expected 3 attempts, got %d", attempts)
	}
}

func TestGetRequest_NotRetriedOn4xx(t *testing.T) {
	attempts := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]any{"error": map[string]string{"code": "PL_404", "message": "not found"}})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL), WithMaxRetries(5))
	_, err := c.Acquiring.GetLink(context.Background(), "id")
	if err == nil {
		t.Fatal("expected an error")
	}
	if attempts != 1 {
		t.Fatalf("expected exactly 1 attempt for a non-retryable 4xx, got %d", attempts)
	}
}

// -----------------------------------------------------------------------
// 6. Balance date validation.
// -----------------------------------------------------------------------

func TestBalance_RejectsDateOtherThanTodayOrYesterday(t *testing.T) {
	called := false
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		called = true
		json.NewEncoder(w).Encode(BalanceResponse{})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))

	tooOld := time.Now().UTC().AddDate(0, 0, -5)
	_, err := c.Acquiring.Balance(context.Background(), tooOld)
	var cfgErr *ConfigError
	if !errors.As(err, &cfgErr) {
		t.Fatalf("expected *ConfigError for an out-of-range date, got %T: %v", err, err)
	}
	if called {
		t.Fatal("expected no network call for an invalid balance date")
	}
}

func TestBalance_AcceptsTodayAndYesterday(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		json.NewEncoder(w).Encode(BalanceResponse{TerminalPublicID: "term_1", Balance: 100, Currency: CurrencyRUB})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))

	for _, d := range []time.Time{time.Now().UTC(), time.Now().UTC().AddDate(0, 0, -1)} {
		if _, err := c.Acquiring.Balance(context.Background(), d); err != nil {
			t.Fatalf("expected date %v to be accepted, got error: %v", d, err)
		}
	}
}

// -----------------------------------------------------------------------
// Extra: sandbox is refused for digital goods, unknown enum values survive
// parsing, and Content-Type/omitempty behavior.
// -----------------------------------------------------------------------

func TestSandbox_RefusedForDigitalGoods(t *testing.T) {
	c := New(Tokens{Steam: "t"}, WithEnvironment(Sandbox))
	_, err := c.Steam.GetOrder(context.Background(), "id")
	var cfgErr *ConfigError
	if !errors.As(err, &cfgErr) {
		t.Fatalf("expected *ConfigError for sandbox + digital goods, got %T: %v", err, err)
	}
}

func TestSandbox_WorksForAcquiring(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		json.NewEncoder(w).Encode(PaymentLink{ID: "link_1"})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, WithEnvironment(Sandbox), withBaseURLOverride(productAcquiring, srv.URL))
	if _, err := c.Acquiring.GetLink(context.Background(), "id"); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestUnknownEnumValue_DoesNotFailParsing(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		json.NewEncoder(w).Encode(map[string]any{
			"id":       "tx1",
			"status":   "SomeBrandNewStatus",
			"kind":     "Payment",
			"currency": "XYZ",
		})
	}))
	defer srv.Close()

	c := New(Tokens{Acquiring: "t"}, withBaseURLOverride(productAcquiring, srv.URL))
	tx, err := c.Acquiring.GetTransaction(context.Background(), "tx1")
	if err != nil {
		t.Fatalf("unexpected error parsing an unknown enum value: %v", err)
	}
	if tx.Status != "SomeBrandNewStatus" {
		t.Fatalf("expected the unknown status string to be preserved, got %q", tx.Status)
	}
	if tx.Currency != "XYZ" {
		t.Fatalf("expected the unknown currency string to be preserved, got %q", tx.Currency)
	}
}
