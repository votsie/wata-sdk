package wata

import "context"

// TransactionIterator ranges over every transaction matching a search,
// fetching additional pages from GET /api/h2h/v2/transactions as needed and
// threading CursorID, CursorAmount and CursorDate between requests
// automatically.
//
// This targets Go 1.21, the minimum version SPEC.md asks every
// implementation to support, rather than the range-over-func iterators
// (iter.Seq2) added in Go 1.23. An explicit type with Next/Err is the
// pattern that works unmodified back to 1.21 and is what the standard
// library itself used before range-over-func existed (bufio.Scanner,
// sql.Rows, database/sql). On Go 1.23+, a caller can still get a
// range-friendly loop with a two-line local adapter using this iterator's
// Next/Transaction/Err methods; adding a native iter.Seq2 method here would
// either raise this module's minimum Go version past what SPEC.md asks for,
// or require a build-tag-gated file — not worth it for a single loop shape.
//
// Usage:
//
//	it := client.Acquiring.Transactions(ctx, wata.SearchTransactionsParams{})
//	for it.Next() {
//	    tx := it.Transaction()
//	    // ...
//	}
//	if err := it.Err(); err != nil {
//	    // handle error
//	}
type TransactionIterator struct {
	ctx    context.Context
	svc    *AcquiringService
	params SearchTransactionsParams

	items   []Transaction
	idx     int
	cur     Transaction
	started bool
	noMore  bool
	err     error
}

func newTransactionIterator(ctx context.Context, svc *AcquiringService, params SearchTransactionsParams) *TransactionIterator {
	return &TransactionIterator{ctx: ctx, svc: svc, params: params}
}

// Next advances the iterator and reports whether a transaction is
// available via Transaction. It returns false both when iteration is
// exhausted and when an error occurred — call Err after a false return to
// tell the two apart.
func (it *TransactionIterator) Next() bool {
	if it.err != nil {
		return false
	}
	for it.idx >= len(it.items) {
		if it.started && it.noMore {
			return false
		}

		page, err := it.svc.SearchTransactions(it.ctx, it.params)
		it.started = true
		if err != nil {
			it.err = err
			return false
		}

		it.items = page.Items
		it.idx = 0
		it.noMore = !page.HasNextPage
		if page.HasNextPage {
			it.params.CursorID = page.NextCursorID
			it.params.CursorAmount = page.NextCursorAmount
			it.params.CursorDate = page.NextCursorDate
		}

		if len(it.items) == 0 && it.noMore {
			return false
		}
	}

	it.cur = it.items[it.idx]
	it.idx++
	return true
}

// Transaction returns the transaction produced by the most recent call to
// Next that returned true.
func (it *TransactionIterator) Transaction() Transaction { return it.cur }

// Err returns the first error encountered while fetching pages, if any.
func (it *TransactionIterator) Err() error { return it.err }
