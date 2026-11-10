package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * Forventede feil når saksbehandler innvilger en periode som reversert opphør.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 */
sealed interface KanIkkeReversereOpphør : KanIkkeOppdatereBehandling {

    /** Et reversert opphør kan bare innvilges ved omgjøring. */
    data object KunVedOmgjøring : KanIkkeReversereOpphør

    /** Periodene er ikke innenfor den opphørte delen av en tidligere innvilgelse for tiltaksdeltakelsen. */
    data class PeriodeneKanIkkeReverseres(
        val perioder: NonEmptyList<PeriodeSomIkkeKanReverseres>,
    ) : KanIkkeReversereOpphør

    data class PeriodeSomIkkeKanReverseres(
        val periode: Periode,
        val internDeltakelseId: TiltaksdeltakerId,
    )
}
