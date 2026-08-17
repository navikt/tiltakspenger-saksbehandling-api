package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.felles.ÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import java.time.LocalDate

data class TiltaksdeltakelseFraRegister(
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
) {
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
    fun overlapperMed(other: TiltaksdeltakelseFraRegister): Boolean? = åpenPeriode.overlapperMed(other.åpenPeriode)

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
