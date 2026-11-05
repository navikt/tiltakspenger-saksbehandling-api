package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.infra.route.toÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkForBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLokalKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilTiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltakDeltakerstatusDto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.toDto
import java.time.Clock
import java.time.LocalDateTime

data class TiltakshistorikkForBehandlingDTO(
    val innslag: List<TiltakshistorikkinnslagDTO>,
    val harEndringEtterSistEndret: Boolean,
)

/**
 * [deltakelseStatus] er statusen tolket slik den ville blitt tolket da innslaget ble innhentet, og er null når vi ikke kjenner igjen statusen fra kilden.
 * [kildestatus] er statuskoden slik den står i kontrakten med kilden.
 * [utløsteBehandlingen] er true for innslaget som førte til at behandlingen ble opprettet automatisk.
 */
data class TiltakshistorikkinnslagDTO(
    val id: String,
    val internDeltakelseId: String,
    val eksternDeltagelseId: String,
    val innhentet: LocalDateTime,
    val typeNavn: String,
    val periode: ÅpenPeriodeDTO,
    val deltakelseStatus: TiltakDeltakerstatusDto?,
    val kildestatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val gjennomforingsprosent: Float?,
    val kilde: String,
    val endringer: List<TiltaksdeltakerEndringDTO>,
    val utløsteBehandlingen: Boolean,
    val behandlingId: String?,
    val oppgaveId: String?,
)

fun TiltakshistorikkForBehandling.tilDTO(): TiltakshistorikkForBehandlingDTO = TiltakshistorikkForBehandlingDTO(
    innslag = innslag.map { it.tilDTO(utløsteBehandlingen = it == utløsendeInnslag) },
    harEndringEtterSistEndret = harEndringEtterSistEndret,
)

private fun Tiltakshistorikkinnslag.tilDTO(utløsteBehandlingen: Boolean): TiltakshistorikkinnslagDTO {
    val kildestatus = verdi.kildestatus
    // En kladd fra Komet skal aldri nå oss, men visningen av historikken skal ikke feile om den likevel gjør det.
    val erKladd = kildestatus is Kometstatus.Kjent && kildestatus.type == Kometstatus.Type.KLADD
    return TiltakshistorikkinnslagDTO(
        id = id.toString(),
        internDeltakelseId = tiltaksdeltakerId.toString(),
        eksternDeltagelseId = eksternDeltakerId,
        innhentet = innhentet,
        typeNavn = verdi.tiltakstypenavn,
        periode = lukketEllerÅpenPeriode(verdi.fraOgMed, verdi.tilOgMed).toÅpenPeriodeDTO(),
        deltakelseStatus = if (erKladd) {
            null
        } else {
            kildestatus.tilTiltakDeltakerstatus(verdi.fraOgMed, Clock.fixed(innhentet.atZone(zoneIdOslo).toInstant(), zoneIdOslo))?.toDto()
        },
        kildestatus = kildestatus.kodeIKontrakten,
        deltakelseProsent = verdi.omfang.deltakelsesprosent,
        antallDagerPerUke = verdi.omfang.dagerPerUke,
        gjennomforingsprosent = verdi.omfang.deltidsprosentPåGjennomføring,
        kilde = kildestatus.kilde.tilLokalKilde().name,
        endringer = endring?.toDTO() ?: emptyList(),
        utløsteBehandlingen = utløsteBehandlingen,
        behandlingId = behandlingId?.toString(),
        oppgaveId = oppgaveId?.toString(),
    )
}
