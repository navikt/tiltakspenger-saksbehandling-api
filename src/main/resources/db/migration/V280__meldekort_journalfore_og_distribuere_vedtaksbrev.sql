-- Steg 1 av 2 for å gi meldekortbehandling.skal_sende_vedtaksbrev nytt navn, skal_journalfore_vedtaksbrev.
-- Valget har bare styrt journalføring, og vi legger nå til et eget valg for distribusjon.
-- Under rullerende deploy skriver gamle replikaer til den gamle kolonnen og nye replikaer til den nye.
-- Triggeren holder de to kolonnene i sync så lenge begge versjonene kan kjøre samtidig.
-- Steg 2, når ingen replikaer lenger bruker skal_sende_vedtaksbrev: fjern triggeren, funksjonen og den gamle kolonnen, og sett skal_journalfore_vedtaksbrev til NOT NULL.

ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS skal_journalfore_vedtaksbrev boolean;

UPDATE meldekortbehandling
SET skal_journalfore_vedtaksbrev = COALESCE(skal_sende_vedtaksbrev, TRUE);

CREATE OR REPLACE FUNCTION meldekortbehandling_synk_skal_journalfore_vedtaksbrev() RETURNS trigger AS
$$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.skal_journalfore_vedtaksbrev IS NULL THEN
            NEW.skal_journalfore_vedtaksbrev = COALESCE(NEW.skal_sende_vedtaksbrev, TRUE);
        ELSE
            NEW.skal_sende_vedtaksbrev = NEW.skal_journalfore_vedtaksbrev;
        END IF;
    ELSIF NEW.skal_journalfore_vedtaksbrev IS DISTINCT FROM OLD.skal_journalfore_vedtaksbrev THEN
        NEW.skal_sende_vedtaksbrev = NEW.skal_journalfore_vedtaksbrev;
    ELSIF NEW.skal_sende_vedtaksbrev IS DISTINCT FROM OLD.skal_sende_vedtaksbrev THEN
        NEW.skal_journalfore_vedtaksbrev = NEW.skal_sende_vedtaksbrev;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER meldekortbehandling_synk_skal_journalfore_vedtaksbrev
    BEFORE INSERT OR UPDATE
    ON meldekortbehandling
    FOR EACH ROW
EXECUTE FUNCTION meldekortbehandling_synk_skal_journalfore_vedtaksbrev();

ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS skal_distribuere_vedtaksbrev boolean NOT NULL DEFAULT FALSE;

ALTER TABLE meldekortvedtak
    ADD COLUMN IF NOT EXISTS distribusjon_id        varchar     NULL,
    ADD COLUMN IF NOT EXISTS distribusjonstidspunkt timestamptz NULL;
