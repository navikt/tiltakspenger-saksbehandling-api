package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import java.time.LocalDate

data class TiltaksdeltakelseFraRegister(
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
) {
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
    fun overlapperMed(other: TiltaksdeltakelseFraRegister): Boolean? {
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

    fun toTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): Tiltaksdeltakelse =
        Tiltaksdeltakelse(
            eksternDeltakelseId = eksternDeltakelseId,
            gjennomføringId = gjennomføringId,
            typeNavn = typeNavn,
            typeKode = typeKode,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelseFraOgMed = deltakelseFraOgMed,
            deltakelseTilOgMed = deltakelseTilOgMed,
            deltakelseStatus = deltakelseStatus,
            deltakelseProsent = deltakelseProsent,
            antallDagerPerUke = antallDagerPerUke,
            kilde = kilde,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = internDeltakelseId,
        )
}
