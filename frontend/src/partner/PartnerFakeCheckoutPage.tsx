import { useEffect, useState } from 'react';
import { completeFakePurchase } from '../api/client';
import { Button, Card, ErrorMessage } from '../components/ui';

type Status = 'loading' | 'success' | 'error';

export function PartnerFakeCheckoutPage() {
  const [status, setStatus] = useState<Status>('loading');
  const [message, setMessage] = useState<string | null>(null);
  const transactionId = new URLSearchParams(window.location.search).get('transactionId');

  useEffect(() => {
    if (!transactionId) {
      setStatus('error');
      setMessage('Missing transaction id');
      return;
    }
    completeFakePurchase(transactionId)
      .then(() => setStatus('success'))
      .catch((caught) => {
        setStatus('error');
        setMessage(caught instanceof Error ? caught.message : 'Unable to complete fake payment');
      });
  }, [transactionId]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-cream-50 px-4">
      <Card className="w-full max-w-lg p-8 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cream-500">Fake checkout</p>
        <h1 className="mt-3 font-display text-3xl font-bold text-cream-900">Simulated payment</h1>
        {status === 'loading' ? <p className="mt-4 text-cream-600">Confirming payment and crediting your wallet…</p> : null}
        {status === 'success' ? <p className="mt-4 text-cream-600">Payment confirmed. Credits were added to your MAIN account.</p> : null}
        <ErrorMessage message={message} />
        {status !== 'loading' ? (
          <Button type="button" className="mt-6" onClick={() => { window.location.href = '/partner'; }}>
            Back to partner portal
          </Button>
        ) : null}
      </Card>
    </main>
  );
}
