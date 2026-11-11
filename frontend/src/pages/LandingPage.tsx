import { Link } from 'react-router-dom';
import { Badge, BrandLogo, Button, SponsoredLabel } from '../components/ui';
import { PawIcon } from '../components/petVisuals';

const features = [
  { emoji: '🛂', title: 'Health passport', text: 'Vaccines, deworming and vet visits always at hand. Get reminders before anything is due.' },
  { emoji: '🗺️', title: 'Pet-friendly map', text: 'Parks, vets and cafés near you, with check-ins and real reviews from other pet parents.' },
  { emoji: '🚨', title: 'Lost pet alerts', text: 'Geolocated alerts reach the right people, right where your pet was last seen.' },
  { emoji: '🏅', title: 'Gamified adventures', text: 'Streaks, badges, levels and weekly challenges turn every walk into an achievement.' },
  { emoji: '✨', title: 'AI that acts', text: 'PetLife AI writes captions, plans weekends and books real appointments — only with your approval.' },
  { emoji: '👥', title: 'Communities', text: 'Groups by breed, neighborhood and cause, with local events and meetups.' },
];

const plans = [
  { name: 'Free', price: '$0', period: 'forever', items: ['Full feed, stories and reactions', 'Pet profile with health passport', 'Map, lost-pet alerts and playdates', '10 AI messages per month'], cta: 'Create free profile' },
  { name: 'Premium', price: '$7.90', period: 'per month', items: ['Everything in Free', 'Unlimited PetLife AI', 'Advanced stats and activity insights', 'Priority profile highlight', 'Early access to new features'], cta: 'Go Premium', highlight: true },
];

const faqs = [
  { q: 'Is PetLife free?', a: 'Yes. The Free plan includes the full social experience — feed, map, communities, health passport and lost-pet alerts. Premium adds unlimited AI and advanced insights.' },
  { q: 'Can PetLife AI book appointments for me?', a: 'It suggests and prepares bookings with partner businesses — vets, groomers, hotels — but only executes after you confirm inside the chat.' },
  { q: 'How do lost-pet alerts work?', a: 'You share where your pet was last seen. The alert reaches users within the radius you choose, and sightings update the search map in real time.' },
  { q: 'I have a pet business. How do I advertise?', a: 'Create a verified partner profile and launch campaigns — sponsored feed cards, promoted map pins and "Near you" offers — with full metrics per campaign.' },
];

function PhonePreview() {
  return (
    <div className="mx-auto w-full max-w-[300px] rounded-[36px] border-[10px] border-teal-dark bg-cream shadow-raised">
      <div className="rounded-[26px] p-3">
        <div className="flex items-center justify-between px-1 pb-2">
          <BrandLogo className="text-label" />
          <Badge tone="accent">FEED</Badge>
        </div>
        <div className="mb-3 flex gap-2 overflow-hidden px-1">
          {['Luna', 'Thor', 'Bidu', 'Mia'].map((n, i) => (
            <div key={n} className="flex flex-col items-center gap-1">
              <span className={`grid h-12 w-12 place-items-center rounded-full border-[3px] ${i === 0 ? 'border-mint shadow-[0_0_10px_rgba(158,216,204,0.9)]' : 'border-teal'} bg-teal-light text-teal`}><PawIcon className="h-5 w-5" /></span>
              <span className="text-[10px] font-bold text-ink">{n}</span>
            </div>
          ))}
        </div>
        <div className="rounded-2xl bg-white p-3 shadow-card">
          <div className="flex items-center gap-2"><span className="grid h-8 w-8 place-items-center rounded-full bg-teal text-[10px] font-black text-white">LU</span><div><p className="text-[12px] font-extrabold">Luna</p><p className="text-[10px] text-muted">Beach walk · 2h</p></div></div>
          <div className="mt-2 grid h-28 place-items-center rounded-xl bg-teal-light text-3xl">🐕</div>
          <div className="mt-2 flex gap-2 text-[10px] font-extrabold text-ink"><span>🐾 124</span><span>😂 8</span><span>❤️ 56</span></div>
        </div>
        <div className="mt-3 rounded-2xl border-2 border-amber bg-white p-3">
          <SponsoredLabel />
          <p className="mt-1 text-[11px] font-extrabold text-teal-dark">20% off grooming this week</p>
        </div>
        <div className="mt-3 flex items-center justify-around rounded-2xl bg-white py-2 text-[10px] font-extrabold text-muted shadow-card">
          <span className="text-teal">Feed</span><span>Map</span>
          <span className="-mt-6 grid h-10 w-10 place-items-center rounded-full bg-coral text-lg font-black text-coral-ink shadow-coral">+</span>
          <span>AI</span><span>Profile</span>
        </div>
      </div>
    </div>
  );
}

function AiDemo() {
  const suggestions = [
    { emoji: '🥾', title: 'Mountain trail', meta: '3.0 km · 10 min drive' },
    { emoji: '🥞', title: 'Pet-friendly brunch', meta: 'The Muzzle Café · 11am' },
    { emoji: '🛁', title: 'Bath & groom', meta: 'The Paw Resort · 3pm' },
  ];
  return (
    <div className="pl-card mx-auto w-full max-w-md p-5">
      <div className="flex items-center gap-3 border-b border-line pb-3">
        <span className="grid h-11 w-11 place-items-center rounded-full bg-teal text-white"><PawIcon className="h-5 w-5" /></span>
        <div><p className="font-extrabold text-teal-dark">PetLife AI</p><p className="text-label text-muted">Your assistant for Luna's world</p></div>
      </div>
      <div className="mt-4 ml-auto w-fit max-w-[85%] rounded-2xl rounded-tr-sm bg-teal px-4 py-3 text-body font-semibold text-white">What can Luna do this weekend? 🐾</div>
      <div className="mt-3 w-fit max-w-[90%] rounded-2xl rounded-tl-sm bg-sand px-4 py-3 text-body text-ink">Sunny and 79°F on Saturday — I picked 3 ideas for you two. Tap <b>Book</b> to lock it in.</div>
      <div className="mt-3 space-y-2">
        {suggestions.map((s) => (
          <div key={s.title} className="flex items-center gap-3 rounded-xl border border-line bg-white p-3">
            <span className="text-2xl" aria-hidden="true">{s.emoji}</span>
            <div className="min-w-0 flex-1"><p className="font-extrabold text-ink">{s.title}</p><p className="text-caption text-muted">{s.meta}</p></div>
            <span className="rounded-pill bg-coral px-3 py-1.5 text-caption font-black text-coral-ink">Book</span>
          </div>
        ))}
      </div>
      <p className="mt-4 text-caption text-muted">PetLife AI only books, schedules or publishes after your confirmation. It is not a vet — for health concerns, see a professional.</p>
    </div>
  );
}

export function LandingPage() {
  const link = 'text-label font-extrabold text-teal hover:text-teal-dark';
  return (
    <div className="min-h-screen bg-cream">
      <header className="sticky top-0 z-40 border-b border-line bg-cream/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <BrandLogo />
          <nav aria-label="Sections" className="hidden items-center gap-6 md:flex">
            <a href="#features" className={link}>Features</a>
            <a href="#ai" className={link}>PetLife AI</a>
            <a href="#business" className={link}>For business</a>
            <a href="#plans" className={link}>Plans</a>
            <a href="#faq" className={link}>FAQ</a>
          </nav>
          <div className="flex items-center gap-2">
            <Link to="/login" className="hidden min-h-11 items-center rounded-pill px-4 text-label font-extrabold text-teal hover:bg-teal-light sm:inline-flex">Sign in</Link>
            <Link to="/signup" className="inline-flex min-h-11 items-center rounded-pill bg-coral px-5 text-label font-black text-coral-ink shadow-coral">Create profile</Link>
          </div>
        </div>
      </header>

      <section className="mx-auto grid max-w-6xl items-center gap-10 px-4 pb-16 pt-14 md:grid-cols-2 md:pt-20">
        <div>
          <Badge tone="accent">The social network for pets</Badge>
          <h1 className="mt-4 text-display font-black leading-[1.05] text-teal-dark">Pets deserve a social life too</h1>
          <p className="mt-4 max-w-lg text-body-lg text-muted">Create a profile for your pet, turn walks and playdates into posts, keep the health passport organized and discover a pet-friendly world near you.</p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/signup"><Button size="lg">Create your pet's profile</Button></Link>
            <Link to="/login"><Button size="lg" variant="outline">I already have an account</Button></Link>
          </div>
          <p className="mt-4 text-label text-muted">Free to start · Available on web and mobile</p>
        </div>
        <PhonePreview />
      </section>

      <section id="features" className="bg-sand/60 py-16">
        <div className="mx-auto max-w-6xl px-4">
          <h2 className="text-heading">Everything your pet needs, in one place</h2>
          <p className="mt-2 max-w-2xl text-body text-muted">Feed, map, health, achievements and community — built for the people who love pets.</p>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="pl-card p-6">
                <span className="text-3xl" aria-hidden="true">{f.emoji}</span>
                <h3 className="mt-3 text-title">{f.title}</h3>
                <p className="mt-2 text-body text-muted">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="ai" className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 md:grid-cols-2">
        <div>
          <Badge tone="accent">PetLife AI</Badge>
          <h2 className="mt-3 text-heading">An assistant that doesn't just suggest — it acts</h2>
          <p className="mt-3 max-w-lg text-body text-muted">Ask for weekend plans, pet-friendly spots or captions for your posts. PetLife AI prepares the booking, reminder or caption and only executes after you confirm.</p>
          <ul className="mt-5 space-y-2 text-body font-semibold text-ink">
            <li>📅 Books partner services (vets, groomers, hotels)</li>
            <li>🔔 Schedules health reminders</li>
            <li>✍️ Writes captions in your pet's voice</li>
            <li>🐶 Suggests playdates with nearby pets</li>
          </ul>
        </div>
        <AiDemo />
      </section>

      <section id="business" className="bg-teal py-16 text-white">
        <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 md:grid-cols-2">
          <div>
            <Badge>FOR BUSINESS</Badge>
            <h2 className="mt-3 text-heading text-white">Reach pet parents near your door</h2>
            <p className="mt-3 max-w-lg text-body text-white/85">Verified business profiles, geolocated promotions and in-app reservations. Measure impressions, clicks and bookings per campaign.</p>
            <ul className="mt-5 space-y-2 text-body font-semibold">
              <li>✅ Verified profile builds trust</li>
              <li>📍 Sponsored feed cards and promoted map pins</li>
              <li>📊 Metrics: impressions, clicks, bookings, cost per result</li>
            </ul>
            <Link to="/partner" className="mt-6 inline-flex min-h-12 items-center rounded-pill bg-coral px-6 text-body font-black text-coral-ink shadow-coral">Open partner portal</Link>
          </div>
          <div className="pl-card p-6 text-ink">
            <div className="flex items-center justify-between"><p className="font-extrabold text-teal-dark">Campaign snapshot</p><Badge tone="success">ACTIVE</Badge></div>
            <div className="mt-4 grid grid-cols-3 gap-3 text-center">
              {[['Impressions', '12.4k'], ['Clicks', '846'], ['Bookings', '132']].map(([l, v]) => (
                <div key={l} className="rounded-xl bg-sand p-3"><p className="text-[11px] font-bold uppercase text-muted">{l}</p><p className="text-title font-black text-teal-dark">{v}</p></div>
              ))}
            </div>
            <div className="mt-4 rounded-xl border-2 border-amber bg-white p-4">
              <SponsoredLabel />
              <p className="mt-2 font-extrabold text-teal-dark">20% off grooming this week</p>
              <p className="text-body text-muted">The Paw Resort · 0.6 km away</p>
            </div>
          </div>
        </div>
      </section>

      <section id="plans" className="mx-auto max-w-6xl px-4 py-16">
        <h2 className="text-center text-heading">Plans for every pack</h2>
        <div className="mx-auto mt-8 grid max-w-3xl gap-5 md:grid-cols-2">
          {plans.map((p) => (
            <div key={p.name} className={`pl-card flex flex-col p-6 ${p.highlight ? 'ring-2 ring-coral' : ''}`}>
              {p.highlight ? <Badge tone="accent">MOST POPULAR</Badge> : null}
              <h3 className="mt-3 text-title">{p.name}</h3>
              <p className="mt-2"><span className="text-display font-black text-teal-dark">{p.price}</span> <span className="text-body text-muted">{p.period}</span></p>
              <ul className="mt-4 flex-1 space-y-2 text-body text-ink">{p.items.map((i) => <li key={i}>✓ {i}</li>)}</ul>
              <Link to="/signup" className="mt-5"><Button variant={p.highlight ? 'primary' : 'outline'} className="w-full">{p.cta}</Button></Link>
            </div>
          ))}
        </div>
      </section>

      <section id="faq" className="bg-sand/60 py-16">
        <div className="mx-auto max-w-3xl px-4">
          <h2 className="text-center text-heading">Frequently asked questions</h2>
          <div className="mt-8 space-y-3">
            {faqs.map((f) => (
              <details key={f.q} className="pl-card group p-5">
                <summary className="cursor-pointer list-none font-extrabold text-teal-dark">{f.q}<span className="float-right text-teal transition group-open:rotate-45">+</span></summary>
                <p className="mt-2 text-body text-muted">{f.a}</p>
              </details>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-4xl px-4 py-16 text-center">
        <h2 className="text-heading">Your pet's next adventure starts here</h2>
        <p className="mx-auto mt-3 max-w-xl text-body text-muted">Join PetLife free, create your pet's profile and start earning your first badges today.</p>
        <Link to="/signup" className="mt-6 inline-block"><Button size="lg">Create free profile</Button></Link>
      </section>

      <footer className="border-t border-line bg-white py-10">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 text-label text-muted md:flex-row">
          <BrandLogo />
          <nav aria-label="Footer" className="flex gap-5">
            <a href="#features" className={link}>Features</a>
            <a href="#plans" className={link}>Plans</a>
            <Link to="/partner" className={link}>For business</Link>
            <Link to="/login" className={link}>Sign in</Link>
          </nav>
          <p>© {new Date().getFullYear()} PetLife. Made for pets and their humans.</p>
        </div>
      </footer>
    </div>
  );
}
