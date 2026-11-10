package no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør

import no.nav.tiltakspenger.libs.common.VedtakId

/**
 * Markerer at en innvilgelsesperiode er et reversert opphør.
 *
 * Et reversert opphør innvilger på nytt en periode som tidligere har vært innvilget, men som senere er opphørt eller stanset.
 * Perioden innvilges uten gyldig tiltaksdeltakelse i dagens saksopplysninger, for eksempel når deltakelsen er avkortet, feilregistrert eller fjernet i ettertid.
 * Tiltaksdeltakelsen er den samme som i det opprinnelige vedtaket, slik den var registrert da.
 *
 * Et reversert opphør kan bare innvilges ved omgjøring, og bare innenfor den opphørte delen av den opprinnelige innvilgelsen, se [perioderForReversertOpphør].
 * Vanlige innvilgelser valideres mot tiltaksdeltakelsene i saksopplysningene, mens et reversert opphør valideres mot vedtakshistorikken, se [validerReversertOpphør].
 * Endringer i tiltaksdeltakelsen fører ikke til automatiske revurderinger av perioder som er innvilget som reversert opphør.
 *
 * @param opprinneligVedtakId Vedtaket som opprinnelig innvilget perioden med gyldig tiltaksdeltakelse.
 */
data class ReversertOpphør(
    val opprinneligVedtakId: VedtakId,
)
