// Command create-link-and-webhook shows the two most common integration
// steps with the WATA Go SDK: creating a payment link, and receiving +
// verifying a webhook that reports its payment result.
//
// Run it with an acquiring token to actually create a link:
//
//	WATA_ACQUIRING_TOKEN=... go run ./examples/create-link-and-webhook
//
// Without a token, it still starts the webhook HTTP server so you can point
// a tool like `curl` or WATA's webhook test sender at it locally.
package main

import (
	"context"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/votsie/wata-sdk/go"
)

func main() {
	client := wata.New(wata.Tokens{
		Acquiring: os.Getenv("WATA_ACQUIRING_TOKEN"),
	})

	if client.Acquiring != nil && os.Getenv("WATA_ACQUIRING_TOKEN") != "" {
		createLink(client)
	} else {
		log.Println("WATA_ACQUIRING_TOKEN not set — skipping link creation, starting webhook server only")
	}

	// The webhook handler below is the important part of this example: it
	// shows the ONE correct way to verify a WATA webhook signature.
	http.HandleFunc("/webhooks/wata", func(w http.ResponseWriter, r *http.Request) {
		handleWebhook(client, w, r)
	})

	addr := ":8080"
	log.Printf("listening for WATA webhooks on %s/webhooks/wata", addr)
	log.Fatal(http.ListenAndServe(addr, nil))
}

func createLink(client *wata.Client) {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	link, err := client.Acquiring.CreateLink(ctx, wata.CreateLinkRequest{
		Amount:      500,
		Currency:    wata.CurrencyRUB,
		Description: "Пример заказа из create-link-and-webhook",
		OrderID:     "example-order-1",
	})
	if err != nil {
		log.Printf("не удалось создать ссылку: %v", err)
		return
	}
	log.Printf("ссылка создана: id=%s url=%s status=%s", link.ID, link.URL, link.Status)
}

// handleWebhook is the part to copy into a real integration.
//
// Two rules matter here:
//  1. Read the body as raw bytes and hand THOSE bytes to VerifyWebhook.
//     Never parse the JSON first and re-serialize it for verification —
//     that changes key order/whitespace and breaks the signature.
//  2. Always return 200 once the event has been safely recorded/queued,
//     and make the handler idempotent: WATA retries postpayment and
//     refund webhooks for up to 32 hours if it doesn't see a 200.
func handleWebhook(client *wata.Client, w http.ResponseWriter, r *http.Request) {
	rawBody, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "cannot read body", http.StatusBadRequest)
		return
	}

	signature := r.Header.Get("X-Signature")
	ok, err := client.VerifyWebhook(r.Context(), rawBody, signature)
	if err != nil {
		log.Printf("не удалось проверить подпись вебхука: %v", err)
		http.Error(w, "verification failed", http.StatusInternalServerError)
		return
	}
	if !ok {
		log.Printf("подпись вебхука не совпадает — запрос отклонён")
		http.Error(w, "invalid signature", http.StatusBadRequest)
		return
	}

	event, err := wata.ParseWebhook(rawBody)
	if err != nil {
		log.Printf("не удалось разобрать тело вебхука: %v", err)
		http.Error(w, "invalid payload", http.StatusBadRequest)
		return
	}

	// TODO in a real handler: look up event.OrderID / event.TransactionID in
	// your own storage and make this idempotent before doing anything with
	// side effects (shipping goods, marking an order paid, etc).
	pretty, _ := json.MarshalIndent(event, "", "  ")
	log.Printf("получен вебхук:\n%s", pretty)

	w.WriteHeader(http.StatusOK)
}
