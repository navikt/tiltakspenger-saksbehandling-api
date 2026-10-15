-- Saksbehandler kan nå velge å journalføre begrunnelsen for barnetillegg i tillegg til begrunnelsen for vilkårsvurderingen.
-- Dagens boolean gjaldt begrunnelsen for vilkårsvurderingen, og flyttes til feltet vilkårsvurdering.
ALTER TABLE behandling
    ALTER COLUMN skal_journalfore_notat DROP DEFAULT;

ALTER TABLE behandling
    ALTER COLUMN skal_journalfore_notat TYPE jsonb
        USING jsonb_build_object('vilkårsvurdering', skal_journalfore_notat, 'barnetillegg', false);

ALTER TABLE behandling
    ALTER COLUMN skal_journalfore_notat SET DEFAULT '{
      "vilkårsvurdering": false,
      "barnetillegg": false
    }'::jsonb;
