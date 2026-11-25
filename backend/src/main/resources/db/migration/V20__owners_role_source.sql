-- C15: origem do papel. Aditiva: admins existentes permanecem 'MANUAL' e nao sao alterados.
ALTER TABLE owners ADD COLUMN role_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE owners ADD CONSTRAINT ck_owners_role_source CHECK (role_source IN ('MANUAL', 'CONFIG'));
