package wata

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"math"
	"math/rand"
	"net/http"
	"net/url"
	"strconv"
	"time"
)

// apiRequest describes one HTTP call to a WATA product API.
type apiRequest struct {
	Method string
	// Path is appended to the product's path prefix ("/api/h2h" for
	// acquiring, "/api" for digital goods), e.g. "/links".
	Path  string
	Query url.Values
	// Body, if non-nil, becomes the JSON request body. It may be a Go value
	// (marshaled with encoding/json) or pre-encoded []byte / json.RawMessage
	// (used as-is) — the latter is how services merge a typed, documented
	// field with caller-supplied extra fields for endpoints SPEC.md does not
	// fully enumerate.
	Body any
	// Mutating marks a state-changing request (create link/payment/refund).
	// SPEC.md requires these to never be retried automatically, since a
	// retry could create a second payment. Mutating requests are attempted
	// exactly once regardless of WithMaxRetries.
	Mutating bool
	// NoAuth is set only for GET /api/h2h/public-key, the sole endpoint
	// called without an Authorization header.
	NoAuth bool
}

const userAgent = "wata-sdk-go/" + SDKVersion

func (c *Client) do(ctx context.Context, p product, r apiRequest, out any) error {
	var token string
	if !r.NoAuth {
		t, err := c.tokenFor(p)
		if err != nil {
			return err
		}
		token = t
	}

	base, err := c.baseURL(p)
	if err != nil {
		return err
	}

	fullPath := p.pathPrefix() + r.Path
	u, err := url.Parse(base + fullPath)
	if err != nil {
		return newConfigError(fmt.Sprintf("некорректный путь запроса %q: %v", fullPath, err))
	}
	if len(r.Query) > 0 {
		u.RawQuery = r.Query.Encode()
	}

	var bodyBytes []byte
	switch b := r.Body.(type) {
	case nil:
	case []byte:
		bodyBytes = b
	case json.RawMessage:
		bodyBytes = b
	default:
		data, err := json.Marshal(b)
		if err != nil {
			return newConfigError(fmt.Sprintf("не удалось сериализовать тело запроса: %v", err))
		}
		bodyBytes = data
	}

	// Mutating requests (create link/payment/refund) are never retried
	// automatically — a retry after a network blip could create a second
	// payment. GET requests, including the unauthenticated public-key
	// fetch, retry on network errors and 5xx per SPEC.md section 3.
	retryable := !r.Mutating

	attempts := 1
	if retryable {
		attempts = c.maxRetries
		if attempts < 1 {
			attempts = 1
		}
	}

	var lastErr error
	for attempt := 0; attempt < attempts; attempt++ {
		if attempt > 0 {
			if werr := waitBackoff(ctx, attempt); werr != nil {
				return lastErr
			}
		}

		reqCtx, cancel := context.WithTimeout(ctx, c.timeout)
		var bodyReader io.Reader
		if bodyBytes != nil {
			bodyReader = bytes.NewReader(bodyBytes)
		}
		httpReq, err := http.NewRequestWithContext(reqCtx, r.Method, u.String(), bodyReader)
		if err != nil {
			cancel()
			return &NetworkError{Err: &Error{Message: err.Error(), Path: fullPath}, Cause: err}
		}
		if bodyBytes != nil {
			httpReq.Header.Set("Content-Type", "application/json")
		}
		httpReq.Header.Set("Accept", "application/json")
		httpReq.Header.Set("User-Agent", userAgent)
		if !r.NoAuth {
			httpReq.Header.Set("Authorization", "Bearer "+token)
		}

		resp, err := c.httpClient.Do(httpReq)
		if err != nil {
			cancel()
			netErr := &NetworkError{Err: &Error{Message: err.Error(), Path: fullPath}, Cause: err}
			lastErr = netErr
			if retryable && attempt < attempts-1 {
				continue
			}
			return netErr
		}

		data, readErr := io.ReadAll(resp.Body)
		closeErr := resp.Body.Close()
		cancel()
		if readErr != nil {
			netErr := &NetworkError{
				Err:   &Error{Message: readErr.Error(), HTTPStatus: resp.StatusCode, Path: fullPath},
				Cause: readErr,
			}
			lastErr = netErr
			if retryable && attempt < attempts-1 {
				continue
			}
			return netErr
		}
		if closeErr != nil {
			// Body was fully read already; a close error here is not fatal
			// to interpreting the response we already have.
			_ = closeErr
		}

		if resp.StatusCode >= 200 && resp.StatusCode < 300 {
			if out != nil && len(data) > 0 {
				if err := json.Unmarshal(data, out); err != nil {
					return newConfigError(fmt.Sprintf("не удалось разобрать ответ WATA (%s %s): %v", r.Method, fullPath, err))
				}
				if rc, ok := out.(rawCapturer); ok {
					rc.captureRawJSON(data)
				}
			}
			return nil
		}

		if resp.StatusCode >= 500 {
			srvErr := &ServerError{Err: &Error{
				Message:    serverErrorMessage(data),
				HTTPStatus: resp.StatusCode,
				Path:       fullPath,
			}}
			lastErr = srvErr
			if retryable && attempt < attempts-1 {
				continue
			}
			return srvErr
		}

		// Non-retryable 4xx: return immediately, do not loop.
		return errorFromResponse(resp.StatusCode, fullPath, data, resp.Header)
	}
	return lastErr
}

// waitBackoff sleeps with exponential backoff and jitter before retry
// attempt number `attempt` (1-based: called before the 2nd, 3rd, ... try).
// It returns ctx.Err() if ctx is done before the wait completes.
func waitBackoff(ctx context.Context, attempt int) error {
	base := 200 * time.Millisecond
	backoff := time.Duration(float64(base) * math.Pow(2, float64(attempt-1)))
	const maxBackoff = 5 * time.Second
	if backoff > maxBackoff {
		backoff = maxBackoff
	}
	jitter := time.Duration(rand.Int63n(int64(backoff/2) + 1))
	wait := backoff/2 + jitter

	t := time.NewTimer(wait)
	defer t.Stop()
	select {
	case <-t.C:
		return nil
	case <-ctx.Done():
		return ctx.Err()
	}
}

// apiErrorBody mirrors the {error: {code, message, details,
// validationErrors}} shape SPEC.md documents for 4xx responses.
type apiErrorBody struct {
	Error struct {
		Code             string            `json:"code"`
		Message          string            `json:"message"`
		Details          any               `json:"details"`
		ValidationErrors []ValidationError `json:"validationErrors"`
	} `json:"error"`
}

func errorFromResponse(status int, path string, data []byte, header http.Header) error {
	var body apiErrorBody
	_ = json.Unmarshal(data, &body) // best-effort: some 4xx bodies may not match the shape.

	msg := body.Error.Message
	if msg == "" {
		msg = http.StatusText(status)
	}
	code := body.Error.Code

	switch status {
	case http.StatusUnauthorized, http.StatusForbidden:
		return &AuthError{Err: &Error{Message: msg, HTTPStatus: status, Path: path, Code: code}}
	case http.StatusTooManyRequests:
		return &RateLimitError{
			Err:        &Error{Message: msg, HTTPStatus: status, Path: path, Code: code},
			RetryAfter: parseRetryAfter(header),
		}
	default:
		return &APIError{
			Err:              &Error{Message: msg, HTTPStatus: status, Path: path, Code: code},
			Details:          body.Error.Details,
			ValidationErrors: body.Error.ValidationErrors,
		}
	}
}

func parseRetryAfter(header http.Header) *time.Duration {
	v := header.Get("Retry-After")
	if v == "" {
		return nil
	}
	if secs, err := strconv.Atoi(v); err == nil {
		d := time.Duration(secs) * time.Second
		return &d
	}
	if t, err := http.ParseTime(v); err == nil {
		d := time.Until(t)
		return &d
	}
	return nil
}

func serverErrorMessage(data []byte) string {
	var body apiErrorBody
	if err := json.Unmarshal(data, &body); err == nil && body.Error.Message != "" {
		return body.Error.Message
	}
	if len(data) > 0 && len(data) < 500 {
		return string(data)
	}
	return "внутренняя ошибка сервера WATA"
}

// marshalWithExtra marshals known to JSON, then overlays the keys of extra
// on top of the resulting object (extra wins on key conflicts). It is used
// by digital-goods request builders to combine a typed, spec-documented
// field (e.g. netAmount) with additional fields the live API accepts but
// SPEC.md does not name.
func marshalWithExtra(known any, extra map[string]any) (json.RawMessage, error) {
	base, err := json.Marshal(known)
	if err != nil {
		return nil, err
	}
	if len(extra) == 0 {
		return base, nil
	}
	m := map[string]any{}
	if err := json.Unmarshal(base, &m); err != nil {
		return nil, err
	}
	for k, v := range extra {
		m[k] = v
	}
	return json.Marshal(m)
}
