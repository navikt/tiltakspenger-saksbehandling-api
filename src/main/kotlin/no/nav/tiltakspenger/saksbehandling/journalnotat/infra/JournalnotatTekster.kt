package no.nav.tiltakspenger.saksbehandling.journalnotat.infra

import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat

/** Tittelen brukes både i PDF-en og som tittel på journalposten og dokumentet i Joark. */
const val JOURNALNOTAT_TITTEL = "Notat om vedtak om tiltakspenger"

/** Rammevedtak har en begrunnelse for vilkårsvurderingen, mens meldekortbehandlinger har en generell begrunnelse. */
fun Journalnotat.Vedtakstype.tilBegrunnelseTittel(): String = when (this) {
    Journalnotat.Vedtakstype.SØKNAD_INNVILGELSE,
    Journalnotat.Vedtakstype.SØKNAD_AVSLAG,
    Journalnotat.Vedtakstype.REVURDERING_INNVILGELSE,
    Journalnotat.Vedtakstype.STANS,
    Journalnotat.Vedtakstype.OMGJØRING_INNVILGELSE,
    Journalnotat.Vedtakstype.OMGJØRING_OPPHØR,
    -> "Begrunnelse for vilkårsvurderingen"

    Journalnotat.Vedtakstype.MELDEKORT -> "Begrunnelse"
}
