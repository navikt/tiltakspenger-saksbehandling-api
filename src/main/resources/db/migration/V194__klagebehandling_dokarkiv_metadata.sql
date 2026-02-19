ALTER TABLE klagebehandling ADD COLUMN brev_metadata JSONB;
ALTER TABLE klagevedtak RENAME COLUMN brev_json TO brev_metadata;