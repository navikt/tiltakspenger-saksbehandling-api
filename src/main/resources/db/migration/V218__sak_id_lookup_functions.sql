-- Hjelpefunksjoner for å gjøre det litt enklere å slå opp i andre tabeller basert på fnr eller saksnummer
-- F.eks. select * from behandling where sak_id = fnrTilSakId('12345678911');
CREATE OR REPLACE FUNCTION fnrTilSakId(p_fnr varchar) RETURNS varchar AS
$$
SELECT id FROM sak WHERE fnr = p_fnr;
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION saksnummerTilSakId(p_saksnummer varchar) RETURNS varchar AS
$$
SELECT id FROM sak WHERE saksnummer = p_saksnummer;
$$ LANGUAGE sql STABLE;

