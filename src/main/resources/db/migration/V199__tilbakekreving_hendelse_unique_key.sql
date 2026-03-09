-- Add unique constraint on key to prevent duplicate Kafka messages
CREATE UNIQUE INDEX IF NOT EXISTS tilbakekreving_hendelse_key_unique ON tilbakekreving_hendelse (key);

