package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.felles.ÅpenPeriode
import java.time.LocalDate

/**
 * @param eksternDeltakelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api).
 * Dette er tiltaksdeltakelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?).
 * Kalles ekstern_id i databasen.
 * Lagres kun for sporbarhet.
 * @param typeNavn Navn på tiltakstypen, f.eks. "Arbeidsforberedende trening"
 * @param gjennomføringId Ekstern id fra Valp.
 * Dette er gjennomføringen sin ID, eksempelvis Rema 1000 i Strandveien.
 * En person knyttes til en gjennomføring og det kalles da en deltakelse.
 * Per nå mottar vi ikke denne fra Arena, men kun fra Komet.
 * @param deltakelseFraOgMed startdato for deltakelsen.
 * Kan mangle, særlig når deltaker venter på oppstart
 * @param deltakelseTilOgMed sluttdato for deltakelsen.
 * Kan mangle.
 * @param internDeltakelseId vår interne id for tiltaksdeltakelsen som finnes i tiltaksdeltaker-tabellen.
 * Siden eksternId kan endres skal man alltid hente eksternId fra tiltaksdeltaker-tabellen for å finne nåværende eksternId.
 */
data class Tiltaksdeltakelse(
    val eksternDeltakelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRettDTO,
    val rettPåTiltakspenger: Boolean,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
    val deltidsprosentGjennomforing: Double?,
    val internDeltakelseId: TiltaksdeltakerId,
) {
    val kanInnvilges: Boolean = deltakelseStatus.deltarEllerHarDeltatt() && deltakelseFraOgMed != null && deltakelseTilOgMed != null

    private val åpenPeriode: ÅpenPeriode by lazy { ÅpenPeriode(deltakelseFraOgMed, deltakelseTilOgMed) }

    /**
     * null dersom [deltakelseFraOgMed] eller [deltakelseTilOgMed] er null.
     */
    val periode: Periode? by lazy { åpenPeriode.periode }

    /**
     * @return true hvis vi med sikkerhet kan si de overlapper, false dersom vi med sikkerhet vet at de ikke overlapper og null dersom de kan overlappe.
     */
    fun overlapperMed(periode: Periode): Boolean? = åpenPeriode.overlapperMed(periode)

    /**
     * Siden en tiltaksdeltakelse ikke nødvendigvis har en fraOgMed eller tilOgMed-dato, kan vi ikke alltid si med sikkerhet om to tiltaksdeltakelser overlapper.
     * Denne metoden håndterer tvilstilfellene og returnerer null dersom vi ikke kan si sikkert om de overlapper.
     * Den returnerer true/false der vi er sikre.
     */
    fun overlapperMed(other: Tiltaksdeltakelse): Boolean? = åpenPeriode.overlapperMed(other.åpenPeriode)
}
