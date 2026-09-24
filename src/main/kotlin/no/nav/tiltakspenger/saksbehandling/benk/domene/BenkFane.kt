package no.nav.tiltakspenger.saksbehandling.benk.domene

/**
 * Fanene i benk v2.
 * Én fane er én spørring med sitt eget radformat, sitt eget filter og sitt eget sett med sorteringskolonner.
 */
enum class BenkFane {
    SØKNADER,
    REVURDERINGER,
    MELDEKORT,
    KLAGE,
    TILBAKEKREVING,

    /**
     * Behandlingene den innloggede saksbehandleren er tildelt, som saksbehandler eller beslutter.
     * Fanen har ingen egen spørring, men viser de andre fanene som seksjoner avgrenset til den innloggede — se [HentMineKommando].
     */
    MINE,
}
