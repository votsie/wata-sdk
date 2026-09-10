package wata

import (
	"encoding/json"
	"net/url"
	"strconv"
	"time"
)

// -----------------------------------------------------------------------
// Enums. Every enum in this SDK is a plain string type: WATA's API is live
// and may add values the SDK has not seen yet, and a string type accepts an
// unknown value without failing to parse — the caller gets the raw string
// back instead of a decode error. Do not add exhaustive switch statements
// with a panicking default anywhere in this SDK; treat an unrecognized value
// as data, not as a bug.
// -----------------------------------------------------------------------

// Currency is a WATA transaction/link currency code.
type Currency string

const (
	CurrencyRUB Currency = "RUB"
	CurrencyUSD Currency = "USD"
	CurrencyEUR Currency = "EUR"
	// CurrencyGBP is present in WATA's schema but not confirmed by the public
	// documentation as of SPEC.md. Included for forward compatibility.
	CurrencyGBP Currency = "GBP"
)

// TransactionStatus is the lifecycle status of an acquiring transaction.
type TransactionStatus string

const (
	TransactionStatusCreated  TransactionStatus = "Created"
	TransactionStatusPending  TransactionStatus = "Pending"
	TransactionStatusPaid     TransactionStatus = "Paid"
	TransactionStatusDeclined TransactionStatus = "Declined"
)

// TransactionKind distinguishes a payment from a refund.
type TransactionKind string

const (
	TransactionKindPayment TransactionKind = "Payment"
	TransactionKindRefund  TransactionKind = "Refund"
)

// TransactionType is the payment method used for a transaction.
type TransactionType string

const (
	TransactionTypeCardCrypto TransactionType = "CardCrypto"
	TransactionTypeSBP        TransactionType = "SBP"
	TransactionTypeTPay       TransactionType = "TPay"
)

// PaymentLinkStatus is the lifecycle status of a payment link.
type PaymentLinkStatus string

const (
	PaymentLinkStatusOpened PaymentLinkStatus = "Opened"
	PaymentLinkStatusClosed PaymentLinkStatus = "Closed"
)

// PaymentLinkType selects whether a link can be paid once or repeatedly.
type PaymentLinkType string

const (
	PaymentLinkTypeOneTime  PaymentLinkType = "OneTime"
	PaymentLinkTypeManyTime PaymentLinkType = "ManyTime"
)

// SubscriptionInterval is the billing interval for a subscription link.
type SubscriptionInterval string

const (
	SubscriptionIntervalTest  SubscriptionInterval = "Test"
	SubscriptionIntervalWeek  SubscriptionInterval = "Week"
	SubscriptionIntervalMonth SubscriptionInterval = "Month"
)

// DGOrderStatus is the lifecycle status of a digital-goods order (Steam,
// Top-Up, vouchers): Pending -> Paid -> Success | Fail.
type DGOrderStatus string

const (
	DGOrderStatusPending DGOrderStatus = "Pending"
	DGOrderStatusPaid    DGOrderStatus = "Paid"
	DGOrderStatusSuccess DGOrderStatus = "Success"
	DGOrderStatusFail    DGOrderStatus = "Fail"
)

// StarsOrderStatus is the lifecycle status of a Telegram Stars order:
// Pending -> Review -> Paid | Refunded -> Success | Fail.
//
// StarsOrderStatusReview means the order exceeded the auto-approval
// threshold (on the order of 2000 RUB, per SPEC.md — WATA does not commit to
// an exact figure) and will NOT be fulfilled until it is explicitly acted
// on: StarsService.ConfirmOrder moves it Review -> Paid,
// StarsService.RejectOrder moves it Review -> Refunded.
type StarsOrderStatus string

const (
	StarsOrderStatusPending  StarsOrderStatus = "Pending"
	StarsOrderStatusReview   StarsOrderStatus = "Review"
	StarsOrderStatusPaid     StarsOrderStatus = "Paid"
	StarsOrderStatusRefunded StarsOrderStatus = "Refunded"
	StarsOrderStatusSuccess  StarsOrderStatus = "Success"
	StarsOrderStatusFail     StarsOrderStatus = "Fail"
)

// DepositOrderStatus is the lifecycle status of an order paid from the
// merchant's digital-goods deposit balance.
type DepositOrderStatus string

const (
	DepositOrderStatusPending DepositOrderStatus = "Pending"
	DepositOrderStatusSuccess DepositOrderStatus = "Success"
	DepositOrderStatusFail    DepositOrderStatus = "Fail"
)

// DepositOrderType identifies which product a deposit-funded order belongs
// to, as returned by GET /v1/deposit/order/{orderId}.
type DepositOrderType string

const (
	DepositOrderTypeSteam    DepositOrderType = "Steam"
	DepositOrderTypeTopUp    DepositOrderType = "TopUp"
	DepositOrderTypeVouchers DepositOrderType = "Vouchers"
)

// -----------------------------------------------------------------------
// Forward-compatibility helper.
// -----------------------------------------------------------------------

// RawBody, embedded in a response type, captures the exact JSON payload the
// API returned. It is embedded in response types whose full field set
// SPEC.md does not enumerate exhaustively (mostly digital-goods orders and
// quotes), so callers can reach fields the SDK does not model yet without
// waiting for a new SDK release: json.Unmarshal(resp.Raw, &myStruct).
type RawBody struct {
	Raw json.RawMessage `json:"-"`
}

func (r *RawBody) captureRawJSON(data []byte) {
	r.Raw = append(json.RawMessage(nil), data...)
}

// rawCapturer is implemented by any response type embedding RawBody.
type rawCapturer interface {
	captureRawJSON(data []byte)
}

// -----------------------------------------------------------------------
// Acquiring: payment links.
// -----------------------------------------------------------------------

// Subscription describes recurring billing for a payment link.
type Subscription struct {
	Period     int                  `json:"period"`
	Interval   SubscriptionInterval `json:"interval"`
	MaxPeriods int                  `json:"maxPeriods"`
	Amount     float64              `json:"amount"`
	StartDate  *time.Time           `json:"startDate,omitempty"`
}

// CreateLinkRequest is the body of POST /api/h2h/links.
type CreateLinkRequest struct {
	// Amount and Currency are required by the API.
	Amount   float64  `json:"amount"`
	Currency Currency `json:"currency"`

	Description              string          `json:"description,omitempty"`
	OrderID                  string          `json:"orderId,omitempty"`
	SuccessRedirectURL       string          `json:"successRedirectUrl,omitempty"`
	FailRedirectURL          string          `json:"failRedirectUrl,omitempty"`
	ExpirationDateTime       *time.Time      `json:"expirationDateTime,omitempty"`
	Type                     PaymentLinkType `json:"type,omitempty"`
	IsArbitraryAmountAllowed *bool           `json:"isArbitraryAmountAllowed,omitempty"`
	ArbitraryAmountPrompts   []float64       `json:"arbitraryAmountPrompts,omitempty"`
	Email                    string          `json:"email,omitempty"`
	Phone                    string          `json:"phone,omitempty"`
	Username                 string          `json:"username,omitempty"`
	UserID                   string          `json:"userId,omitempty"`
	Subscription             *Subscription   `json:"subscription,omitempty"`
}

// PaymentLink is the shape of a payment link, returned by CreateLink,
// SearchLinks and GetLink.
type PaymentLink struct {
	ID                       string            `json:"id"`
	Amount                   float64           `json:"amount"`
	Currency                 Currency          `json:"currency"`
	Status                   PaymentLinkStatus `json:"status"`
	URL                      string            `json:"url"`
	TerminalName             string            `json:"terminalName"`
	TerminalPublicID         string            `json:"terminalPublicId"`
	CreationTime             time.Time         `json:"creationTime"`
	Type                     PaymentLinkType   `json:"type"`
	OrderID                  string            `json:"orderId,omitempty"`
	ExpirationDateTime       *time.Time        `json:"expirationDateTime,omitempty"`
	IsArbitraryAmountAllowed bool              `json:"isArbitraryAmountAllowed"`
	ArbitraryAmounts         []float64         `json:"arbitraryAmounts,omitempty"`
}

// LinkPage is the response of GET /api/h2h/links.
type LinkPage struct {
	Items      []PaymentLink `json:"items"`
	TotalCount int           `json:"totalCount"`
}

// SearchLinksParams are the query parameters of GET /api/h2h/links.
type SearchLinksParams struct {
	OrderID          string
	CreationTimeFrom *time.Time
	CreationTimeTo   *time.Time
	AmountFrom       *float64
	AmountTo         *float64
	Currencies       []Currency
	Statuses         []PaymentLinkStatus
	// Sorting is one of "orderId", "creationTime", "amount", optionally
	// suffixed with " desc".
	Sorting string
	// SkipCount and MaxResultCount default to the API's own defaults (0 and
	// 10) when left at zero. MaxResultCount is capped by the API at 1000.
	SkipCount      int
	MaxResultCount int
}

func (p SearchLinksParams) toQuery() url.Values {
	q := url.Values{}
	addIfNotEmpty(q, "OrderId", p.OrderID)
	addTimePtr(q, "CreationTimeFrom", p.CreationTimeFrom)
	addTimePtr(q, "CreationTimeTo", p.CreationTimeTo)
	addFloatPtr(q, "AmountFrom", p.AmountFrom)
	addFloatPtr(q, "AmountTo", p.AmountTo)
	for _, c := range p.Currencies {
		q.Add("Currencies", string(c))
	}
	for _, s := range p.Statuses {
		q.Add("Statuses", string(s))
	}
	addIfNotEmpty(q, "Sorting", p.Sorting)
	addIfPositive(q, "SkipCount", p.SkipCount)
	addIfPositive(q, "MaxResultCount", p.MaxResultCount)
	return q
}

// -----------------------------------------------------------------------
// Acquiring: transactions.
// -----------------------------------------------------------------------

// Transaction is an acquiring transaction (payment or refund). The field set
// reflects what SPEC.md and the webhook payload document; the live API may
// return additional fields not modeled here.
type Transaction struct {
	ID       string            `json:"id"`
	OrderID  string            `json:"orderId,omitempty"`
	Amount   float64           `json:"amount"`
	Currency Currency          `json:"currency"`
	Status   TransactionStatus `json:"status"`
	Kind     TransactionKind   `json:"kind"`
	Type     TransactionType   `json:"type,omitempty"`
	// TotalCommission is named "commission" in the webhook payload but
	// "totalCommission" in this endpoint's response — this is a genuine API
	// inconsistency documented in SPEC.md, not a typo in this SDK.
	TotalCommission  float64    `json:"totalCommission,omitempty"`
	CreationTime     time.Time  `json:"creationTime,omitempty"`
	PaymentTime      *time.Time `json:"paymentTime,omitempty"`
	PaymentLinkID    string     `json:"paymentLinkId,omitempty"`
	ErrorCode        string     `json:"errorCode,omitempty"`
	ErrorDescription string     `json:"errorDescription,omitempty"`
	RawBody
}

// SearchTransactionsParams are the query parameters of GET /api/h2h/v2/transactions.
//
// Do not set the cursor fields (CursorID, CursorAmount, CursorDate) by hand
// when paging manually — use AcquiringService.Transactions, which threads
// them for you. Manual paging is exactly what SPEC.md warns is error-prone:
// forgetting CursorDate silently breaks the second page.
type SearchTransactionsParams struct {
	OrderID          string
	CreationTimeFrom *time.Time
	CreationTimeTo   *time.Time
	AmountFrom       *float64
	AmountTo         *float64
	Currencies       []Currency
	PaymentLinkIDs   []string
	Statuses         []TransactionStatus
	Sorting          string
	MaxResultCount   int

	CursorID     string
	CursorAmount *float64
	CursorDate   *time.Time
}

func (p SearchTransactionsParams) toQuery() url.Values {
	q := url.Values{}
	addIfNotEmpty(q, "OrderId", p.OrderID)
	addTimePtr(q, "CreationTimeFrom", p.CreationTimeFrom)
	addTimePtr(q, "CreationTimeTo", p.CreationTimeTo)
	addFloatPtr(q, "AmountFrom", p.AmountFrom)
	addFloatPtr(q, "AmountTo", p.AmountTo)
	for _, c := range p.Currencies {
		q.Add("Currencies", string(c))
	}
	for _, id := range p.PaymentLinkIDs {
		q.Add("PaymentLinkIds", id)
	}
	for _, s := range p.Statuses {
		q.Add("Statuses", string(s))
	}
	addIfNotEmpty(q, "Sorting", p.Sorting)
	addIfPositive(q, "MaxResultCount", p.MaxResultCount)
	addIfNotEmpty(q, "CursorId", p.CursorID)
	addFloatPtr(q, "CursorAmount", p.CursorAmount)
	addTimePtr(q, "CursorDate", p.CursorDate)
	return q
}

// TransactionPage is the response of GET /api/h2h/v2/transactions.
type TransactionPage struct {
	Items            []Transaction `json:"items"`
	HasNextPage      bool          `json:"hasNextPage"`
	NextCursorID     string        `json:"nextCursorId,omitempty"`
	NextCursorDate   *time.Time    `json:"nextCursorDate,omitempty"`
	NextCursorAmount *float64      `json:"nextCursorAmount,omitempty"`
}

// RefundRequest is the body of POST /api/h2h/transactions/refunds. There is
// no "reason" field — the API does not have one — and no currency field: the
// currency is taken from the original transaction.
type RefundRequest struct {
	OriginalTransactionID string  `json:"originalTransactionId"`
	Amount                float64 `json:"amount"`
}

// RefundResponse is the response of POST /api/h2h/transactions/refunds.
type RefundResponse struct {
	TransactionID         string            `json:"transactionId"`
	OriginalTransactionID string            `json:"originalTransactionId"`
	TransactionStatus     TransactionStatus `json:"transactionStatus"`
	// Kind is always TransactionKindRefund for this endpoint.
	Kind             TransactionKind `json:"kind"`
	ErrorCode        string          `json:"errorCode,omitempty"`
	ErrorDescription string          `json:"errorDescription,omitempty"`
}

// BalanceResponse is the response of GET /api/h2h/finance/balance.
type BalanceResponse struct {
	TerminalPublicID string   `json:"terminalPublicId"`
	Date             string   `json:"date"`
	Balance          float64  `json:"balance"`
	Currency         Currency `json:"currency"`
}

// -----------------------------------------------------------------------
// Acquiring: direct payments.
// -----------------------------------------------------------------------

// DeviceData is the browser fingerprint payload required by the direct
// payment endpoints (3-D Secure device data collection). SPEC.md names the
// field but does not enumerate its shape, so it is left as a free-form map:
// populate it with whatever your checkout's device-fingerprinting script
// collects.
type DeviceData map[string]any

// ThreeDsData describes a required 3-D Secure step: either redirect the
// payer to URL, or auto-submit Parameters as a form to URL via Method.
type ThreeDsData struct {
	URL        string            `json:"url"`
	Method     string            `json:"method"`
	Parameters map[string]string `json:"parameters,omitempty"`
}

// CardCryptoRequest is the body of POST /api/h2h/payments/card-crypto.
//
// CardCrypto is produced by WATA's client-side checkout script running in
// the payer's browser; the merchant server never assembles or sees raw card
// data, and must not attempt to build this value itself.
type CardCryptoRequest struct {
	Amount     float64    `json:"amount"`
	Currency   Currency   `json:"currency"`
	CardCrypto string     `json:"cardCrypto"`
	IP         string     `json:"ip"`
	ReturnURL  string     `json:"returnUrl"`
	DeviceData DeviceData `json:"deviceData"`
}

// CardCryptoResponse is the response of POST /api/h2h/payments/card-crypto.
// When ThreeDsData is present, the payer must complete a 3-D Secure step
// before the payment resolves.
type CardCryptoResponse struct {
	TransactionID string            `json:"transactionId,omitempty"`
	Status        TransactionStatus `json:"status,omitempty"`
	ThreeDsData   *ThreeDsData      `json:"threeDsData,omitempty"`
	RawBody
}

// SbpRequest is the body of POST /api/h2h/payments/sbp. There is no currency
// field — SBP payments are always RUB. FirstName/LastName are optional.
type SbpRequest struct {
	Amount     float64    `json:"amount"`
	IP         string     `json:"ip"`
	ReturnURL  string     `json:"returnUrl"`
	DeviceData DeviceData `json:"deviceData"`
	FirstName  string     `json:"firstName,omitempty"`
	LastName   string     `json:"lastName,omitempty"`
}

// SbpResponse is the response of POST /api/h2h/payments/sbp.
type SbpResponse struct {
	SbpLink string `json:"sbpLink"`
	RawBody
}

// TPayRequest is the body of POST /api/h2h/payments/tpay. Same shape as
// SbpRequest but without FirstName/LastName, which T-Pay does not accept.
type TPayRequest struct {
	Amount     float64    `json:"amount"`
	IP         string     `json:"ip"`
	ReturnURL  string     `json:"returnUrl"`
	DeviceData DeviceData `json:"deviceData"`
}

// TPayResponse is the response of POST /api/h2h/payments/tpay.
type TPayResponse struct {
	TPayLink string `json:"tPayLink"`
	RawBody
}

// -----------------------------------------------------------------------
// Webhooks.
// -----------------------------------------------------------------------

// PayerData carries payer identification passed through in a webhook event.
type PayerData struct {
	PayerID string `json:"payerId,omitempty"`
}

// WebhookEvent is the payload WATA posts to a merchant's webhook endpoint.
// Note that the commission field here is named "commission", while the same
// value is called "totalCommission" on GET /transactions/{id} — that is a
// genuine inconsistency in the WATA API, documented in SPEC.md.
type WebhookEvent struct {
	TransactionType       TransactionType   `json:"transactionType,omitempty"`
	Kind                  TransactionKind   `json:"kind,omitempty"`
	ID                    string            `json:"id,omitempty"`
	TransactionID         string            `json:"transactionId,omitempty"`
	OriginalTransactionID string            `json:"originalTransactionId,omitempty"`
	TerminalPublicID      string            `json:"terminalPublicId,omitempty"`
	TransactionStatus     TransactionStatus `json:"transactionStatus,omitempty"`
	ErrorCode             string            `json:"errorCode,omitempty"`
	ErrorDescription      string            `json:"errorDescription,omitempty"`
	TerminalName          string            `json:"terminalName,omitempty"`
	Amount                float64           `json:"amount,omitempty"`
	Currency              Currency          `json:"currency,omitempty"`
	OrderID               string            `json:"orderId,omitempty"`
	OrderDescription      string            `json:"orderDescription,omitempty"`
	Commission            float64           `json:"commission,omitempty"`
	PaymentTime           *time.Time        `json:"paymentTime,omitempty"`
	Email                 string            `json:"email,omitempty"`
	PaymentLinkID         string            `json:"paymentLinkId,omitempty"`
	PayerData             *PayerData        `json:"payerData,omitempty"`
	RawBody
}

// -----------------------------------------------------------------------
// Digital goods: field-level shapes per SPEC.md section 10 ("Точные поля
// Digital Goods"). That section was added specifically so implementations
// did not have to guess field names; every field it names is a typed struct
// field below. Every response type still embeds RawBody regardless — SPEC.md
// section 10 itself requires this, since WATA's own documentation does not
// cover every field the live API returns, and RawBody is the only way to
// reach a field this SDK does not yet model without waiting for a release.
// -----------------------------------------------------------------------

// SteamNetAmountQuery is the query for GET /v3/steam/amount: given the
// amount that should end up credited to a Steam account, returns what the
// payer must pay.
type SteamNetAmountQuery struct {
	NetAmount float64
	Account   string
}

func (q SteamNetAmountQuery) toQuery() url.Values {
	v := url.Values{}
	v.Set("netAmount", formatFloat(q.NetAmount))
	v.Set("account", q.Account)
	return v
}

// SteamAmountQuery is the query for GET /v3/steam/by-amount: given the
// amount the payer pays and a margin, returns what gets credited to the
// Steam account.
type SteamAmountQuery struct {
	Amount  float64
	Margin  float64
	Account string
}

func (q SteamAmountQuery) toQuery() url.Values {
	v := url.Values{}
	v.Set("amount", formatFloat(q.Amount))
	v.Set("margin", formatFloat(q.Margin))
	v.Set("account", q.Account)
	return v
}

// SteamDepositPriceQuery is the query for GET /v1/steam/deposit/price
// (deposit-funded): given the amount to credit, returns its price in USD.
type SteamDepositPriceQuery struct {
	Account   string
	NetAmount float64
}

func (q SteamDepositPriceQuery) toQuery() url.Values {
	v := url.Values{}
	v.Set("account", q.Account)
	v.Set("netAmount", formatFloat(q.NetAmount))
	return v
}

// SteamDepositNetAmountQuery is the query for GET
// /v1/steam/deposit/netamount (deposit-funded): given a price, returns the
// amount it credits.
type SteamDepositNetAmountQuery struct {
	Account string
	Price   float64
}

func (q SteamDepositNetAmountQuery) toQuery() url.Values {
	v := url.Values{}
	v.Set("account", q.Account)
	v.Set("price", formatFloat(q.Price))
	return v
}

// SteamQuote is the response shape shared by every Steam price-quote
// endpoint (acquiring and deposit-funded alike). Which fields are populated
// depends on which quote endpoint was called — see SPEC.md section 10.
type SteamQuote struct {
	Price     float64 `json:"price,omitempty"`
	MinPrice  float64 `json:"minPrice,omitempty"`
	NetAmount float64 `json:"netAmount,omitempty"`
	SteamRate float64 `json:"steamRate,omitempty"`
	RawBody
}

// CreateSteamOrderByNetAmountRequest is the body of POST /v3/steam: create
// an acquiring-funded Steam order specifying the amount to credit.
type CreateSteamOrderByNetAmountRequest struct {
	Account     string  `json:"account"`
	Amount      float64 `json:"amount"`
	NetAmount   float64 `json:"netAmount"`
	Description string  `json:"description"`
	OrderID     string  `json:"orderId"`
	// SuccessRedirectURL and FailRedirectURL are optional, per SPEC.md.
	SuccessRedirectURL string `json:"successRedirectUrl,omitempty"`
	FailRedirectURL    string `json:"failRedirectUrl,omitempty"`
}

// CreateSteamOrderByAmountRequest is the body of POST /v3/steam/by-amount:
// create an acquiring-funded Steam order specifying the amount the payer
// pays, plus a margin.
type CreateSteamOrderByAmountRequest struct {
	Account            string  `json:"account"`
	Amount             float64 `json:"amount"`
	Margin             float64 `json:"margin"`
	Description        string  `json:"description"`
	OrderID            string  `json:"orderId"`
	SuccessRedirectURL string  `json:"successRedirectUrl,omitempty"`
	FailRedirectURL    string  `json:"failRedirectUrl,omitempty"`
}

// SteamOrder is an acquiring-funded Steam order, returned by
// CreateOrderByNetAmount, CreateOrderByAmount and GetOrder. Which fields are
// populated depends on which of those produced it — see SPEC.md section 10.
type SteamOrder struct {
	OrderID            string        `json:"orderId,omitempty"`
	Amount             float64       `json:"amount,omitempty"`
	Price              float64       `json:"price,omitempty"`
	MinPrice           float64       `json:"minPrice,omitempty"`
	NetAmount          float64       `json:"netAmount,omitempty"`
	Commission         float64       `json:"commission,omitempty"`
	Margin             float64       `json:"margin,omitempty"`
	SteamRate          float64       `json:"steamRate,omitempty"`
	PaymentLink        string        `json:"paymentLink,omitempty"`
	Status             DGOrderStatus `json:"status,omitempty"`
	SuccessRedirectURL string        `json:"successRedirectUrl,omitempty"`
	FailRedirectURL    string        `json:"failRedirectUrl,omitempty"`
	RawBody
}

// CreateSteamDepositOrderByNetAmountRequest is the body of POST
// /v1/steam/deposit: create a deposit-funded Steam order specifying the
// amount to credit.
type CreateSteamDepositOrderByNetAmountRequest struct {
	Account     string  `json:"account"`
	NetAmount   float64 `json:"netAmount"`
	Description string  `json:"description"`
	OrderID     string  `json:"orderId"`
}

// CreateSteamDepositOrderByPriceRequest is the body of POST
// /v1/steam/deposit/by-price: create a deposit-funded Steam order
// specifying the price to pay.
type CreateSteamDepositOrderByPriceRequest struct {
	Account     string  `json:"account"`
	Price       float64 `json:"price"`
	Description string  `json:"description"`
	OrderID     string  `json:"orderId"`
}

// SteamDepositOrder is a deposit-funded Steam order, returned by
// CreateDepositOrderByNetAmount and CreateDepositOrderByPrice. It does not
// carry a status: poll GetDepositOrder (the shared /v1/deposit/order/{id}
// endpoint) for that.
type SteamDepositOrder struct {
	OrderID   string  `json:"orderId,omitempty"`
	Account   string  `json:"account,omitempty"`
	Price     float64 `json:"price,omitempty"`
	NetAmount float64 `json:"netAmount,omitempty"`
	SteamRate float64 `json:"steamRate,omitempty"`
	RawBody
}

// StarsPrice is the response of GET /stars/price.
type StarsPrice struct {
	StarPrice float64 `json:"starPrice,omitempty"`
	MinPrice  float64 `json:"minPrice,omitempty"`
	RawBody
}

// CreateStarsOrderRequest is the body of POST /stars. Count must be between
// 50 and 50000 per SPEC.md; the API enforces this, not the SDK.
type CreateStarsOrderRequest struct {
	Username    string  `json:"username"`
	Count       int     `json:"count"`
	Amount      float64 `json:"amount"`
	Description string  `json:"description"`
	OrderID     string  `json:"orderId"`
}

// StarsOrder is a Telegram Stars order, returned by CreateOrder, GetOrder,
// ConfirmOrder and RejectOrder. See StarsOrderStatus for the lifecycle, and
// in particular StarsOrderStatusReview: an order stops there and is NOT
// fulfilled until StarsService.ConfirmOrder or StarsService.RejectOrder is
// called.
type StarsOrder struct {
	OrderID      string           `json:"orderId,omitempty"`
	Username     string           `json:"username,omitempty"`
	Count        int              `json:"count,omitempty"`
	Amount       float64          `json:"amount,omitempty"`
	Price        float64          `json:"price,omitempty"`
	Commission   float64          `json:"commission,omitempty"`
	Description  string           `json:"description,omitempty"`
	PaymentLink  string           `json:"paymentLink,omitempty"`
	Status       StarsOrderStatus `json:"status,omitempty"`
	CreationTime *time.Time       `json:"creationTime,omitempty"`
	RawBody
}

// TopUpProduct is one purchasable item in an acquiring-funded top-up
// category, as listed by TopUpService.ListOffers.
type TopUpProduct struct {
	ID          string  `json:"id"`
	Name        string  `json:"name"`
	Price       float64 `json:"price"`
	MinPrice    float64 `json:"minPrice,omitempty"`
	IsAvailable bool    `json:"isAvailable"`
}

// TopUpCategory is the response of GET /v3/topup/all. Unlike the
// deposit-funded catalog (TopUpDepositCatalog), SPEC.md describes this
// endpoint's response as a single category, not a list of them.
type TopUpCategory struct {
	CategoryID   string `json:"categoryId,omitempty"`
	CategoryName string `json:"categoryName,omitempty"`
	Type         string `json:"type,omitempty"`
	// Fields describes the custom input fields this category's products
	// require when creating an order (e.g. a game account id). SPEC.md does
	// not give the shape of a field descriptor, so each entry is kept as raw
	// JSON — inspect Raw, or a live response, for the exact shape, and pass
	// matching keys/values in CreateTopUpOrderRequest.Fields.
	Fields   []json.RawMessage `json:"fields,omitempty"`
	Products []TopUpProduct    `json:"products,omitempty"`
	RawBody
}

// CreateTopUpOrderRequest is the body of POST /v3/topup.
type CreateTopUpOrderRequest struct {
	TopUpID string  `json:"topupId"`
	Amount  float64 `json:"amount"`
	OrderID string  `json:"orderId"`
	// Fields carries the values for whatever custom fields the chosen
	// product's TopUpCategory.Fields declares (e.g. a game account id).
	// SPEC.md names this field "fields" without enumerating sub-keys, since
	// they vary per product.
	Fields      map[string]string `json:"fields"`
	Email       string            `json:"email"`
	Description string            `json:"description"`
}

// TopUpOrder is an acquiring-funded top-up order, returned by CreateOrder
// and GetOrder.
type TopUpOrder struct {
	OrderID     string        `json:"orderId,omitempty"`
	Status      DGOrderStatus `json:"status,omitempty"`
	Amount      float64       `json:"amount,omitempty"`
	Commission  float64       `json:"commission,omitempty"`
	OrderPrice  float64       `json:"orderPrice,omitempty"`
	TopUpID     string        `json:"topupId,omitempty"`
	Email       string        `json:"email,omitempty"`
	PaymentLink string        `json:"paymentLink,omitempty"`
	RawBody
}

// TopUpDepositProduct is one purchasable item in a deposit-funded top-up
// category, as listed by TopUpService.ListDepositOffers.
type TopUpDepositProduct struct {
	ID          string  `json:"id"`
	Name        string  `json:"name"`
	Price       float64 `json:"price"`
	IsAvailable bool    `json:"isAvailable"`
}

// TopUpDepositCategory is one entry of TopUpDepositCatalog.Categories.
type TopUpDepositCategory struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	// Fields: see TopUpCategory.Fields — same caveat applies.
	Fields   []json.RawMessage     `json:"fields,omitempty"`
	Products []TopUpDepositProduct `json:"products,omitempty"`
}

// TopUpDepositCatalog is the response of GET /v1/deposit/topups. Unlike the
// acquiring-funded catalog (TopUpCategory), SPEC.md explicitly documents
// this response as a "categories[]" array wrapper.
type TopUpDepositCatalog struct {
	Categories []TopUpDepositCategory `json:"categories,omitempty"`
	RawBody
}

// CreateTopUpDepositOrderRequest is the body of POST /v1/deposit/topups.
// Fields is optional here (SPEC.md marks it so for this endpoint, unlike
// the acquiring-funded one).
type CreateTopUpDepositOrderRequest struct {
	TopUpID    string            `json:"topupId"`
	CategoryID string            `json:"categoryId"`
	OrderID    string            `json:"orderId"`
	Email      string            `json:"email"`
	Fields     map[string]string `json:"fields,omitempty"`
}

// TopUpDepositOrder is a deposit-funded top-up order, returned by
// CreateDepositOrder.
type TopUpDepositOrder struct {
	OrderID    string             `json:"orderId,omitempty"`
	TopUpID    string             `json:"topupId,omitempty"`
	CategoryID string             `json:"categoryId,omitempty"`
	Price      float64            `json:"price,omitempty"`
	Status     DepositOrderStatus `json:"status,omitempty"`
	Fields     map[string]string  `json:"fields,omitempty"`
	Email      string             `json:"email,omitempty"`
	RawBody
}

// VoucherProduct is one purchasable voucher in an acquiring-funded voucher
// category, as listed by VouchersService.ListOffers.
type VoucherProduct struct {
	ID          string  `json:"id"`
	Name        string  `json:"name"`
	Price       float64 `json:"price"`
	MinPrice    float64 `json:"minPrice,omitempty"`
	IsAvailable bool    `json:"isAvailable"`
	Stock       int     `json:"stock,omitempty"`
}

// VoucherCategory is the response of GET /v3/vouchers/all — a single
// category, the same way TopUpCategory is for top-ups.
type VoucherCategory struct {
	CategoryID   string            `json:"categoryId,omitempty"`
	CategoryName string            `json:"categoryName,omitempty"`
	Type         string            `json:"type,omitempty"`
	Fields       []json.RawMessage `json:"fields,omitempty"`
	Vouchers     []VoucherProduct  `json:"vouchers,omitempty"`
	RawBody
}

// CreateVoucherOrderRequest is the body of POST /v3/vouchers.
type CreateVoucherOrderRequest struct {
	VoucherID   string  `json:"voucherId"`
	Amount      float64 `json:"amount"`
	Count       int     `json:"count"`
	OrderID     string  `json:"orderId"`
	Email       string  `json:"email"`
	Description string  `json:"description"`
}

// VoucherOrder is an acquiring-funded voucher order, returned by
// CreateOrder and GetOrder. Vouchers holds the issued codes once available
// — SPEC.md notes there is no separate redemption endpoint and codes can
// appear with up to a 10-minute delay after payment.
type VoucherOrder struct {
	OrderID     string        `json:"orderId,omitempty"`
	Status      DGOrderStatus `json:"status,omitempty"`
	Vouchers    []string      `json:"vouchers,omitempty"`
	Amount      float64       `json:"amount,omitempty"`
	OrderPrice  float64       `json:"orderPrice,omitempty"`
	Commission  float64       `json:"commission,omitempty"`
	VoucherID   string        `json:"voucherId,omitempty"`
	Count       int           `json:"count,omitempty"`
	Email       string        `json:"email,omitempty"`
	PaymentLink string        `json:"paymentLink,omitempty"`
	RawBody
}

// VoucherDepositProduct is one purchasable voucher in a deposit-funded
// voucher category, as listed by VouchersService.ListDepositOffers.
type VoucherDepositProduct struct {
	ID          string  `json:"id"`
	Name        string  `json:"name"`
	Price       float64 `json:"price"`
	Stock       int     `json:"stock,omitempty"`
	IsAvailable bool    `json:"isAvailable"`
}

// VoucherDepositCategory is one entry of VoucherDepositCatalog.Categories.
type VoucherDepositCategory struct {
	ID       string                  `json:"id"`
	Name     string                  `json:"name"`
	Products []VoucherDepositProduct `json:"products,omitempty"`
}

// VoucherDepositCatalog is the response of GET /v1/deposit/vouchers.
type VoucherDepositCatalog struct {
	Categories []VoucherDepositCategory `json:"categories,omitempty"`
	RawBody
}

// CreateVoucherDepositOrderRequest is the body of POST /v1/deposit/vouchers.
type CreateVoucherDepositOrderRequest struct {
	VoucherID  string `json:"voucherId"`
	CategoryID string `json:"categoryId"`
	Count      int    `json:"count"`
	OrderID    string `json:"orderId"`
	Email      string `json:"email"`
}

// VoucherDepositOrder is a deposit-funded voucher order, returned by
// CreateDepositOrder. Codes holds the issued voucher codes once available.
type VoucherDepositOrder struct {
	OrderID    string             `json:"orderId,omitempty"`
	VoucherID  string             `json:"voucherId,omitempty"`
	CategoryID string             `json:"categoryId,omitempty"`
	Count      int                `json:"count,omitempty"`
	Price      float64            `json:"price,omitempty"`
	Status     DepositOrderStatus `json:"status,omitempty"`
	Codes      []string           `json:"codes,omitempty"`
	Email      string             `json:"email,omitempty"`
	RawBody
}

// DepositOrder is the status of an order paid from the merchant's
// digital-goods deposit balance (GET /v1/deposit/order/{orderId}), shared
// across Steam/Top-Up/voucher deposit-funded orders. Details' shape depends
// on Type and SPEC.md notes it "may be empty"; it is kept as raw JSON
// rather than guessing per-type fields — decode it into a more specific
// type once you know Type.
type DepositOrder struct {
	OrderID string             `json:"orderId,omitempty"`
	Price   float64            `json:"price,omitempty"`
	Status  DepositOrderStatus `json:"status,omitempty"`
	Type    DepositOrderType   `json:"type,omitempty"`
	Details json.RawMessage    `json:"details,omitempty"`
	RawBody
}

// DepositBalance is the response of GET /v1/deposit/balance.
type DepositBalance struct {
	TotalBalance     float64 `json:"totalBalance"`
	FrozenBalance    float64 `json:"frozenBalance"`
	AvailableBalance float64 `json:"availableBalance"`
	RawBody
}

// -----------------------------------------------------------------------
// query-encoding helpers
// -----------------------------------------------------------------------

func addIfNotEmpty(q url.Values, key, val string) {
	if val != "" {
		q.Set(key, val)
	}
}

func addIfPositive(q url.Values, key string, val int) {
	if val > 0 {
		q.Set(key, strconv.Itoa(val))
	}
}

func addFloatPtr(q url.Values, key string, val *float64) {
	if val != nil {
		q.Set(key, formatFloat(*val))
	}
}

func addTimePtr(q url.Values, key string, val *time.Time) {
	if val != nil {
		q.Set(key, val.UTC().Format(time.RFC3339))
	}
}

func formatFloat(f float64) string {
	return strconv.FormatFloat(f, 'f', -1, 64)
}
