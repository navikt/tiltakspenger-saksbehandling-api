package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate

/**
 * @param eksternDeltakelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltakelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen. Lagres kun for sporbarhet.
 * @param typeNavn Navn på tiltakstypen, f.eks. "Arbeidsforberedende trening"
 * @param gjennomføringId Ekstern id fra Valp. Dette er gjennomføringen sin ID, eksempelvis Rema 1000 i Strandveien. En person knyttes til en gjennomføring og det kalles da en deltakelse. Per nå mottar vi ikke denne fra Arena, men kun fra Komet.
 * @param deltakelseFraOgMed startdato for deltakelsen. Kan mangle, særlig når deltaker venter på oppstart
 * @param deltakelseTilOgMed sluttdato for deltakelsen. Kan mangle.
 * @param internDeltakelseId vår interne id for tiltaksdeltakelsen som finnes i tiltaksdeltaker-tabellen. Siden eksternId kan endres skal man alltid hente eksternId fra tiltaksdeltaker-tabellen for å finne nåværende eksternId.
 */
data class Tiltaksdeltakelse(
    val eksternDeltakelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRett,
    val rettPåTiltakspenger: Boolean,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
    val deltidsprosentGjennomforing: Double?,
    val internDeltakelseId: TiltaksdeltakerId?,
) {
    val kanInnvilges: Boolean = deltakelseStatus.deltarEllerHarDeltatt() && deltakelseFraOgMed != null && deltakelseTilOgMed != null

    /**
     * null dersom [deltakelseFraOgMed] eller [deltakelseTilOgMed] er null.
     */
    val periode: Periode? by lazy {
        if (deltakelseFraOgMed != null && deltakelseTilOgMed != null) {
            Periode(deltakelseFraOgMed, deltakelseTilOgMed)
        } else {
            null
        }
    }

    /**
     * @return true hvis vi med sikkerhet kan si de overlapper, false dersom vi med sikkerhet vet at de ikke overlapper og null dersom de kan overlappe.
     */
    fun overlapperMedPeriode(periode: Periode): Boolean? {
        // Hvis begge datoene mangler kan vi ikke si noe om overlapp og må dermed anta at de kan overlappe
        if (deltakelseFraOgMed == null && deltakelseTilOgMed == null) {
            return null
        }

        if (this.periode != null) return this.periode!!.overlapperMed(periode)

        if (deltakelseFraOgMed == null && deltakelseTilOgMed != null) {
            if (periode.inneholder(deltakelseTilOgMed)) return true
            if (deltakelseTilOgMed.isBefore(periode.fraOgMed)) return false
        }

        if (deltakelseFraOgMed != null && deltakelseTilOgMed == null) {
            if (periode.inneholder(deltakelseFraOgMed)) return true
            if (deltakelseFraOgMed.isAfter(periode.tilOgMed)) return false
        }

        return null
    }

    /**
     * Siden en tiltaksdeltakelse ikke nødvendigvis har en fraOgMed eller tilOgMed-dato, kan vi ikke alltid si med sikkerhet om to tiltaksdeltakelser overlapper.
     * Denne metoden håndterer tvilstilfellene og returnerer null dersom vi ikke kan si sikkert om de overlapper.
     * Den returnerer true/false der vi er sikre.
     */
    fun overlapperMed(other: Tiltaksdeltakelse): Boolean? {
        return when {
            this.periode != null && other.periode != null -> this.periode!!.overlapperMed(other.periode!!)
            this.periode != null && other.periode == null -> other.overlapperMedPeriode(this.periode!!)
            this.periode == null && other.periode != null -> this.overlapperMedPeriode(other.periode!!)
            this.deltakelseFraOgMed != null && other.deltakelseFraOgMed != null -> if (this.deltakelseFraOgMed == other.deltakelseFraOgMed) true else null
            this.deltakelseTilOgMed != null && other.deltakelseTilOgMed != null -> if (this.deltakelseTilOgMed == other.deltakelseTilOgMed) true else null
            this.deltakelseFraOgMed != null && other.deltakelseTilOgMed != null -> if (this.deltakelseFraOgMed == other.deltakelseTilOgMed) true else null
            this.deltakelseTilOgMed != null && other.deltakelseFraOgMed != null -> if (this.deltakelseTilOgMed == other.deltakelseFraOgMed) true else null
            else -> null
        }
    }
}
