package wata

import (
	"fmt"
	"strings"
	"time"
)

// Error is the common shape carried by every error the SDK returns. It is
// not meant to be constructed directly by callers — match one of the
// concrete types below (ConfigError, AuthError, RateLimitError, APIError,
// ServerError, NetworkError, WebhookError) with errors.As instead. Every one
// of them wraps an *Error and forwards Unwrap to it, so errors.As(err,
// &target) also works against a plain *Error for code that only cares about
// the common fields regardless of category.
type Error struct {
	// Message is a human readable description. Never contains secrets
	// (tokens, card data): callers can log it safely.
	Message string
	// HTTPStatus is the HTTP status code returned by the server, or 0 for
	// errors detected locally before any request was sent (ConfigError) or
	// for network failures that never produced a response.
	HTTPStatus int
	// Path is the request path that produced the error (e.g. "/api/h2h/links"),
	// empty for errors detected before a path was resolved.
	Path string
	// Code is the WATA error code from the response body (PL_*, TRA_*, ORD_*,
	// STM_*, STR_*, TPP_*, VCR_*), when the server provided one.
	Code string
}

func (e *Error) Error() string {
	switch {
	case e == nil:
		return "wata: <nil>"
	case e.Code != "" && e.HTTPStatus != 0:
		return fmt.Sprintf("wata: %s (status=%d, code=%s, path=%s)", e.Message, e.HTTPStatus, e.Code, e.Path)
	case e.HTTPStatus != 0:
		return fmt.Sprintf("wata: %s (status=%d, path=%s)", e.Message, e.HTTPStatus, e.Path)
	default:
		return fmt.Sprintf("wata: %s", e.Message)
	}
}

// ConfigError signals a problem the SDK detected locally, before making any
// network call: a missing product token, an unsupported sandbox request, or
// an invalid balance date. Fix the call site rather than retrying.
type ConfigError struct{ Err *Error }

func (e *ConfigError) Error() string { return e.Err.Error() }
func (e *ConfigError) Unwrap() error { return e.Err }

func newConfigError(msg string) *ConfigError {
	return &ConfigError{Err: &Error{Message: msg}}
}

// AuthError corresponds to HTTP 401/403: the token expired, was revoked,
// belongs to a different terminal, or the request came from an unexpected IP.
type AuthError struct{ Err *Error }

func (e *AuthError) Error() string { return e.Err.Error() }
func (e *AuthError) Unwrap() error { return e.Err }

// RateLimitError corresponds to HTTP 429. RetryAfter is populated when the
// server sent a Retry-After header; the SDK never retries a 429 on its own.
type RateLimitError struct {
	Err        *Error
	RetryAfter *time.Duration
}

func (e *RateLimitError) Error() string { return e.Err.Error() }
func (e *RateLimitError) Unwrap() error { return e.Err }

// ValidationError describes one field-level validation failure reported by
// the WATA API alongside a 4xx response.
type ValidationError struct {
	Property string `json:"property,omitempty"`
	Message  string `json:"message,omitempty"`
}

// APIError corresponds to a 4xx response other than 401/403/429. Details and
// ValidationErrors mirror the `error.details` / `error.validationErrors`
// fields of the response body, when present.
type APIError struct {
	Err              *Error
	Details          any
	ValidationErrors []ValidationError
}

func (e *APIError) Error() string { return e.Err.Error() }
func (e *APIError) Unwrap() error { return e.Err }

// ServerError corresponds to a 5xx response received after retries (if any)
// were exhausted.
type ServerError struct{ Err *Error }

func (e *ServerError) Error() string { return e.Err.Error() }
func (e *ServerError) Unwrap() error { return e.Err }

// NetworkError wraps a transport-level failure: timeout, connection refused,
// DNS failure, etc. Cause holds the underlying error so callers can use
// errors.Is against, e.g., context.DeadlineExceeded.
type NetworkError struct {
	Err   *Error
	Cause error
}

func (e *NetworkError) Error() string { return e.Err.Error() }

// Unwrap exposes both the common base Error and the underlying transport
// error, so both errors.As(&Error{}) and errors.Is(context.DeadlineExceeded)
// style checks work against a NetworkError.
func (e *NetworkError) Unwrap() []error { return []error{e.Err, e.Cause} }

// WebhookError signals that a webhook could not be verified or parsed:
// the signature did not match, the public key could not be retrieved or
// parsed, or the payload was not valid JSON.
type WebhookError struct{ Err *Error }

func (e *WebhookError) Error() string { return e.Err.Error() }
func (e *WebhookError) Unwrap() error { return e.Err }

// Коды ошибок WATA, приходящие в поле error.code.
//
// Список неполный по своей природе: платформа может добавить код без изменения
// версии SDK. Поэтому это справочник известных значений, а не закрытый тип —
// APIError.Code всегда содержит исходную строку, даже незнакомую.
const (
	// Платёжные ссылки.
	CodeLinkNotFound = "PL_1001"
	CodeLinkInvalid  = "PL_1002"
	CodeLinkExpired  = "PL_1003"

	// Шифрование карточных данных.
	CodeCryptoInvalid = "CRY_1001"

	// Возвраты. Транзакции: TRA_1001..TRA_1019 — валидация,
	// TRA_2001..TRA_2999 — отказы шлюза и эмитента.
	CodeRefundInvalidAmount     = "TRA_1101"
	CodeRefundInsufficientFunds = "TRA_1102"
	CodeRefundPendingExists     = "TRA_1103"
)

// ErrorFamily — семейство, к которому относится код ошибки WATA.
type ErrorFamily string

const (
	FamilyPaymentLink ErrorFamily = "payment-link"
	FamilyCrypto      ErrorFamily = "crypto"
	FamilyTransaction ErrorFamily = "transaction"
	FamilyRefund      ErrorFamily = "refund"
	FamilyOrder       ErrorFamily = "order"
	FamilySteam       ErrorFamily = "steam"
	FamilyStars       ErrorFamily = "stars"
	FamilyTopUp       ErrorFamily = "topup"
	FamilyVoucher     ErrorFamily = "voucher"
	FamilyUnknown     ErrorFamily = "unknown"
)

// FamilyOf определяет семейство ошибки по префиксу кода. Полезно, чтобы
// обработать целую группу отказов, не перечисляя каждый код по отдельности.
func FamilyOf(code string) ErrorFamily {
	switch {
	case code == "":
		return FamilyUnknown
	case strings.HasPrefix(code, "PL_"):
		return FamilyPaymentLink
	case strings.HasPrefix(code, "CRY_"):
		return FamilyCrypto
	case strings.HasPrefix(code, "TRA_11"):
		return FamilyRefund
	case strings.HasPrefix(code, "TRA_"):
		return FamilyTransaction
	case strings.HasPrefix(code, "ORD_"):
		return FamilyOrder
	case strings.HasPrefix(code, "STM_"):
		return FamilySteam
	case strings.HasPrefix(code, "STR_"):
		return FamilyStars
	case strings.HasPrefix(code, "TPP_"):
		return FamilyTopUp
	case strings.HasPrefix(code, "VCR_"):
		return FamilyVoucher
	default:
		return FamilyUnknown
	}
}
