package no.nav.tiltakspenger.saksbehandling.benk.domene

/**
 * Angir hva en tilbakekreving stammer fra.
 * Fylles kun ut for [BehandlingssammendragType.TILBAKEKREVING].
 */
enum class TilbakekrevingKilde {
    RAMMEVEDTAK,
    MELDEKORT,
}
