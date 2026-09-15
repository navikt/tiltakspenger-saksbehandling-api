package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseLegacy
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import java.time.LocalDate

data class TiltaksdeltakelseFraRegister(
    override val eksternDeltakelseId: String,
    override val gjennomføringId: String?,
    override val typeNavn: String,
    override val typeKode: TiltakstypeSomGirRettDTO,
    override val rettPåTiltakspenger: Boolean,
    override val deltakelseFraOgMed: LocalDate?,
    override val deltakelseTilOgMed: LocalDate?,
    override val deltakelseStatus: TiltakDeltakerstatus,
    override val deltakelseProsent: Float?,
    override val antallDagerPerUke: Float?,
    override val kilde: Tiltakskilde,
    override val deltidsprosentGjennomforing: Double?,
) : TiltaksdeltakelseLegacy {

    fun tilTiltaksdeltakelseIntern(internDeltakelseId: TiltaksdeltakerId): TiltaksdeltakelseIntern =
        TiltaksdeltakelseIntern(
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
