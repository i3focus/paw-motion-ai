#!/usr/bin/env bash
#
# Seeds demo data into a running Paw Motion AI backend:
#   ~100 owners (3 named + generated), a pet for each, posts + activities,
#   12 partner businesses with map places + services across São Paulo,
#   funded wallets + campaigns, a lost-pet alert, check-ins and a playdate.
#
# Prerequisites: docker compose up -d postgres && backend running on $API_BASE_URL.
# Re-runnable: logins fall back to login on conflict, creation steps skip
# records that already exist (matched by name/title), and posts/activities are
# only created for pets that did not exist yet.
#
# Note: the backend rate-limits auth endpoints to 10 requests/minute per IP —
# the script waits out 429s, so a fresh 100-owner run takes ~10-15 minutes.
#
# Safety guards (run before any network call):
#   SEED_ALLOW_LOCAL=1   required, explicit opt-in to seed at all.
#   SEED_ALLOW_REMOTE=1  required when API_BASE_URL host is not localhost/127.0.0.1/::1.
#   SEED_PASSWORD        password for the seeded accounts. When unset, a random one is
#                        generated and saved in a 0600 file outside the repository
#                        (SEED_PASSWORD_FILE, default ~/.local/state/paw-motion/seed-password).
#   POSTGRES_PASSWORD    required, no fallback.
#
# Tunables: OWNER_COUNT (default 100), API_BASE_URL, DB_CONTAINER,
#           POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD.
set -euo pipefail

API="${API_BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-pawmotion-postgres}"
DB_NAME="${POSTGRES_DB:-pawmotion}"
DB_USER="${POSTGRES_USER:-pawmotion}"
OWNER_COUNT="${OWNER_COUNT:-100}"
CENTER_LAT=-23.5615   # Ibirapuera, São Paulo — matches the apps' fallback area
CENTER_LNG=-46.6559

say() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*" >&2; }
die() { printf '\033[1;31m[error]\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- safety guards (before any network call)
[ "${SEED_ALLOW_LOCAL:-}" = "1" ] || die "SEED_ALLOW_LOCAL=1 is required to run the seed (explicit local mode)"
API_HOST="${API#*://}"; API_HOST="${API_HOST%%/*}"; API_HOST="${API_HOST##*@}"
case "$API_HOST" in
  \[*\]*) API_HOST="${API_HOST%%\]*}"; API_HOST="${API_HOST#\[}" ;;
  *) API_HOST="${API_HOST%%:*}" ;;
esac
case "$API_HOST" in
  localhost|127.0.0.1|::1) ;;
  *) [ "${SEED_ALLOW_REMOTE:-}" = "1" ] || die "API_BASE_URL host '$API_HOST' is not local; set SEED_ALLOW_REMOTE=1 to seed a remote target" ;;
esac
[ -n "${POSTGRES_PASSWORD:-}" ] || die "POSTGRES_PASSWORD is required (no default)"
if [ -n "${SEED_PASSWORD:-}" ]; then
  PASSWORD="$SEED_PASSWORD"
else
  SEED_PASSWORD_FILE="${SEED_PASSWORD_FILE:-${XDG_STATE_HOME:-$HOME/.local/state}/paw-motion/seed-password}"
  if [ -s "$SEED_PASSWORD_FILE" ]; then
    PASSWORD=$(cat "$SEED_PASSWORD_FILE")
  else
    command -v openssl >/dev/null || die "openssl is required to generate SEED_PASSWORD (or export SEED_PASSWORD)"
    mkdir -p "$(dirname "$SEED_PASSWORD_FILE")"
    (umask 077; printf 'Sd1!%s' "$(openssl rand -hex 12)" > "$SEED_PASSWORD_FILE")
    chmod 600 "$SEED_PASSWORD_FILE"
    PASSWORD=$(cat "$SEED_PASSWORD_FILE")
    echo "Generated seed password saved to $SEED_PASSWORD_FILE (0600)"
  fi
fi
command -v curl >/dev/null || die "curl is required"
command -v jq >/dev/null || die "jq is required"
command -v awk >/dev/null || die "awk is required"
[ "$OWNER_COUNT" -ge 3 ] || die "OWNER_COUNT must be >= 3 (named demo owners are part of the count)"

req() { # method path token body [extra-header]
  local method=$1 path=$2 token=${3:-} body=${4:-} extra=${5:-}
  local args=(-sS -X "$method" "$API$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$extra" ] && args+=(-H "$extra")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}

# post_auth PATH BODY — POST an auth request, waiting out the server's
# 10-requests/minute/IP auth rate limit (HTTP 429) between attempts.
# Prints "<body>\n<status>" like a normal `curl -w '\n%{http_code}'` response.
post_auth() {
  local path=$1 body=$2 attempt res status
  for attempt in 1 2 3 4 5 6; do
    res=$(curl -sS -w '\n%{http_code}' -X POST "$API$path" -H 'Content-Type: application/json' -d "$body")
    status=$(tail -n1 <<<"$res")
    if [ "$status" != "429" ]; then
      printf '%s' "$res"
      return
    fi
    [ "$attempt" -eq 6 ] && break
    printf '\033[1;33m[rate-limit]\033[0m %s — waiting for the 1-minute window (%s/6)\n' "$path" "$attempt" >&2
    sleep 62
  done
  printf '%s' "$res"
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
  res=$(post_auth "$reg_path" "$body")
  status=$(tail -n1 <<<"$res")
  if [ "$status" != "200" ] && [ "$status" != "201" ]; then
    res=$(post_auth "$login_path" "$(jq -nc --arg e "$email" --arg p "$PASSWORD" '{email:$e,password:$p}')")
    status=$(tail -n1 <<<"$res")
  fi
  res=$(sed '$d' <<<"$res")
  local token
  token=$(jq -r '.accessToken // empty' <<<"$res")
  [ -n "$token" ] || die "could not authenticate $email — $(jq -r '.detail // .title // .' <<<"$res" 2>/dev/null || echo "$res")"
  printf '%s' "$token"
}

# ensure_pet TOKEN NAME SPECIES BREED SIZE BIRTHDATE ACTIVITY_PREF
# prints "<pet-id> <1|0>" — 1 when the pet was created now, 0 when it existed.
ensure_pet() {
  local token=$1 name=$2
  local existing
  existing=$(req GET /api/pets "$token" | jq -r --arg n "$name" '.[] | select(.name == $n) | .id' | head -1)
  [ -n "$existing" ] && { echo "$existing 0"; return; }
  local id
  id=$(req POST /api/pets "$token" "$(jq -nc --arg n "$name" --arg s "$3" --arg b "$4" --arg z "$5" --arg d "$6" --arg a "$7" \
    '{name:$n,species:$s,breed:$b,size:$z,birthDate:$d,activityPreference:$a}')" | jq -r '.id')
  echo "$id 1"
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

# ---------------------------------------------------------------- data pools
FIRST_NAMES=(Ana Bruno Carla Diego Elisa Fabio Gabriela Henrique Isabela Joao Karina Lucas Marina Nicolas Olivia Paulo Quezia Rafael Sofia Tiago Ursula Victor Walter Ximena Yuri Zilda Amanda Beatriz Caio Daniela Eduardo Fernanda Guilherme Helena Igor Julia Kaua Larissa Marcelo Natalia Otavio Priscila Renato Sabrina Thales Vitoria William Yasmin)
LAST_NAMES=(Costa Lima Souza Santos Oliveira Pereira Ferreira Almeida Rodrigues Gomes Carvalho Araujo Ribeiro Martins Barbosa Cardoso Teixeira Moreira Nogueira Castro)
DOG_NAMES=(Thor Bidu Fred Luna Melodia Zeus Nina Apollo Bella Max Luna2 Rocky Lola Toby Mia Rex Daisy Bento Frida Simba Nala Ozzy Pipoca Amora Cookie Duke Fiona Gaia Hulk Ivy Jade Koda Logan Maya Nino Olive Preta Quindim Romeu Safira Tito Uma Vicky Whisk Xodozinho Yumi Zara)
CAT_NAMES=(Mel Nina Gata Felix Lua Mia Simba Nala Oliver Lola Frida Tom Chiara Salem Bino)
BIRD_NAMES=(Loro Jade Blu Kiwi Sky Cacau)
DOG_BREEDS=('Golden Retriever' 'Beagle' 'Bulldog' 'Labrador' 'Poodle' 'Shih Tzu' 'Border Collie' 'Dachshund' 'SRD' 'Husky' 'Pug' 'Yorkshire')
CAT_BREEDS=('Siamese' 'Persian' 'Maine Coon' 'Bengal' 'SRD' 'Sphynx')
BIRD_BREEDS=('Calopsita' 'Periquito' 'Papagaio')
SIZES=(SMALL MEDIUM LARGE)
ACT_PREFS=(HIGH MEDIUM LOW)
POST_CAPTIONS=(
  'conquered the park today — pure joy!'
  'discovered the pet-friendly café downtown. Approved.'
  'sniff everything, walk nowhere. That is the way.'
  'first swim of the season went better than expected'
  'new trick unlocked: sitting for treats. Revolutionary.'
  'met three new friends at the dog park today'
  'vet said we are in great shape. Treats were earned.'
  'napping is a sport and we are champions'
  'chased the sunset, caught the feels'
  'mud bath deluxe — sorry not sorry'
  'the squeaky toy has been defeated. Again.'
  'best walk of the week, no debate'
)

say "Checking backend at $API"
curl -sS -o /dev/null --max-time 5 "$API/actuator/health" || curl -sS -o /dev/null --max-time 5 "$API/api/places?lat=0&lng=0" \
  || die "backend not reachable at $API — start it first (cd backend && ./gradlew bootRun)"

# ---------------------------------------------------------------- named owners (story-driving accounts)
say "Creating named owners"
ANA=$(register_or_login ana@petlife.demo 'Ana Costa')
BRUNO=$(register_or_login bruno@petlife.demo 'Bruno Lima')
CARLA=$(register_or_login carla@petlife.demo 'Carla Souza')

say "Creating named owners' pets"
read -r THOR THOR_NEW <<<"$(ensure_pet "$ANA" 'Thor' dog 'Golden Retriever' large '2021-03-15' HIGH)"
read -r MEL _ <<<"$(ensure_pet "$ANA" 'Mel' cat 'Siamese' small '2022-07-02' LOW)"
read -r BIDU BIDU_NEW <<<"$(ensure_pet "$BRUNO" 'Bidu' dog 'Beagle' medium '2020-11-20' MEDIUM)"
read -r FRED FRED_NEW <<<"$(ensure_pet "$CARLA" 'Fred' dog 'Bulldog' medium '2019-05-30' LOW)"
echo "pets: thor=$THOR mel=$MEL bidu=$BIDU fred=$FRED"

if [ "$THOR_NEW" = "1" ]; then
  say "Named owners: posts + activities"
  req POST /api/posts "$ANA" "$(jq -nc --arg p "$THOR" '{petId:$p,caption:"Thor conquered the park today — 5km of pure joy!",placeName:"Parque Ibirapuera"}')" >/dev/null
  req POST /api/posts "$BRUNO" "$(jq -nc --arg p "$BIDU" '{petId:$p,caption:"Beagle mode: sniff everything, walk nowhere."}')" >/dev/null
  req POST /api/posts "$CARLA" "$(jq -nc --arg p "$FRED" '{petId:$p,caption:"Fred discovered the pet-friendly café downtown. Approved."}')" >/dev/null
  req POST /api/activities "$ANA" "$(jq -nc --arg p "$THOR" '{petId:$p,type:"WALK",startedAt:"2025-01-10T08:30:00Z",durationSeconds:2700,distanceMeters:3200,latitude:-23.5615,longitude:-46.6559,notes:"Morning run around the lake"}')" >/dev/null
  req POST /api/activities "$BRUNO" "$(jq -nc --arg p "$BIDU" '{petId:$p,type:"PLAY",startedAt:"2025-01-10T17:00:00Z",durationSeconds:1800,latitude:-23.5578,longitude:-46.6602,notes:"Fetch at the dog park"}')" >/dev/null
  req POST /api/activities "$CARLA" "$(jq -nc --arg p "$FRED" '{petId:$p,type:"WALK",startedAt:"2025-01-11T09:15:00Z",durationSeconds:1500,distanceMeters:900,latitude:-23.5642,longitude:-46.6492}')" >/dev/null
fi

LOCATIONS="('ana@petlife.demo', -23.5615, -46.6559), ('bruno@petlife.demo', -23.5578, -46.6602), ('carla@petlife.demo', -23.5642, -46.6492)"

# ---------------------------------------------------------------- generated owners
say "Creating generated owners + pets (total owners: $OWNER_COUNT)"
GEN_TOTAL=$((OWNER_COUNT - 3))
GEN_CREATED=0
for i in $(seq 4 "$OWNER_COUNT"); do
  EMAIL=$(printf 'owner%03d@petlife.demo' "$i")
  DISPLAY="${FIRST_NAMES[$((i % ${#FIRST_NAMES[@]}))]} ${LAST_NAMES[$(((i * 7) % ${#LAST_NAMES[@]}))]}"
  TOKEN=$(register_or_login "$EMAIL" "$DISPLAY")

  # species mix: ~70% dog, ~20% cat, ~10% bird — deterministic by index
  case $((i % 10)) in
    0|1)
      SPECIES=cat; NAME="${CAT_NAMES[$((i % ${#CAT_NAMES[@]}))]}"
      BREED="${CAT_BREEDS[$((i % ${#CAT_BREEDS[@]}))]}"; SIZE="${SIZES[$((i % 2))]}" ;;
    2)
      SPECIES=bird; NAME="${BIRD_NAMES[$((i % ${#BIRD_NAMES[@]}))]}"
      BREED="${BIRD_BREEDS[$((i % ${#BIRD_BREEDS[@]}))]}"; SIZE=SMALL ;;
    *)
      SPECIES=dog; NAME="${DOG_NAMES[$((i % ${#DOG_NAMES[@]}))]}"
      BREED="${DOG_BREEDS[$((i % ${#DOG_BREEDS[@]}))]}"; SIZE="${SIZES[$((i % 3))]}" ;;
  esac
  PET_NAME="$NAME-$i"   # unique per owner so re-runs match by name
  BIRTH=$(printf '%04d-%02d-%02d' $((2018 + i % 7)) $((1 + i % 12)) $((1 + i % 28)))
  PREF="${ACT_PREFS[$((i % 3))]}"

  read -r PET_ID PET_NEW <<<"$(ensure_pet "$TOKEN" "$PET_NAME" "$SPECIES" "$BREED" "$SIZE" "$BIRTH" "$PREF")"
  [ "$PET_NEW" = "1" ] && GEN_CREATED=$((GEN_CREATED + 1))

  # deterministic spread around Ibirapuera (~±4km) for owner location / nearby map
  read -r OLAT OLNG <<<"$(awk -v i="$i" -v la="$CENTER_LAT" -v ln="$CENTER_LNG" \
    'BEGIN { printf "%.5f %.5f", la + (((i*37) % 90) - 45)/1000.0, ln + (((i*53) % 90) - 45)/1000.0 }')"
  LOCATIONS+=", ('$EMAIL', $OLAT, $OLNG)"

  # feed population: caption-only post for every new pet, activity for every 2nd
  if [ "$PET_NEW" = "1" ] && [ -n "$PET_ID" ] && [ "$PET_ID" != "null" ]; then
    CAPTION="${POST_CAPTIONS[$((i % ${#POST_CAPTIONS[@]}))]}"
    req POST /api/posts "$TOKEN" "$(jq -nc --arg p "$PET_ID" --arg c "$NAME $CAPTION" '{petId:$p,caption:$c}')" >/dev/null
    if [ $((i % 2)) -eq 0 ]; then
      TYPE=$([ $((i % 4)) -eq 0 ] && echo PLAY || echo WALK)
      req POST /api/activities "$TOKEN" "$(jq -nc --arg p "$PET_ID" --arg t "$TYPE" \
        --argjson lat "$OLAT" --argjson lng "$OLNG" \
        '{petId:$p,type:$t,startedAt:"2025-01-12T10:00:00Z",durationSeconds:1800,distanceMeters:1500,latitude:$lat,longitude:$lng}')" >/dev/null
    fi
  fi

  if [ $((i % 10)) -eq 0 ]; then echo "  ... $((i - 3))/$GEN_TOTAL generated owners"; fi
done
echo "generated owners done ($GEN_CREATED new pets created this run)"

# ---------------------------------------------------------------- partners
say "Creating partners"
SPA=$(register_or_login spa@petlife.demo 'Paula Mota' partner 'Happy Paws Spa')
VET=$(register_or_login vet@petlife.demo 'Dr. Ramos' partner 'Central Pet Vet')
CAFE=$(register_or_login cafe@petlife.demo 'Rui Alves' partner 'Café Au Au')

# extra partners spread across São Paulo so the map is well populated:
# email|display|business|category|description|phone|address|lat|lng|hours
EXTRA_PARTNERS=(
  'shop@petlife.demo|Célia Prado|Petzz Store Moema|PET_SHOP|Premium food, toys and accessories.|(11) 96655-1100|Av. Ibirapuera, 2100 - Moema, São Paulo|-23.5920|-46.6640|Daily · 9:00–21:00'
  'hotel@petlife.demo|Mauro Braga|Hotel Fofo Pet|HOTEL|Boarding and daycare with 24h supervision.|(11) 95544-2200|Rua Michigan, 480 - Brooklin, São Paulo|-23.6070|-46.6830|24/7'
  'trainer@petlife.demo|Sandra Luz|Adestra Mais|TRAINING|Positive-reinforcement obedience classes.|(11) 94433-3300|Rua Vergueiro, 1500 - Vila Mariana, São Paulo|-23.5850|-46.6350|Mon–Sat · 8:00–19:00'
  'walker@petlife.demo|Igor Pena|Passeio Feliz|WALKER|Group and solo dog walks in Pinheiros.|(11) 93322-4400|Rua dos Pinheiros, 700 - Pinheiros, São Paulo|-23.5670|-46.6930|Mon–Fri · 7:00–18:00'
  'vet2@petlife.demo|Dra. Heloisa|Vet Vila Madalena|VET|Neighborhood clinic — consults and vaccines.|(11) 92211-5500|Rua Harmonia, 320 - Vila Madalena, São Paulo|-23.5560|-46.6910|Mon–Sat · 9:00–18:00'
  'groom2@petlife.demo|Beto Sales|Banho & Tosa Premium|GROOMING|Express grooming with pickup option.|(11) 91100-6600|Rua Joaquim Antunes, 250 - Itaim Bibi, São Paulo|-23.5820|-46.6760|Mon–Sat · 8:00–20:00'
  'cafe2@petlife.demo|Luiza Reis|Cat Café SP|CAFE|Coffee lounge with resident cats.|(11) 90099-7700|Rua Augusta, 1100 - Consolação, São Paulo|-23.5520|-46.6590|Tue–Sun · 10:00–22:00'
  'groom3@petlife.demo|Nara Duarte|Tosa Express Perdizes|GROOMING|Quick baths and hygienic trims.|(11) 98877-8800|Rua Cardoso de Almeida, 900 - Perdizes, São Paulo|-23.5380|-46.6770|Mon–Sat · 9:00–19:00'
  'vet3@petlife.demo|Dr. Otavio|Pet Center Saúde|VET|Full-service clinic with lab and imaging.|(11) 97766-9900|Av. Jabaquara, 1400 - Saúde, São Paulo|-23.6150|-46.6390|Daily · 8:00–22:00'
)

PARTNER_TOKENS=("$SPA" "$VET" "$CAFE")
for row in "${EXTRA_PARTNERS[@]}"; do
  IFS='|' read -r PEMAIL PDISPLAY PBUSINESS _ <<<"$row"
  PARTNER_TOKENS+=("$(register_or_login "$PEMAIL" "$PDISPLAY" partner "$PBUSINESS")")
done

say "Configuring partner profiles (creates map places)"
req PATCH /api/partner/profile "${PARTNER_TOKENS[0]}" "$(jq -nc '{businessName:"Happy Paws Spa",category:"GROOMING",description:"Baths, trims and spa day for dogs and cats.",phone:"(11) 99988-1122",address:"Rua das Flores, 120 - Ibirapuera, São Paulo",latitude:-23.5580,longitude:-46.6500,openingHours:"Mon–Sat · 9:00–19:00"}')" >/dev/null
req PATCH /api/partner/profile "${PARTNER_TOKENS[1]}" "$(jq -nc '{businessName:"Central Pet Vet",category:"VET",description:"24h veterinary clinic with preventive care plans.",phone:"(11) 98877-3344",address:"Av. Paulista, 900 - Bela Vista, São Paulo",latitude:-23.5650,longitude:-46.6520,openingHours:"24/7"}')" >/dev/null
req PATCH /api/partner/profile "${PARTNER_TOKENS[2]}" "$(jq -nc '{businessName:"Café Au Au",category:"CAFE",description:"Pet-friendly café — puppuccino on the house.",phone:"(11) 97766-5566",address:"Rua Oscar Freire, 450 - Jardins, São Paulo",latitude:-23.5625,longitude:-46.6690,openingHours:"Daily · 8:00–20:00"}')" >/dev/null
for idx in "${!EXTRA_PARTNERS[@]}"; do
  IFS='|' read -r _ _ PBUSINESS PCAT PDESC PPHONE PADDR PLAT PLNG PHOURS <<<"${EXTRA_PARTNERS[$idx]}"
  req PATCH /api/partner/profile "${PARTNER_TOKENS[$((idx + 3))]}" "$(jq -nc \
    --arg b "$PBUSINESS" --arg c "$PCAT" --arg d "$PDESC" --arg p "$PPHONE" --arg a "$PADDR" --arg h "$PHOURS" \
    --argjson lat "$PLAT" --argjson lng "$PLNG" \
    '{businessName:$b,category:$c,description:$d,phone:$p,address:$a,latitude:$lat,longitude:$lng,openingHours:$h}')" >/dev/null
done

say "Creating partner services"
# per-category service templates: name|description|minutes|cents
services_for() { # category -> two service rows
  case $1 in
    GROOMING) printf '%s\n%s\n' 'Full groom|Bath, haircut, nail trim and ear cleaning|90|12000' 'Bath & brush|Hygiene bath and brushing|60|8000' ;;
    VET)      printf '%s\n%s\n' 'Check-up|General health assessment|45|15000' 'Vaccination|Core vaccines, includes the shot card update|30|8000' ;;
    CAFE)     printf '%s\n%s\n' 'Pet table reservation|Reserved pet table + free puppuccino|60|2500' 'Puppuccino combo|Coffee + puppuccino for your companion|30|1800' ;;
    PET_SHOP) printf '%s\n%s\n' 'Nutrition consult|Food plan guidance in-store|20|900' 'Toy fitting|Find the right toy for your pet|15|500' ;;
    HOTEL)    printf '%s\n%s\n' 'Day boarding|Supervised daycare, 8 hours|480|9000' 'Overnight stay|Boarding with 24h supervision|1440|15000' ;;
    TRAINING) printf '%s\n%s\n' 'Obedience basics|Core commands and leash manners|60|11000' 'Puppy class|Socialization for puppies|45|9000' ;;
    WALKER)   printf '%s\n%s\n' 'Group walk|Social walk with up to 5 dogs|60|4000' 'Solo adventure walk|One-on-one park session|90|7000' ;;
    *)        printf '%s\n' 'Standard service|Demo service|30|1000' ;;
  esac
}
PARTNER_CATEGORIES=(GROOMING VET CAFE)
for row in "${EXTRA_PARTNERS[@]}"; do IFS='|' read -r _ _ _ c _ <<<"$row"; PARTNER_CATEGORIES+=("$c"); done

for idx in "${!PARTNER_TOKENS[@]}"; do
  TOKEN="${PARTNER_TOKENS[$idx]}"
  CAT="${PARTNER_CATEGORIES[$idx]}"
  while IFS='|' read -r SNAME SDESC SMINS SCENTS; do
    [ -n "$SNAME" ] && ensure_service "$TOKEN" "$SNAME" "$SDESC" "$SMINS" "$SCENTS"
  done <<<"$(services_for "$CAT")"
done

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
ALERT_NEW=0
if [ -z "$ALERT_ID" ]; then
  ALERT_ID=$(req POST /api/lost-pets "$ANA" "$(jq -nc --arg p "$MEL" \
    '{petId:$p,description:"Mel slipped out near the park — grey Siamese, blue collar, very shy.",contactPhone:"(11) 98811-2233",latitude:-23.5600,longitude:-46.6530,lastSeenAt:"2025-01-12T07:45:00Z",radiusMeters:3000}')" | jq -r '.id')
  ALERT_NEW=1
fi
[ "$ALERT_NEW" = "1" ] && [ -n "$ALERT_ID" ] && [ "$ALERT_ID" != "null" ] && req POST "/api/lost-pets/$ALERT_ID/sightings" "$BRUNO" \
  "$(jq -nc '{latitude:-23.5590,longitude:-46.6510,seenAt:"2025-01-12T09:00:00Z",note:"Saw a grey cat near the pet shop entrance"}')" >/dev/null

say "Playdate invite"
[ "$FRED_NEW" = "1" ] && req POST /api/playdates "$ANA" "$(jq -nc --arg p "$THOR" --arg f "$FRED" \
  '{petId:$p,mode:"NOW",inviteePetIds:[$f],latitude:-23.5615,longitude:-46.6559,locationName:"Ibirapuera dog park"}')" >/dev/null || true

# ---------------------------------------------------------------- SQL tail (no API equivalents)
say "Applying SQL tail (owner locations, verified partners, campaign approval)"
SQL="
UPDATE owners o SET location = ST_SetSRID(ST_MakePoint(x.lng, x.lat), 4326)::geography
FROM (VALUES $LOCATIONS) AS x(email, lat, lng)
WHERE o.email = x.email AND o.location IS NULL;
UPDATE partner_profiles pp SET verified = TRUE, verified_at = now() FROM owners o WHERE pp.partner_id = o.id AND o.email IN ('spa@petlife.demo','vet@petlife.demo','vet2@petlife.demo','hotel@petlife.demo');
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
elif command -v psql >/dev/null && PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" -c "$SQL" >/dev/null 2>&1; then
  echo "SQL tail applied"
else
  warn "could not reach Postgres — run manually:
$SQL"
fi

say "Done! $OWNER_COUNT owners + 12 partner businesses seeded (password: SEED_PASSWORD env or ${SEED_PASSWORD_FILE:-\$SEED_PASSWORD})"
cat <<EOF
  Named owners: ana@petlife.demo (Thor + Mel, lost-pet alert), bruno@petlife.demo (Bidu), carla@petlife.demo (Fred)
  Generated:    owner004@petlife.demo … owner$(printf '%03d' "$OWNER_COUNT")@petlife.demo (1 pet each, posts + activities)
  Partners:     spa@petlife.demo (Happy Paws Spa, active campaign, funded wallet)
                vet@petlife.demo (Central Pet Vet, draft campaign)
                cafe@petlife.demo + 9 more businesses across São Paulo — all visible on the map
EOF
