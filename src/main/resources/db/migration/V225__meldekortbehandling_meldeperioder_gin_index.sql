-- GIN-indeks på meldekortbehandling.meldeperioder for å støtte effektive jsonb-containment-spørringer
-- (f.eks. `meldeperioder @> jsonb_build_array(jsonb_build_object('kjedeId', :kjedeId))`).
--
-- Dette gjør at vi kan finne meldekortbehandlinger basert på kjedeId uten å være avhengig av at
-- elementet ligger på en bestemt posisjon (element 0) i arrayet, slik den eksisterende btree-indeksen
-- `idx_meldekortbehandling_kjede_jsonb_sist_endret` forutsetter. Når vi etter hvert tillater flere
-- meldeperioder per behandling vil GIN-indeksen være nødvendig for ytelse.
--
-- jsonb_path_ops er en mer kompakt og raskere variant av GIN for `@>`-spørringer (vi trenger ikke
-- de andre operatorene som default-opclass støtter).

CREATE INDEX idx_meldekortbehandling_meldeperioder_gin
    ON meldekortbehandling
    USING gin (meldeperioder jsonb_path_ops);

