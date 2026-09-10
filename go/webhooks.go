package wata

import (
	"context"
	"crypto"
	"crypto/rsa"
	"crypto/sha512"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"net/http"
)

// publicKeyResponse mirrors the body of GET /api/h2h/public-key.
type publicKeyResponse struct {
	Value string `json:"value"`
}

func (c *Client) fetchPublicKeyPEM(ctx context.Context) (string, error) {
	var out publicKeyResponse
	err := c.do(ctx, productAcquiring, apiRequest{
		Method: http.MethodGet,
		Path:   "/public-key",
		NoAuth: true,
	}, &out)
	if err != nil {
		return "", err
	}
	return out.Value, nil
}

// getOrFetchPublicKey returns the cached webhook public key for this
// client's Environment, fetching and caching it on first use. The cache is
// scoped to this Client instance, which in turn is scoped to exactly one
// Environment for its lifetime — production and sandbox keys never share a
// cache slot, since production and sandbox always live in different
// Clients.
func (c *Client) getOrFetchPublicKey(ctx context.Context) (*rsa.PublicKey, error) {
	c.keyMu.Lock()
	if c.cachedKey != nil {
		key := c.cachedKey
		c.keyMu.Unlock()
		return key, nil
	}
	c.keyMu.Unlock()

	pemStr, err := c.fetchPublicKeyPEM(ctx)
	if err != nil {
		return nil, &WebhookError{Err: &Error{Message: fmt.Sprintf("не удалось получить публичный ключ вебхуков: %v", err)}}
	}
	key, err := parsePublicKeyPEM(pemStr)
	if err != nil {
		return nil, &WebhookError{Err: &Error{Message: fmt.Sprintf("получен некорректный публичный ключ вебхуков: %v", err)}}
	}

	c.keyMu.Lock()
	c.cachedKey = key
	c.keyMu.Unlock()
	return key, nil
}

func parsePublicKeyPEM(pemStr string) (*rsa.PublicKey, error) {
	block, _ := pem.Decode([]byte(pemStr))
	if block == nil {
		return nil, errors.New("не удалось декодировать PEM-блок")
	}
	if pub, err := x509.ParsePKIXPublicKey(block.Bytes); err == nil {
		if rsaPub, ok := pub.(*rsa.PublicKey); ok {
			return rsaPub, nil
		}
		return nil, errors.New("ключ не является RSA-ключом")
	}
	if rsaPub, err := x509.ParsePKCS1PublicKey(block.Bytes); err == nil {
		return rsaPub, nil
	}
	if cert, err := x509.ParseCertificate(block.Bytes); err == nil {
		if rsaPub, ok := cert.PublicKey.(*rsa.PublicKey); ok {
			return rsaPub, nil
		}
	}
	return nil, errors.New("неизвестный формат публичного ключа")
}

// verifySignature checks a base64 X-Signature against rawBody using
// SHA512withRSA (RSA PKCS#1 v1.5 + SHA-512) — SPEC.md is explicit that this
// is not SHA-256. It returns (false, nil) for a signature that is well
// formed but does not match, and (false, err) when verification could not
// even be attempted (bad base64, unusable key).
func verifySignature(rawBody []byte, signatureBase64 string, pub *rsa.PublicKey) (bool, error) {
	sig, err := base64.StdEncoding.DecodeString(signatureBase64)
	if err != nil {
		return false, &WebhookError{Err: &Error{Message: fmt.Sprintf("подпись X-Signature не в base64: %v", err)}}
	}
	hash := sha512.Sum512(rawBody)
	if err := rsa.VerifyPKCS1v15(pub, crypto.SHA512, hash[:], sig); err != nil {
		return false, nil
	}
	return true, nil
}

// VerifyWebhook checks a webhook's signature against WATA's public key for
// this client's Environment, fetching and caching that key on first use.
//
// rawBody MUST be the exact bytes of the HTTP request body, before any JSON
// parsing. Re-marshaling a parsed struct changes key order and whitespace
// and will make a genuine signature fail to verify — that is why this
// method takes []byte rather than a struct, and there is deliberately no
// overload that accepts one.
//
// signatureBase64 is the raw value of the X-Signature header.
func (c *Client) VerifyWebhook(ctx context.Context, rawBody []byte, signatureBase64 string) (bool, error) {
	key, err := c.getOrFetchPublicKey(ctx)
	if err != nil {
		return false, err
	}
	return verifySignature(rawBody, signatureBase64, key)
}

// VerifyWebhookString is a convenience wrapper around VerifyWebhook for
// callers holding the raw body as a string rather than []byte. Like
// VerifyWebhook, it never accepts a parsed object.
func (c *Client) VerifyWebhookString(ctx context.Context, rawBody string, signatureBase64 string) (bool, error) {
	return c.VerifyWebhook(ctx, []byte(rawBody), signatureBase64)
}

// VerifyWebhookWithKey checks a webhook's signature against an explicitly
// supplied PEM-encoded public key, without touching the client's cache or
// making any network call. Use this if you fetch and store the key
// yourself (e.g. to avoid a network round trip on every webhook).
//
// rawBody has the same raw-bytes requirement as VerifyWebhook.
func VerifyWebhookWithKey(rawBody []byte, signatureBase64 string, keyPEM string) (bool, error) {
	key, err := parsePublicKeyPEM(keyPEM)
	if err != nil {
		return false, &WebhookError{Err: &Error{Message: fmt.Sprintf("некорректный публичный ключ: %v", err)}}
	}
	return verifySignature(rawBody, signatureBase64, key)
}

// ParseWebhook decodes a webhook payload into a WebhookEvent. It does not
// verify the signature — call VerifyWebhook (or VerifyWebhookWithKey) as
// well, on the same raw bytes, before trusting the event.
func ParseWebhook(rawBody []byte) (*WebhookEvent, error) {
	var ev WebhookEvent
	if err := json.Unmarshal(rawBody, &ev); err != nil {
		return nil, &WebhookError{Err: &Error{Message: fmt.Sprintf("не удалось разобрать тело вебхука: %v", err)}}
	}
	ev.captureRawJSON(rawBody)
	return &ev, nil
}
