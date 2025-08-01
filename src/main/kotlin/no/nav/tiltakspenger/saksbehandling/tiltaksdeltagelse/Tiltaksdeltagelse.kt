package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate

/**
 * @param eksternDeltagelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltagelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen.
 * @param typeNavn Navn på tiltakstypen, f.eks. "Arbeidsforberedende trening"
 * @param gjennomføringId Ekstern id fra Valp. Dette er gjennomføringen sin ID, eksempelvis Rema 1000 i Strandveien. En person knyttes til en gjennomføring og det kalles da en deltagelse. Per nå mottar vi ikke denne fra Arena, men kun fra Komet.
 * @param deltagelseFraOgMed startdato for deltakelsen. Kan mangle, særlig når deltaker venter på oppstart
 * @param deltagelseTilOgMed sluttdato for deltakelsen. Kan mangle.
 */
data class Tiltaksdeltagelse(
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRett,
    val rettPåTiltakspenger: Boolean,
    val deltagelseFraOgMed: LocalDate?,
    val deltagelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
) {
    val periode: Periode?
        get() = if (deltagelseFraOgMed != null && deltagelseTilOgMed != null) {
            Periode(
                deltagelseFraOgMed,
                deltagelseTilOgMed,
            )
        } else {
            null
        }

    fun overlapperMedPeriode(periode: Periode): Boolean {
        // Hvis begge datoene mangler kan vi ikke si noe om overlapp og må dermed anta at de kan overlappe
        if (deltagelseFraOgMed == null && deltagelseTilOgMed == null) {
            return true
        }

        if (deltagelseFraOgMed == null && deltagelseTilOgMed != null) {
            return periode.inneholder(deltagelseTilOgMed) || periode.tilOgMed.isBefore(deltagelseTilOgMed)
        }

        if (deltagelseFraOgMed != null && deltagelseTilOgMed == null) {
            return periode.inneholder(deltagelseFraOgMed) || deltagelseFraOgMed.isBefore(periode.fraOgMed)
        }

        return Periode(deltagelseFraOgMed!!, deltagelseTilOgMed!!).overlapperMed(periode)
    }
}
