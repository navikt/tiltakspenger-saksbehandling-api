package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate

/**
 * Tiltaksdeltakelse som også kan ha med [visningsnavn]. Visningsnavn er typisk på formen "Tiltakstype hos Arrangør".
 * Ettersom navnet til arrangøren kan inneholde geolokaliserende informajson  brukes kun tiltakstypen hvis bruker har
 * adressebeskyttelse.
 *
 * se [Tiltaksdeltakelse]
 * @param eksternDeltakelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltakelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (UUID). Kalles ekstern_id i databasen.
 * @param typeNavn Navn på tiltakstypen, f.eks. "Arbeidsforberedende trening"
 * @param deltakelseFraOgMed startdato for deltakelsen. Kan mangle, særlig når deltaker venter på oppstart
 * @param deltakelseTilOgMed sluttdato for deltakelsen. Kan mangle.
 */
data class TiltaksdeltakelseMedArrangørnavn(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRett,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val visningsnavn: String,
) {
    val periode: Periode? by lazy {
        if (deltakelseFraOgMed != null && deltakelseTilOgMed != null) {
            Periode(deltakelseFraOgMed, deltakelseTilOgMed)
        } else {
            null
        }
    }
}
