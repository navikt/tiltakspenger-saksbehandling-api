/*
 Begrunnelses feltet har tidligere vært brukt både som en begrunnelse for beslutter - men også blitt brukt på selve brevet
 Dette viser seg til å bare gjelde opp til et punkt. Før det så ?????
 TODO - se om vi klarer å rydde opp i hvordan defaulten settes.
 */
ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS tekst_til_vedtaksbrev VARCHAR;

UPDATE meldekortbehandling
SET tekst_til_vedtaksbrev = begrunnelse
WHERE tekst_til_vedtaksbrev IS NULL;


