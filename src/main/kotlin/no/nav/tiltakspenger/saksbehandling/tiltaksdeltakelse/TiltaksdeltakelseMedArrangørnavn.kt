package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periode.LukketEllerÅpenPeriode
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.felles.krevLukketNårBeggeDatoeneErKjent
import no.nav.tiltakspenger.saksbehandling.felles.lukketPeriode

/**
 * Tiltaksdeltakelse som også kan ha med [visningsnavn].
 * Visningsnavn er typisk på formen "Tiltakstype hos Arrangør".
 * Ettersom navnet til arrangøren kan inneholde geolokaliserende informajson  brukes kun tiltakstypen hvis bruker har adressebeskyttelse.
 *
 * se [TiltaksdeltakelseIntern]
 * @param eksternDeltakelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api).
 * Dette er tiltaksdeltakelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (UUID).
 * Kalles ekstern_id i databasen.
 * @param typeNavn Navn på tiltakstypen, f.eks. "Arbeidsforberedende trening"
 * @param typeKode null dersom tiltakstypen fratar rett til tiltakspenger (i første omgang lønnstilskudd).
 * Slike deltakelser vises til saksbehandler, men kan ikke velges.
 * @param periode en [Periode] når både start- og sluttdato er kjent, ellers en [ÅpenPeriode].
 * Datoene kan mangle, særlig startdatoen når deltaker venter på oppstart.
 */
data class TiltaksdeltakelseMedArrangørnavn(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRettDTO?,
    val periode: LukketEllerÅpenPeriode,
    val visningsnavn: String,
    val status: TiltakDeltakerstatus,
) {
    init {
        periode.krevLukketNårBeggeDatoeneErKjent()
    }

    val fratarRett: Boolean get() = typeKode == null

    /**
     * null dersom start- eller sluttdato mangler.
     */
    val lukketPeriode: Periode? get() = periode.lukketPeriode
}
