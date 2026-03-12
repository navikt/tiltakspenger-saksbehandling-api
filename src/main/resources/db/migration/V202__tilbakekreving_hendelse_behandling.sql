ALTER TABLE tilbakekreving_hendelse
    ADD COLUMN IF NOT EXISTS behandling JSONB NULL,
    ADD COLUMN IF NOT EXISTS ekstern_behandling_id VARCHAR NULL,
    DROP COLUMN IF EXISTS url,
    DROP COLUMN IF EXISTS behandlingsstatus;

UPDATE tilbakekreving_hendelse
SET behandling = jsonb_build_object(
        'behandlingId', value -> 'tilbakekreving' ->> 'behandlingId',
        'sakOpprettet', value -> 'tilbakekreving' ->> 'sakOpprettet',
        'varselSendt', value -> 'tilbakekreving' ->> 'varselSendt',
        'behandlingsstatus', value -> 'tilbakekreving' ->> 'behandlingsstatus',
        'forrigeBehandlingsstatus', value -> 'tilbakekreving' ->> 'forrigeBehandlingsstatus',
        'totaltFeilutbetaltBeløp', value -> 'tilbakekreving' ->> 'totaltFeilutbetaltBeløp',
        'saksbehandlingURL', value -> 'tilbakekreving' ->> 'saksbehandlingURL',
        'fullstendigPeriode', jsonb_build_object(
                'fraOgMed', value -> 'tilbakekreving' -> 'fullstendigPeriode' ->> 'fom',
                'tilOgMed', value -> 'tilbakekreving' -> 'fullstendigPeriode' ->> 'tom'
                              )
                 )
WHERE hendelse_type = 'BehandlingEndret'
  AND behandling IS NULL;

UPDATE tilbakekreving_hendelse
SET ekstern_behandling_id = value ->> 'eksternBehandlingId'
WHERE hendelse_type = 'BehandlingEndret'
  AND ekstern_behandling_id IS NULL;