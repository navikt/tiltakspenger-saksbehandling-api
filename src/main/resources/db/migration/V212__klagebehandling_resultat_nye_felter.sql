/**
  Migeringen blir gjort i context av videre behov for klagebehandling og situasjon som er oppstått med å omgjøre videre
    Man skal nå kunne ferdigstille en klagebehandling i 'alle' situasjoner. Saksbehandler skal gjerne begrunne dette
    Bedre støtte for statistikk hvor rammebehandlinger er opprettet på en klagebehandling. Her kan man knytte uendelig mange behandlinger på klagen

  Litt bedre teknisk oversikt over hvilken rammebehandling er åpen på en klagebehandling.
 */

/**
  Gjør om rammebehandlingId (som et enkelt felt) til en liste
 */
UPDATE klagebehandling
SET resultat = jsonb_set(
        resultat,
        '{rammebehandlingId}',
        CASE
            WHEN (resultat ->> 'rammebehandlingId') IS NULL
                THEN '[]'::jsonb
            ELSE jsonb_build_array(resultat ->> 'rammebehandlingId')
            END
               )
WHERE jsonb_typeof(resultat -> 'rammebehandlingId') != 'array';

/**
  Legger til åpenRammebehandlingId, som skal være den rammebehandlingen som er åpen på klagebehandlingen på dette tidspunktet.
 */
UPDATE klagebehandling k
SET resultat = jsonb_set(
        k.resultat,
        '{åpenRammebehandlingId}',
        CASE
            WHEN b.status IN ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING')
                THEN to_jsonb(b.id)
            ELSE 'null'::jsonb
            END
               )
FROM behandling b
WHERE b.id = ANY (ARRAY(SELECT jsonb_array_elements_text(k.resultat -> 'rammebehandlingId')));

/**
  ...ellers skal den være null hvis det ikke er noen åpne rammebehandlinger på klagebehandlingen
 */
UPDATE klagebehandling
SET resultat = jsonb_set(resultat, '{åpenRammebehandlingId}', 'null'::jsonb)
WHERE resultat ->> 'åpenRammebehandlingId' is null;

/**
    Legger til begrunnelseFerdigstilling, som skal være en tekst som saksbehandler kan skrive inn når dem ferdigstiller en klagebehandling
 */
UPDATE klagebehandling
SET resultat = jsonb_set(resultat, '{begrunnelseFerdigstilling}', 'null'::jsonb)
WHERE resultat ->> 'begrunnelseFerdigstilling' is null;
