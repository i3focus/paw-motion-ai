import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, createPurchase, getBillingPricing, getWallet, getWalletEntries, listPurchases } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorMessage, SectionHeader, StatCard, TextInput } from '../components/ui';
import type { BillingPricing, LedgerEntryPage, PaymentTransaction, Wallet } from '../types';

function money(currency: string, value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(value);
}

export function PartnerBillingPage() {
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [pricing, setPricing] = useState<BillingPricing | null>(null);
  const [purchases, setPurchases] = useState<PaymentTransaction[]>([]);
  const [statement, setStatement] = useState<LedgerEntryPage | null>(null);
  const [statementAccountId, setStatementAccountId] = useState<string | null>(null);
  const [statementLoading, setStatementLoading] = useState(false);
  const [statementPage, setStatementPage] = useState(0);
  const [statementError, setStatementError] = useState<string | null>(null);
  const [amount, setAmount] = useState('50');
  const [method, setMethod] = useState<'PIX' | 'CARD'>('PIX');
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const totalBalance = wallet?.totalBalance ?? 0;

  async function loadStatement(accountId: string, page = 0) {
    setStatementLoading(true);
    setStatementError(null);
    try {
      const result = await getWalletEntries(accountId, page);
      setStatement(result);
      setStatementAccountId(accountId);
      setStatementPage(result.page);
    } catch (caught) {
      setStatement(null);
      setStatementAccountId(accountId);
      setStatementError(caught instanceof ApiError ? caught.message : 'Unable to load ledger statement');
    } finally {
      setStatementLoading(false);
    }
  }

  async function refresh() {
    const [walletResponse, pricingResponse, purchaseResponse] = await Promise.all([
      getWallet(), getBillingPricing(), listPurchases(),
    ]);
    setWallet(walletResponse);
    setPricing(pricingResponse);
    setPurchases(purchaseResponse);
    const preferredAccount = walletResponse.accounts.find((account) => account.code === 'MAIN') ?? walletResponse.accounts[0];
    if (preferredAccount) {
      await loadStatement(preferredAccount.id, 0);
    } else {
      setStatement(null);
      setStatementAccountId(null);
      setStatementPage(0);
      setStatementError(null);
    }
  }

  useEffect(() => {
    refresh().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load billing'));
  }, []);

  async function changeStatementPage(delta: number) {
    if (!statementAccountId || !statement) {
      return;
    }
    await loadStatement(statementAccountId, statementPage + delta);
  }

  async function handlePurchase(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    const value = Number(amount);
    if (!Number.isFinite(value) || !Number.isInteger(value) || value < (pricing?.minPurchaseAmount ?? 1)) {
      setError(`Enter a whole amount of at least ${pricing?.minPurchaseAmount ?? 1} ${pricing?.currency ?? ''}`.trim());
      return;
    }
    setSubmitting(true);
    try {
      const purchase = await createPurchase({ amount: value, method });
      const fakeCheckout = purchase.provider === 'fake' || (purchase.providerCheckoutUrl ?? '').includes('/billing/fake');
      if (!fakeCheckout && purchase.providerCheckoutUrl && /^https?:\/\//i.test(purchase.providerCheckoutUrl)) {
        window.location.assign(purchase.providerCheckoutUrl);
        return;
      }
      setNotice(`Purchase ${purchase.id} is pending confirmation.`);
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to create purchase');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Credits & Billing" subtitle="Prepaid credits power campaign creation. Campaigns debit credits when created and refund prorated credits when archived." />
      {wallet?.status === 'BLOCKED' ? <ErrorMessage message={`Wallet blocked: ${wallet.blockedReason ?? 'contact support'}`} /> : null}
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Available credits" value={String(totalBalance)} />
        <StatCard label="Main account" value={String(wallet?.accounts.find((account) => account.code === 'MAIN')?.balance ?? 0)} />
        <StatCard label="Campaign cost" value={pricing ? `${pricing.campaignCreationCostCredits} credits` : '—'} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        <Card title="Buy credits" description={pricing ? `1 ${pricing.currency} = ${pricing.creditsPerCurrencyUnit} credits. Campaign creation costs ${pricing.campaignCreationCostCredits} credits.` : 'Loading pricing…'}>
          <form onSubmit={handlePurchase} className="space-y-4">
            <TextInput label={`Amount (${pricing?.currency ?? 'BRL'})`} required type="number" min={pricing?.minPurchaseAmount} max={pricing?.maxPurchaseAmount} step={1} value={amount} onChange={(event) => setAmount(event.target.value)} />
            <div className="flex gap-2">
              {(['PIX', 'CARD'] as const).map((option) => (
                <button key={option} type="button" onClick={() => setMethod(option)} className={`flex-1 rounded-2xl border px-3 py-2 text-sm font-semibold ${method === option ? 'border-primary bg-primary text-white' : 'border-cream-300 text-cream-700'}`}>
                  {option}
                </button>
              ))}
            </div>
            <ErrorMessage message={error} />
            {notice ? <p className="rounded-2xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{notice}</p> : null}
            <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Creating checkout…' : 'Buy credits'}</Button>
          </form>
        </Card>

        <Card title="Wallet accounts" description="MAIN is spent first, then BONUS, then PARTNER promotional credits.">
          <div className="space-y-3">
            {(wallet?.accounts ?? []).map((account) => (
              <div key={account.id} className="flex items-center justify-between gap-3 rounded-2xl border border-cream-200 bg-cream-50 px-4 py-3">
                <div>
                  <p className="font-semibold text-cream-900">{account.code}</p>
                  <p className="text-xs text-cream-500">{account.status}</p>
                </div>
                <div className="flex items-center gap-3">
                  <p className="text-lg font-bold text-primary">{account.balance} credits</p>
                  <Button type="button" variant="ghost" onClick={() => loadStatement(account.id, 0)} disabled={statementLoading}>
                    Statement
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card
        title="Ledger statement"
        description={statementAccountId
          ? `${wallet?.accounts.find((account) => account.id === statementAccountId)?.code ?? 'Account'} · ${statement?.totalElements ?? 0} movements`
          : 'Select an account to view credit and debit history.'}
      >
        {statementError ? (
          <ErrorMessage message={statementError} />
        ) : statementLoading ? (
          <p className="text-sm text-cream-500">Loading statement…</p>
        ) : !statement || statement.content.length === 0 ? (
          <EmptyState title="No ledger movements" description="Credits and debits will appear here." />
        ) : (
          <div className="space-y-3">
            {statement.content.map((entry) => (
              <div key={entry.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-cream-200 px-4 py-3">
                <div>
                  <div className="flex items-center gap-2">
                    <Badge tone={entry.direction === 'CREDIT' ? 'success' : 'accent'}>{entry.direction}</Badge>
                    <p className="font-semibold text-cream-900">{entry.referenceType}</p>
                  </div>
                  <p className="mt-1 text-xs text-cream-500">{entry.description ?? 'Ledger movement'} · balance after {entry.balanceAfter} · {new Date(entry.occurredAt).toLocaleString()}</p>
                </div>
                <p className={`text-lg font-bold ${entry.direction === 'CREDIT' ? 'text-emerald-700' : 'text-primary'}`}>
                  {entry.direction === 'CREDIT' ? '+' : '-'}{entry.amount}
                </p>
              </div>
            ))}
            {statement.totalPages > 1 ? (
              <div className="flex items-center justify-between gap-3 pt-2">
                <Button type="button" variant="ghost" disabled={statementLoading || statementPage === 0} onClick={() => changeStatementPage(-1)}>Previous</Button>
                <p className="text-xs text-cream-500">Page {statementPage + 1} of {statement.totalPages}</p>
                <Button type="button" variant="ghost" disabled={statementLoading || statementPage >= statement.totalPages - 1} onClick={() => changeStatementPage(1)}>Next</Button>
              </div>
            ) : null}
          </div>
        )}
      </Card>

      <Card title="Payment history" description="Hosted checkout purchases and their confirmation status.">
        {purchases.length === 0 ? (
          <EmptyState title="No purchases yet" description="Buy credits to create campaigns." />
        ) : (
          <div className="space-y-3">
            {purchases.map((purchase) => (
              <div key={purchase.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-cream-200 px-4 py-3">
                <div>
                  <p className="font-semibold text-cream-900">{money(purchase.currency, purchase.amount)}</p>
                  <p className="text-xs text-cream-500">{purchase.expectedCredits} credits · {purchase.method ?? purchase.provider} · {new Date(purchase.createdAt).toLocaleString()}</p>
                </div>
                <Badge tone={purchase.status === 'CONFIRMED' ? 'success' : purchase.status === 'PENDING' ? 'accent' : 'neutral'}>{purchase.status}</Badge>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
