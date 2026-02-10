package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett

import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.StønadTypeTiltakspenger

/**
 * @property stønadstype Stønadstypene for tiltakspenger representerer tiltakstypene.
 * @property barnetillegg Settes når utbetalingsperioden gjelder et barnetillegg.
 * @property brukersNavKontor Enhetsnummeret for NAV-kontoret som brukeren tilhører.
 * @property meldekortId Id på meldekortet utbetalingen gjelder.
 */
data class StønadsdataTiltakspengerV2Dto(
    val stønadstype: StønadTypeTiltakspenger,
    val barnetillegg: Boolean = false,
    val brukersNavKontor: String,
    val meldekortId: String,
)
