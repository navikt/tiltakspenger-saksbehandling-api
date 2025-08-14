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
    /**
     * null dersom [deltagelseFraOgMed] eller [deltagelseTilOgMed] er null.
     */
    val periode: Periode? by lazy {
        if (deltagelseFraOgMed != null && deltagelseTilOgMed != null) {
            Periode(deltagelseFraOgMed, deltagelseTilOgMed)
        } else {
            null
        }
    }

    /**
     * @return true hvis vi med sikkerhet kan si de overlapper, false dersom vi med sikkerhet vet at de ikke overlapper og null dersom de kan overlappe.
     */
    fun overlapperMedPeriode(periode: Periode): Boolean? {
        // Hvis begge datoene mangler kan vi ikke si noe om overlapp og må dermed anta at de kan overlappe
        if (deltagelseFraOgMed == null && deltagelseTilOgMed == null) {
            return null
        }

        if (this.periode != null) return this.periode!!.overlapperMed(periode)

        if (deltagelseFraOgMed == null && deltagelseTilOgMed != null) {
            if (periode.inneholder(deltagelseTilOgMed)) return true
            if (deltagelseTilOgMed.isBefore(periode.fraOgMed)) return false
        }

        if (deltagelseFraOgMed != null && deltagelseTilOgMed == null) {
            if (periode.inneholder(deltagelseFraOgMed)) return true
            if (deltagelseFraOgMed.isAfter(periode.tilOgMed)) return false
        }

        return null
    }

    /**
     * Siden en tiltaksdeltagelse ikke nødvendigvis har en fraOgMed eller tilOgMed-dato, kan vi ikke alltid si med sikkerhet om to tiltaksdeltagelser overlapper.
     * Denne metoden håndterer tvilstilfellene og returnerer null dersom vi ikke kan si sikkert om de overlapper.
     * Den returnerer true/false der vi er sikre.
     */
    fun overlapperMed(other: Tiltaksdeltagelse): Boolean? {
        return when {
            this.periode != null && other.periode != null -> this.periode!!.overlapperMed(other.periode!!)
            this.periode != null && other.periode == null -> other.overlapperMedPeriode(this.periode!!)
            this.periode == null && other.periode != null -> this.overlapperMedPeriode(other.periode!!)
            this.deltagelseFraOgMed != null && other.deltagelseFraOgMed != null -> if (this.deltagelseFraOgMed == other.deltagelseFraOgMed) true else null
            this.deltagelseTilOgMed != null && other.deltagelseTilOgMed != null -> if (this.deltagelseTilOgMed == other.deltagelseTilOgMed) true else null
            this.deltagelseFraOgMed != null && other.deltagelseTilOgMed != null -> if (this.deltagelseFraOgMed == other.deltagelseTilOgMed) true else null
            this.deltagelseTilOgMed != null && other.deltagelseFraOgMed != null -> if (this.deltagelseTilOgMed == other.deltagelseFraOgMed) true else null
            else -> null
        }
    }
}
