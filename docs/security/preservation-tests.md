# Testes de preservação (auditoria de segurança)

Spec: `.kiro/specs/security-audit-remediation` (tarefa 2, Property 2: Preservation).

Estes testes fixam o comportamento do código **não corrigido** para entradas em que nenhuma `isBugCondition_Cn` vale. Eles passam hoje e devem continuar passando depois de cada PR de correção. Ao contrário dos testes exploratórios, **rodam no CI padrão** (`./gradlew build`), sem tag.

```bash
cd backend
./gradlew test --tests 'com.pawmotionai.preservation.*'   # exige Docker (Testcontainers), exceto RateLimitPreservationTest
```

Os testes ficam em `backend/src/test/kotlin/com/pawmotionai/preservation/`. Propriedades usam entradas aleatórias com semente fixa no nome do teste (`@ValueSource(longs = ...)`), sem biblioteca nova (jqwik fica para a decisão do PR 2a).

| Arquivo | Cláusulas | O que fixa |
|---|---|---|
| `BillingPreservationIT` | 3.1 | Com perfil `local` e `fake-complete-enabled=true`: criar compra, `fake-complete`, MAIN creditado uma vez (valores 10 a 10000). Webhook fake com segredo válido credita uma vez e a reentrega não duplica. Segredo errado é recusado (4xx). |
| `AbacatePayWebhookPreservationIT` | 3.2 | Adaptador real do AbacatePay contra um servidor HTTP local no lugar da API deles. Webhook válido credita uma vez, reentrega e novo evento para a mesma transação não duplicam, assinatura ou segredo inválidos não creditam. |
| `AuthPreservationIT` | 3.3, 3.4, 3.11 | Registro, login e uso do token (owner e partner), 401 sem token, 403 entre papéis. Admin vindo do banco acessa `/api/admin/**`, wallet admin e verificação de parceiro. Senha errada, conta ausente, inativa ou do papel errado devolvem o mesmo status (401) e corpo. |
| `FeedProfileAdsPreservationIT` | 3.5, 3.6 | Reação e comentário em item visível, autor interage com o próprio item, perfil próprio conta tudo e terceiro vê a atividade compartilhada. Pausar e retomar sem alterar conteúdo volta a `ACTIVE` (3 ciclos). Edição em `DRAFT` continua livre. |
| `RateLimitPreservationTest` | 3.7 | 10 tentativas/min/IP/rota de auth (200) e 429 da 11ª, isolamento por IP e rota. 120 escritas/min/conta (200) e 429 da 121ª, isolamento por conta. 30 mensagens de IA/min. Tráfego normal sem 429. Caminhos canônicos apenas. |
| `MapEngagementResourcesPreservationIT` | 3.8, 3.9, 3.10 | Primeiro alerta de pet perdido e primeira resposta a playdate notificam. Upload dentro da quota e URL `/api/media/{id}`. Listas paginadas na faixa antiga, `size` normal na wallet, slug sem colisão, insight de pet próprio. Atividade nos limites exatos 86400 s e 500000 m, atividades plausíveis, XP dentro do teto, impressão e primeiro clique contados uma vez. |
| `OpenApiSurfacePreservationIT` | 3.12 | O `docs/api/openapi.yaml` versionado descreve a API em execução (operações, parâmetros, códigos de resposta, schemas e nomes de propriedades). Somente leitura. |

Cláusulas 3.13 e 3.14 já têm cobertura existente: `LegacyDataMigrationIT` e `HealthApiIT`. A cláusula 3.15 (N não confirmados) não tem teste dedicado.

## Observações

- **Nada aqui pina um comportamento defeituoso.** Onde o código atual também expõe um defeito (por exemplo, o perfil de terceiro e as atividades privadas, C19), o teste afirma só a parte legítima (atividade compartilhada visível, dono vê tudo).
- **`OpenApiSurfacePreservationIT`**: o arquivo versionado já difere do gerado em três pontos que não são mudança de contrato: a porta em `servers`, a ordem das propriedades e as listas `required` (ex.: `CreatePostRequest.required=[petId]` só existe no gerado). Por isso o teste compara conjuntos e ignora `required`. Quando uma correção mudar o contrato, o PR deve regenerar o arquivo com `packages/api-client/scripts/generate.sh`.
- **Contexto Spring próprio**: `AuthPreservationIT` (`preservation.suite=auth`) isola o orçamento de 10 logins/min por rota. As classes de billing usam o perfil `local` e propriedades explícitas, então continuam válidas quando o PR 0a remover o default `fake`.
- **Tetos ainda não definidos** (decisão de produto 3.2): os testes usam valores bem abaixo de qualquer teto proposto (3 atividades, poucos pets), exceto os limites já existentes (86400 s, 500000 m, 10/min, 120/min, 30/min).
