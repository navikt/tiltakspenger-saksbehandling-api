package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.OpprinneligInnvilgelse

/**
 * En periode som kan innvilges som reversert opphør ved omgjøring.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 *
 * @param typeNavn Navnet på tiltakstypen slik den var registrert i den opprinnelige innvilgelsen.
 * Tiltaksdeltakelsen finnes ikke nødvendigvis i saksopplysningene lenger.
 */
data class PeriodeForReversertOpphørDTO(
    val internDeltakelseId: String,
    val periode: PeriodeDTO,
    val typeNavn: String,
)

private data class Deltakelse(
    val internDeltakelseId: String,
    val typeNavn: String,
)

/**
 * Gir én periode per tiltaksdeltakelse, så periodene for ulike tiltaksdeltakelser kan overlappe.
 * Tilstøtende perioder for samme tiltaksdeltakelse slås sammen, selv om de er opprinnelig innvilget i ulike vedtak.
 */
fun Periodisering<OpprinneligInnvilgelse>.tilPerioderForReversertOpphørDTO(): List<PeriodeForReversertOpphørDTO> {
    val internDeltakelseIder = this.verdier.flatMap { opprinnelig -> opprinnelig.tiltaksdeltakelser.map { it.internDeltakelseId } }.distinct()

    return internDeltakelseIder.flatMap { internDeltakelseId ->
        this
            .filter { opprinnelig, _ -> opprinnelig.innvilgerTiltaksdeltakelse(internDeltakelseId) }
            .map { opprinnelig, _ ->
                Deltakelse(
                    internDeltakelseId = internDeltakelseId.toString(),
                    typeNavn = opprinnelig.hentTiltaksdeltakelse(internDeltakelseId)!!.typeNavn,
                )
            }
            .slåSammenTilstøtendePerioder()
            .perioderMedVerdi
            .map {
                PeriodeForReversertOpphørDTO(
                    internDeltakelseId = it.verdi.internDeltakelseId,
                    periode = it.periode.toDTO(),
                    typeNavn = it.verdi.typeNavn,
                )
            }
    }.sortedBy { it.periode.fraOgMed }
}
