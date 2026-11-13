package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse

/**
 * En tiltaksdeltakelse slik den lokale fake-klienten leverer den.
 * Tiltakstyper som gir rett representeres som [TiltaksdeltakelseIntern], og de som fratar rett som [TiltaksdeltakelseSomFratarRett].
 * [tittel] ligger ved siden av deltakelsen, siden den ikke lagres i saksopplysningene, men slås opp hos kilden ved behov.
 */
sealed interface LokalTiltaksdeltakelse {
    val eksternDeltakelseId: String

    /** Koden som velges i dev-endepunktet: navnet på [no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett] eller kildens kode for en tiltakstype som fratar rett. */
    val tiltakskode: String
    val kilde: Tiltakskilde

    /** Hele tiltaksnavnet slik kilden leverer det, f.eks. `Oppfølging hos Arrangør AS`. */
    val tittel: String?

    fun tilLibsDeltakelse(): Tiltaksdeltakelse

    fun tilTiltaksdeltakelseMedArrangørnavn(): TiltaksdeltakelseMedArrangørnavn

    /** Felter som er null beholder dagens verdi. */
    fun endre(command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand): LokalTiltaksdeltakelse

    data class GirRett(
        val deltakelse: TiltaksdeltakelseIntern,
        override val tittel: String?,
    ) : LokalTiltaksdeltakelse {
        override val eksternDeltakelseId: String get() = deltakelse.eksternDeltakelseId
        override val tiltakskode: String get() = deltakelse.tiltakstype.name
        override val kilde: Tiltakskilde get() = deltakelse.kilde

        override fun tilLibsDeltakelse(): Tiltaksdeltakelse.GirRett = deltakelse.tilLibsDeltakelse(tittel)

        override fun tilTiltaksdeltakelseMedArrangørnavn() = TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = deltakelse.eksternDeltakelseId,
            typeNavn = deltakelse.typeNavn,
            tiltakstype = deltakelse.tiltakstype,
            tiltakskodeFraKilden = tiltakskode,
            periode = deltakelse.periode,
            visningsnavn = tittel ?: deltakelse.typeNavn,
            status = deltakelse.deltakelseStatus,
        )

        override fun endre(command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand) = GirRett(
            deltakelse.copy(
                typeNavn = command.typeNavn ?: deltakelse.typeNavn,
                periode = lukketEllerÅpenPeriode(
                    command.fraOgMed ?: deltakelse.periode.fraOgMed,
                    command.tilOgMed ?: deltakelse.periode.tilOgMed,
                ),
                deltakelseStatus = command.status ?: deltakelse.deltakelseStatus,
                deltakelseProsent = command.deltakelseProsent ?: deltakelse.deltakelseProsent,
                antallDagerPerUke = command.antallDagerPerUke ?: deltakelse.antallDagerPerUke,
            ),
            tittel = command.tittel ?: tittel,
        )
    }

    data class FratarRett(
        val deltakelse: TiltaksdeltakelseSomFratarRett,
        override val tittel: String?,
    ) : LokalTiltaksdeltakelse {
        override val eksternDeltakelseId: String get() = deltakelse.eksternDeltakelseId
        override val tiltakskode: String get() = deltakelse.tiltakskodeFraKilden
        override val kilde: Tiltakskilde get() = deltakelse.kilde

        override fun tilLibsDeltakelse(): Tiltaksdeltakelse.FratarRett = deltakelse.tilLibsDeltakelse(tittel)

        override fun tilTiltaksdeltakelseMedArrangørnavn() = TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = deltakelse.eksternDeltakelseId,
            typeNavn = deltakelse.typeNavn,
            tiltakstype = null,
            tiltakskodeFraKilden = tiltakskode,
            periode = deltakelse.periode,
            visningsnavn = tittel ?: deltakelse.typeNavn,
            status = deltakelse.deltakelseStatus,
        )

        override fun endre(command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand) = FratarRett(
            deltakelse.copy(
                typeNavn = command.typeNavn ?: deltakelse.typeNavn,
                periode = lukketEllerÅpenPeriode(
                    command.fraOgMed ?: deltakelse.periode.fraOgMed,
                    command.tilOgMed ?: deltakelse.periode.tilOgMed,
                ),
                deltakelseStatus = command.status ?: deltakelse.deltakelseStatus,
                deltakelseProsent = command.deltakelseProsent ?: deltakelse.deltakelseProsent,
                antallDagerPerUke = command.antallDagerPerUke ?: deltakelse.antallDagerPerUke,
            ),
            tittel = command.tittel ?: tittel,
        )
    }
}
