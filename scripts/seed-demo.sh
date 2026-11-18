#!/usr/bin/env bash
#
# Seeds demo data into a running Paw Motion AI backend:
#   owners + pets + posts + activities + a lost-pet alert,
#   partners + business profiles (which create map places) + services,
#   funded wallets + one submitted campaign (approved via SQL when possible).
#
# Prerequisites: docker compose up -d postgres && backend running on $API_BASE_URL.
# Re-runnable: logins fall back to login on conflict, and creation steps skip
# records that already exist (matched by name).
set -euo pipefail

API="${API_BASE_URL:-http://localhost:8080}"
PASSWORD='PetLife123!'
DB_CONTAINER="${DB_CONTAINER:-pawmotion-postgres}"
DB_NAME="${POSTGRES_DB:-pawmotion}"
DB_USER="${POSTGRES_USER:-pawmotion}"
CENTER_LAT=-23.5615   # Ibirapuera, São Paulo — matches the apps' fallback area
CENTER_LNG=-46.6559

say() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*" >&2; }
die() { printf '\033[1;31m[error]\033[0m %s\n' "$*" >&2; exit 1; }

command -v curl >/dev/null || die "curl is required"
command -v jq >/dev/null || die "jq is required"

req() { # method path token body [extra-header]
  local method=$1 path=$2 token=${3:-} body=${4:-} extra=${5:-}
  local args=(-sS -X "$method" "$API$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$extra" ] && args+=(-H "$extra")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}

# register_or_login EMAIL DISPLAY_NAME [partner] BUSINESS_NAME
register_or_login() {
  local email=$1 name=$2 mode=${3:-owner} business=${4:-}
  local reg_path login_path body
  if [ "$mode" = partner ]; then
    reg_path=/api/partner/auth/register; login_path=/api/partner/auth/login
    body=$(jq -nc --arg e "$email" --arg p "$PASSWORD" --arg n "$name" --arg b "$business" \
      '{email:$e,password:$p,displayName:$n,businessName:$b,description:"Demo partner seeded by seed-demo.sh"}')
  else
    reg_path=/api/auth/register; login_path=/api/auth/login
    body=$(jq -nc --arg e "$email" --arg p "$PASSWORD" --arg n "$name" '{email:$e,password:$p,displayName:$n}')
  fi
  local res status
  res=$(curl -sS -w '\n%{http_code}' -X POST "$API$reg_path" -H 'Content-Type: application/json' -d "$body")
  status=$(tail -n1 <<<"$res")
  if [ "$status" != "200" ] && [ "$status" != "201" ]; then
    res=$(curl -sS -X POST "$API$login_path" -H 'Content-Type: application/json' \
      -d "$(jq -nc --arg e "$email" --arg p "$PASSWORD" '{email:$e,password:$p}')")
  else
    res=$(sed '$d' <<<"$res")
  fi
  local token
  token=$(jq -r '.accessToken // empty' <<<"$res")
  [ -n "$token" ] || die "could not authenticate $email — $(jq -r '.detail // .title // .' <<<"$res" 2>/dev/null || echo "$res")"
  printf '%s' "$token"
}

ensure_pet() { # token name species breed size birthDate activityPreference
  local token=$1 name=$2
  local existing
  existing=$(req GET /api/pets "$token" | jq -r --arg n "$name" '.[] | select(.name == $n) | .id' | head -1)
  [ -n "$existing" ] && { echo "$existing"; return; }
  req POST /api/pets "$token" "$(jq -nc --arg n "$name" --arg s "$3" --arg b "$4" --arg z "$5" --arg d "$6" --arg a "$7" \
    '{name:$n,species:$s,breed:$b,size:$z,birthDate:$d,activityPreference:$a}')" | jq -r '.id'
}

ensure_service() { # token name description duration priceCents
  local token=$1 name=$2
  req GET /api/partner/services "$token" | jq -e --arg n "$name" '.[] | select(.name == $n)' >/dev/null && return
  req POST /api/partner/services "$token" "$(jq -nc --arg n "$name" --arg d "$3" --arg m "$4" --arg p "$5" \
    '{name:$n,description:$d,durationMinutes:($m|tonumber),priceCents:($p|tonumber),active:true}')" >/dev/null
}

upload_png() { # token  -> media id (tiny 1x1 PNG)
  local token=$1 tmp
  tmp=$(mktemp -t seedpng).png
  printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' | base64 -d > "$tmp"
  curl -sS -X POST "$API/api/media" -H "Authorization: Bearer $token" -F "file=@$tmp;type=image/png" | jq -r '.id'
  rm -f "$tmp"
}

say "Checking backend at $API"
curl -sS -o /dev/null --max-time 5 "$API/actuator/health" || curl -sS -o /dev/null --max-time 5 "$API/api/places?lat=0&lng=0" \
  || die "backend not reachable at $API — start it first (cd backend && ./gradlew bootRun)"

# ---------------------------------------------------------------- owners
say "Creating owners"
ANA=$(register_or_login ana@petlife.demo 'Ana Costa')
BRUNO=$(register_or_login bruno@petlife.demo 'Bruno Lima')
CARLA=$(register_or_login carla@petlife.demo 'Carla Souza')

say "Creating pets"
THOR=$(ensure_pet "$ANA" 'Thor' dog 'Golden Retriever' large '2021-03-15' HIGH)
MEL=$(ensure_pet "$ANA" 'Mel' cat 'Siamese' small '2022-07-02' LOW)
BIDU=$(ensure_pet "$BRUNO" 'Bidu' dog 'Beagle' medium '2020-11-20' MEDIUM)
FRED=$(ensure_pet "$CARLA" 'Fred' dog 'Bulldog' medium '2019-05-30' LOW)
echo "pets: thor=$THOR mel=$MEL bidu=$BIDU fred=$FRED"

say "Creating posts"
req POST /api/posts "$ANA" "$(jq -nc --arg p "$THOR" '{petId:$p,caption:"Thor conquered the park today — 5km of pure joy!",placeName:"Parque Ibirapuera"}')" >/dev/null
req POST /api/posts "$BRUNO" "$(jq -nc --arg p "$BIDU" '{petId:$p,caption:"Beagle mode: sniff everything, walk nowhere."}')" >/dev/null
req POST /api/posts "$CARLA" "$(jq -nc --arg p "$FRED" '{petId:$p,caption:"Fred discovered the pet-friendly café downtown. Approved."}')" >/dev/null

say "Logging activities"
req POST /api/activities "$ANA" "$(jq -nc --arg p "$THOR" '{petId:$p,type:"WALK",startedAt:"2025-01-10T08:30:00Z",durationSeconds:2700,distanceMeters:3200,latitude:-23.5615,longitude:-46.6559,notes:"Morning run around the lake"}')" >/dev/null
req POST /api/activities "$BRUNO" "$(jq -nc --arg p "$BIDU" '{petId:$p,type:"PLAY",startedAt:"2025-01-10T17:00:00Z",durationSeconds:1800,latitude:-23.5578,longitude:-46.6602,notes:"Fetch at the dog park"}')" >/dev/null
req POST /api/activities "$CARLA" "$(jq -nc --arg p "$FRED" '{petId:$p,type:"WALK",startedAt:"2025-01-11T09:15:00Z",durationSeconds:1500,distanceMeters:900,latitude:-23.5642,longitude:-46.6492}')" >/dev/null

# ---------------------------------------------------------------- partners
say "Creating partners"
SPA=$(register_or_login spa@petlife.demo 'Paula Mota' partner 'Happy Paws Spa')
VET=$(register_or_login vet@petlife.demo 'Dr. Ramos' partner 'Central Pet Vet')
CAFE=$(register_or_login cafe@petlife.demo 'Rui Alves' partner 'Café Au Au')

say "Configuring partner profiles (creates map places)"
req PATCH /api/partner/profile "$SPA" "$(jq -nc '{businessName:"Happy Paws Spa",category:"GROOMING",description:"Baths, trims and spa day for dogs and cats.",phone:"(11) 99988-1122",address:"Rua das Flores, 120 - Ibirapuera, São Paulo",latitude:-23.5580,longitude:-46.6500,openingHours:"Mon–Sat · 9:00–19:00"}')" >/dev/null
req PATCH /api/partner/profile "$VET" "$(jq -nc '{businessName:"Central Pet Vet",category:"VET",description:"24h veterinary clinic with preventive care plans.",phone:"(11) 98877-3344",address:"Av. Paulista, 900 - Bela Vista, São Paulo",latitude:-23.5650,longitude:-46.6520,openingHours:"24/7"}')" >/dev/null
req PATCH /api/partner/profile "$CAFE" "$(jq -nc '{businessName:"Café Au Au",category:"CAFE",description:"Pet-friendly café — puppuccino on the house.",phone:"(11) 97766-5566",address:"Rua Oscar Freire, 450 - Jardins, São Paulo",latitude:-23.5625,longitude:-46.6690,openingHours:"Daily · 8:00–20:00"}')" >/dev/null

say "Creating partner services"
ensure_service "$SPA" 'Full groom' 'Bath, haircut, nail trim and ear cleaning' '90' '12000'
ensure_service "$SPA" 'Spa day' 'Hydro bath, massage and coat treatment' '120' '18000'
ensure_service "$VET" 'Check-up' 'General health assessment' '45' '15000'
ensure_service "$VET" 'Vaccination' 'Core vaccines, includes the shot card update' '30' '8000'
ensure_service "$CAFE" 'Puppuccino hour' 'Reserved pet table + free puppuccino' '60' '2500'

say "Funding wallets via fake checkout"
for TOKEN in "$SPA" "$VET"; do
  TXN=$(req POST /api/partner/billing/purchases "$TOKEN" "$(jq -nc '{amount:1000,method:"PIX"}')" "Idempotency-Key: seed-$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || echo $RANDOM$RANDOM)")
  TXN_ID=$(jq -r '.id // empty' <<<"$TXN")
  if [ -n "$TXN_ID" ]; then
    req POST "/api/partner/billing/purchases/$TXN_ID/fake-complete" "$TOKEN" >/dev/null
    echo "funded wallet for ${TXN_ID:0:8}… (1000 BRL → credits)"
  else
    warn "purchase failed — $(jq -r '.detail // .' <<<"$TXN" 2>/dev/null || echo "$TXN")"
  fi
done

say "Creating campaigns"
SPA_MEDIA=$(upload_png "$SPA")
VET_MEDIA=$(upload_png "$VET")

SPA_CAMPAIGN=$(req GET /api/partner/v2/campaigns "$SPA" | jq -r '.[] | select(.title == "Spa Week — 30% off") | .id' | head -1)
if [ -z "$SPA_CAMPAIGN" ]; then
  SPA_CAMPAIGN=$(req POST /api/partner/campaigns "$SPA" \
    "$(jq -nc '{title:"Spa Week — 30% off",description:"Book a full groom this week and get 30% off spa day upgrades.",targetAudience:"Dogs and cats within 8km",callToAction:"Book now"}')" \
    "Idempotency-Key: seed-spa-campaign" | jq -r '.id')
fi
SERVICE_ID=$(req GET /api/partner/services "$SPA" | jq -r '.[] | select(.name == "Full groom") | .id' | head -1)
PATCH_RES=$(req PATCH "/api/partner/campaigns/$SPA_CAMPAIGN" "$SPA" "$(jq -nc --arg m "$SPA_MEDIA" --arg s "$SERVICE_ID" \
  '{mediaAssetId:$m,ctaType:"BOOKING",serviceId:$s,formats:["FEED_CARD","MAP_PIN","DISCOVER_HIGHLIGHT","NEARBY_PROMO"],targetSpecies:["DOG","CAT"],targetLatitude:-23.5615,targetLongitude:-46.6559,targetRadiusMeters:8000}')")
jq -e '.id' <<<"$PATCH_RES" >/dev/null 2>&1 || warn "campaign patch skipped (status=$(jq -r '.status // "?"' <<<"$PATCH_RES" 2>/dev/null)) — SQL tail will fix formats"

# second campaign stays a draft — exercises the free-draft flow (no credits needed)
VET_CAMPAIGN=$(req GET /api/partner/v2/campaigns "$VET" | jq -r '.[] | select(.title == "Vaccine drive") | .id' | head -1)
if [ -z "$VET_CAMPAIGN" ]; then
  VET_CAMPAIGN=$(req POST /api/partner/campaigns "$VET" \
    "$(jq -nc '{title:"Vaccine drive",description:"Annual vaccines with 20% off this month.",targetAudience:"All pets",callToAction:"Learn more"}')" \
    "Idempotency-Key: seed-vet-campaign" | jq -r '.id')
fi

say "Submitting spa campaign for review (debits credits)"
SPA_STATUS=$(req GET "/api/partner/campaigns/$SPA_CAMPAIGN" "$SPA" | jq -r '.status // empty')
if [ "$SPA_STATUS" = "DRAFT" ]; then
  SUBMIT=$(req POST "/api/partner/campaigns/$SPA_CAMPAIGN/submit" "$SPA")
  jq -r '"campaign " + .id + " → " + .status' <<<"$SUBMIT" 2>/dev/null || warn "submit failed: $SUBMIT"
else
  echo "campaign $SPA_CAMPAIGN already $SPA_STATUS — skipping submit"
fi

# ---------------------------------------------------------------- social/map
say "Check-ins at partner places"
PLACES=$(req GET "/api/places?lat=$CENTER_LAT&lng=$CENTER_LNG&radius=10000" "$ANA" | jq -r '.items[]?.id // empty')
FIRST_PLACE=$(head -n1 <<<"$PLACES")
[ -n "$FIRST_PLACE" ] && req POST "/api/places/$FIRST_PLACE/check-ins" "$ANA" "$(jq -nc --arg p "$THOR" '{petId:$p}')" >/dev/null

say "Lost-pet alert + sighting"
ALERT_ID=$(req GET "/api/lost-pets?lat=$CENTER_LAT&lng=$CENTER_LNG&radius=10000" "$ANA" | jq -r '.[] | select(.status == "ACTIVE") | .id' | head -1)
if [ -z "$ALERT_ID" ]; then
  ALERT_ID=$(req POST /api/lost-pets "$ANA" "$(jq -nc --arg p "$MEL" \
    '{petId:$p,description:"Mel slipped out near the park — grey Siamese, blue collar, very shy.",contactPhone:"(11) 98811-2233",latitude:-23.5600,longitude:-46.6530,lastSeenAt:"2025-01-12T07:45:00Z",radiusMeters:3000}')" | jq -r '.id')
fi
[ -n "$ALERT_ID" ] && req POST "/api/lost-pets/$ALERT_ID/sightings" "$BRUNO" \
  "$(jq -nc '{latitude:-23.5590,longitude:-46.6510,seenAt:"2025-01-12T09:00:00Z",note:"Saw a grey cat near the pet shop entrance"}')" >/dev/null

say "Playdate invite"
req POST /api/playdates "$ANA" "$(jq -nc --arg p "$THOR" --arg f "$FRED" \
  '{petId:$p,mode:"NOW",inviteePetIds:[$f],latitude:-23.5615,longitude:-46.6559,locationName:"Ibirapuera dog park"}')" >/dev/null || true

# ---------------------------------------------------------------- SQL tail (no API equivalents)
say "Applying SQL tail (owner locations, verified partners, campaign approval)"
SQL="
UPDATE owners o SET location = ST_SetSRID(ST_MakePoint(x.lng, x.lat), 4326)::geography
FROM (VALUES ('ana@petlife.demo', -23.5615, -46.6559), ('bruno@petlife.demo', -23.5578, -46.6602), ('carla@petlife.demo', -23.5642, -46.6492)) AS x(email, lat, lng)
WHERE o.email = x.email AND o.location IS NULL;
UPDATE partner_profiles pp SET verified = TRUE, verified_at = now() FROM owners o WHERE pp.partner_id = o.id AND o.email IN ('spa@petlife.demo','vet@petlife.demo');
UPDATE partner_campaigns SET status = 'ACTIVE', reviewed_at = now(), updated_at = now() WHERE id = '$SPA_CAMPAIGN' AND status = 'IN_REVIEW';
UPDATE places SET rating = 4.8, rating_count = 32 WHERE partner_id IS NOT NULL AND rating IS NULL;
"
# Re-apply campaign fields directly when the campaign is no longer editable via the API (ACTIVE).
if [ -n "$SPA_CAMPAIGN" ] && [ -n "$SPA_MEDIA" ] && [ "$SPA_MEDIA" != "null" ] && [ -n "$SERVICE_ID" ]; then
  SQL+="
UPDATE partner_campaigns SET media_asset_id = '$SPA_MEDIA'::uuid, cta_type = 'BOOKING', service_id = '$SERVICE_ID'::uuid,
    formats = '{FEED_CARD,MAP_PIN,DISCOVER_HIGHLIGHT,NEARBY_PROMO}', target_species = '{DOG,CAT}',
    target_location = ST_SetSRID(ST_MakePoint(-46.6559, -23.5615), 4326)::geography, target_radius_m = 8000, updated_at = now()
WHERE id = '$SPA_CAMPAIGN' AND status = 'ACTIVE';
"
fi
if docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "$SQL" >/dev/null 2>&1; then
  echo "SQL tail applied"
elif command -v psql >/dev/null && PGPASSWORD="${POSTGRES_PASSWORD:-devpw-local-only-9f3a}" psql -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" -c "$SQL" >/dev/null 2>&1; then
  echo "SQL tail applied"
else
  warn "could not reach Postgres — run manually:
$SQL"
fi

say "Done! Demo logins (password: $PASSWORD)"
cat <<EOF
  Owners:   ana@petlife.demo (Thor + Mel, lost-pet alert), bruno@petlife.demo (Bidu), carla@petlife.demo (Fred)
  Partners: spa@petlife.demo (Happy Paws Spa, active campaign, funded wallet)
            vet@petlife.demo (Central Pet Vet, draft campaign)
            cafe@petlife.demo (Café Au Au)
EOF
