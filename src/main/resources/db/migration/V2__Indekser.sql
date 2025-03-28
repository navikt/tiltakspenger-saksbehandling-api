CREATE INDEX sak_behandling
    ON behandling
        (
         sak_id
            );


CREATE INDEX saks_ident
    ON sak
        (
         ident
            );

CREATE INDEX søknad_ident
    ON søknad
        (
         ident
            );

CREATE INDEX søknad_behandling
    ON søknad
        (
         behandling_id
            );

CREATE INDEX søknadstiltak_søknad_id
    ON søknadstiltak
        (
         søknad_id
            );

CREATE INDEX søknad_barnetillegg_søknad_id
    ON søknad_barnetillegg
        (
         søknad_id
            );

CREATE INDEX idx_utfylt_meldekort_sak_id ON meldekort (sak_id);

CREATE INDEX idx_utfylt_meldekort_rammevedtak_id ON meldekort (rammevedtak_id);
