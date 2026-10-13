package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomIkkeGirRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse

/**
 * En tiltaksdeltakelse slik den lokale fake-klienten leverer den.
 * Tiltakstyper som gir rett representeres som [TiltaksdeltakelseIntern], og lønnstilskudd som [TiltaksdeltakelseSomIkkeGirRett].
 */
sealed interface LokalTiltaksdeltakelse {
    val eksternDeltakelseId: String

    /** Koden som velges i dev-endepunktet: navnet på [no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO] eller kildens lønnstilskuddkode. */
    val tiltakskode: String
    val kilde: Tiltakskilde

    fun tilLibsDeltakelse(): Tiltaksdeltakelse

    fun tilTiltaksdeltakelseMedArrangørnavn(): TiltaksdeltakelseMedArrangørnavn

    /** Felter som er null beholder dagens verdi. */
    fun endre(command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand): LokalTiltaksdeltakelse

    data class GirRett(val deltakelse: TiltaksdeltakelseIntern) : LokalTiltaksdeltakelse {
        override val eksternDeltakelseId: String get() = deltakelse.eksternDeltakelseId
        override val tiltakskode: String get() = deltakelse.typeKode.name
        override val kilde: Tiltakskilde get() = deltakelse.kilde

        override fun tilLibsDeltakelse(): Tiltaksdeltakelse.GirRett = deltakelse.tilLibsDeltakelse()

        override fun tilTiltaksdeltakelseMedArrangørnavn() = TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = deltakelse.eksternDeltakelseId,
            typeNavn = deltakelse.typeNavn,
            typeKode = deltakelse.typeKode,
            deltakelseFraOgMed = deltakelse.deltakelseFraOgMed,
            deltakelseTilOgMed = deltakelse.deltakelseTilOgMed,
            visningsnavn = deltakelse.typeNavn,
            status = deltakelse.deltakelseStatus,
        )

        override fun endre(command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand) = GirRett(
            deltakelse.copy(
                typeNavn = command.typeNavn ?: deltakelse.typeNavn,
                deltakelseFraOgMed = command.fraOgMed ?: deltakelse.deltakelseFraOgMed,
                deltakelseTilOgMed = command.tilOgMed ?: deltakelse.deltakelseTilOgMed,
                deltakelseStatus = command.status ?: deltakelse.deltakelseStatus,
                deltakelseProsent = command.deltakelseProsent ?: deltakelse.deltakelseProsent,
                antallDagerPerUke = command.antallDagerPerUke ?: deltakelse.antallDagerPerUke,
            ),
        )
    }

    data class GirIkkeRett(val deltakelse: TiltaksdeltakelseSomIkkeGirRett) : LokalTiltaksdeltakelse {
        override val eksternDeltakelseId: String get() = deltakelse.eksternDeltakelseId
        override val tiltakskode: String get() = deltakelse.tiltakskodeFraKilden
        override val kilde: Tiltakskilde get() = deltakelse.kilde

        override fun tilLibsDeltakelse(): Tiltaksdeltakelse.GirIkkeRett = deltakelse.tilLibsDeltakelse()

        override fun tilTiltaksdeltakelseMedArrangørnavn() = TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = deltakelse.eksternDeltakelseId,
            typeNavn = deltakelse.typeNavn,
            typeKode = null,
            deltakelseFraOgMed = deltakelse.deltakelseFraOgMed,
            deltakelseTilOgMed = deltakelse.deltakelseTilOgMed,
            visningsnavn = deltakelse.typeNavn,
            status = deltakelse.deltakelseStatus,
        )

        override fun endre(command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand) = GirIkkeRett(
            deltakelse.copy(
                typeNavn = command.typeNavn ?: deltakelse.typeNavn,
                deltakelseFraOgMed = command.fraOgMed ?: deltakelse.deltakelseFraOgMed,
                deltakelseTilOgMed = command.tilOgMed ?: deltakelse.deltakelseTilOgMed,
                deltakelseStatus = command.status ?: deltakelse.deltakelseStatus,
                deltakelseProsent = command.deltakelseProsent ?: deltakelse.deltakelseProsent,
                antallDagerPerUke = command.antallDagerPerUke ?: deltakelse.antallDagerPerUke,
            ),
        )
    }
}
