# ADR-0013: Client Input Validation and Pet Profile Editing

- **Status**: Proposed
- **Context**: Owner and partner forms collect email, password, phone, URL, date, numeric, and campaign fields. Pet owners also need to edit registered pets, while the API initially only supported create/list/get.
- **Decision**: Use shared client-side validation helpers plus specialized web/mobile inputs for common formats, and expose owner-scoped `PATCH /api/pets/{petId}` for full pet-detail updates. Keep backend validation as the authoritative boundary.
- **Alternatives**: Rely only on server-side validation; add a separate pet-update command per field; introduce a third-party masking/validation library for every platform.
- **Rationale**: Shared helpers keep web and mobile behavior consistent without adding dependencies. A single PATCH matches the current full pet-profile form and keeps the hexagonal use-case boundary simple. Server-side Bean Validation remains authoritative for API clients.
- **Positive consequences**: Faster user feedback, consistent masks for phone/URL/email, clearer field errors, and pet profiles can be corrected without recreating pets.
- **Negative consequences**: Client and server validation rules must be kept aligned; partial-field update semantics are not supported by the current pet form.
- **Risks**: Overly strict client masks could reject valid international formats; mitigation is to keep masks format-oriented and backend validation permissive within documented limits.
- **Implementation guidance**: Web uses `frontend/src/lib/validation.ts` and typed input components in `frontend/src/components/ui.tsx`; mobile mirrors the helpers in `mobile/src/validation.ts`. Pet editing is implemented through `UpdatePetUseCase`, `Pet.updateDetails`, and the persistence adapter update path.
- **Migration/reversal**: Move to generated API types or a shared validation package if web/mobile rules diverge; switch to JSON Patch or field-level commands if partial pet updates become necessary.
