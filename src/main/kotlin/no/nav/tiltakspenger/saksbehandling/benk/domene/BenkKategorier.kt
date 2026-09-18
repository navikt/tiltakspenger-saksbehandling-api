package no.nav.tiltakspenger.saksbehandling.benk.domene

/**
 * De fanespesifikke kategoriene benken filtrerer og viser på.
 * Alle er benkens egne typer, slik at benken kan endre visningen uten å endre domenet den leser fra.
 */

enum class BenkSøknadstype {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
}

enum class BenkSøknadsbehandlingResultat {
    INNVILGELSE,
    AVSLAG,
    IKKE_VALGT,
}

enum class BenkRevurderingResultat {
    STANS,
    REVURDERING_INNVILGELSE,
    OMGJØRING,
    OMGJØRING_OPPHØR,
    OMGJØRING_IKKE_VALGT,
}

enum class BenkKlagebehandlingResultat {
    AVVIST,
    OMGJØR,
    OPPRETTHOLDT,
}

/**
 * Klagebehandling har ingen beslutter og ingen automatisk flyt, så statusene er færre enn for de andre fanene.
 * En klage mottatt fra klageinstansen vises som [KLAR_TIL_FERDIGSTILLING], fordi det eneste som gjenstår er å ferdigstille.
 */
enum class BenkKlagebehandlingStatus {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_FERDIGSTILLING,
}

/**
 * Meldekortfanen samler både meldekortbehandlinger saksbehandler har startet, og meldekort fra bruker som venter på behandling.
 */
enum class BenkMeldekortType {
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
}

/**
 * Tilbakekreving har en egen saksbehandlingsflyt, og derfor egne statuser.
 * Speiler tilbakekrevingens statuser uten de avsluttede behandlingene, og skiller «til» fra «under» på om noen har tatt behandlingen.
 */
enum class BenkTilbakekrevingStatus {
    OPPRETTET,
    TIL_FORHÅNDSVARSEL,
    UNDER_FORHÅNDSVARSLING,
    TIL_BEHANDLING,
    UNDER_BEHANDLING,
    TIL_GODKJENNING,
    UNDER_GODKJENNING,
}

enum class BenkTilbakekrevingKilde {
    RAMMEVEDTAK,
    MELDEKORT,
}

/**
 * Behandlingstypene mine-fanen kan filtreres på.
 * Innsendte og korrigerte meldekort er aldri tildelt en saksbehandler, og kan derfor aldri være «mine».
 */
enum class BenkBehandlingstype {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    KLAGEBEHANDLING,
    TILBAKEKREVING,
}
