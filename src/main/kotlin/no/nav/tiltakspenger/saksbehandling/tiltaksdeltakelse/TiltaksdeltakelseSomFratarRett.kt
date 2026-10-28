package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periode.LukketEllerÅpenPeriode
import no.nav.tiltakspenger.libs.periode.Overlapp
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.felles.krevLukketNårBeggeDatoeneErKjent
import no.nav.tiltakspenger.saksbehandling.felles.overlapperMed

/**
 * En tiltaksdeltakelse som fratar rett til tiltakspenger, og som derfor skal vises til saksbehandler.
 * Speiler [no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse.FratarRett], som i første omgang gjelder lønnstilskudd (alle typer).
 * Kan aldri innvilges, og får derfor ingen intern id i tiltaksdeltaker-tabellen.
 *
 * @param eksternDeltakelseId Arena (TA1234567) eller Team Tiltak (UUID).
 * @param typeNavn Navn på tiltakstypen, f.eks. "Midlertidig lønnstilskudd".
 * @param tiltakskodeFraKilden Tiltakskoden slik kilden oppgir den, f.eks. "MIDLONTIL".
 * @param periode en [Periode] når både start- og sluttdato er kjent, ellers en [ÅpenPeriode].
 */
data class TiltaksdeltakelseSomFratarRett(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val tiltakskodeFraKilden: String,
    val periode: LukketEllerÅpenPeriode,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
) {
    init {
        periode.krevLukketNårBeggeDatoeneErKjent()
    }

    /**
     * Alle tiltakstyper som fratar rett er i dag lønnstilskudd, og libs bærer derfor ingen diskriminator for typen.
     * TODO: Når libs får flere tiltakstyper som fratar rett, må dette utledes fra en diskriminator på [no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse.FratarRett].
     * Klassifiser aldri på [tiltakskodeFraKilden], den er kun til visning.
     */
    val erLønnstilskudd: Boolean get() = true

    /**
     * Se [ÅpenPeriode.overlapperMed].
     */
    fun overlapperMed(periode: Periode): Overlapp = this.periode.overlapperMed(periode)
}
