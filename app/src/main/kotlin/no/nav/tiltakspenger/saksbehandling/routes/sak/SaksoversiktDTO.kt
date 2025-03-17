package no.nav.tiltakspenger.saksbehandling.routes.sak

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.BehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.toBenkBehandlingstype
import java.time.LocalDateTime

/**
 * @property periode Null dersom det er en søknad.
 * @property sakId Null dersom det er en søknad.
 * @property saksnummer Er med for visning i frontend. Null dersom det er en søknad.
 * @property id Unik identifikator for behandlingen. For søknader er dette søknadId. For førstegangsbehandlinger er dette behandlingId.
 */
data class SaksoversiktDTO(
    val periode: PeriodeDTO?,
    val status: String,
    val kravtidspunkt: LocalDateTime?,
    val underkjent: Boolean?,
    val typeBehandling: BehandlingstypeDTO,
    val fnr: String,
    val id: String,
    val saksnummer: String,
    val sakId: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val opprettet: LocalDateTime,
)

internal fun Saksoversikt.toDTO(): List<SaksoversiktDTO> = this.map { it.toSaksoversiktDTO() }

fun BehandlingEllerSøknadForSaksoversikt.toSaksoversiktDTO() = SaksoversiktDTO(
    periode = periode?.toDTO(),
    status =
    when (val s = status) {
        is BehandlingEllerSøknadForSaksoversikt.Status.Søknad -> "SØKNAD"
        is BehandlingEllerSøknadForSaksoversikt.Status.Behandling -> s.behandlingsstatus.toDTO().toString()
    },
    underkjent = underkjent,
    kravtidspunkt = kravtidspunkt,
    typeBehandling = behandlingstype.toDTO(),
    fnr = fnr.verdi,
    saksnummer = saksnummer.toString(),
    id = id.toString(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    sakId = sakId.toString(),
    opprettet = opprettet,
)

fun List<Behandling>.toSaksoversiktDTO(): List<SaksoversiktDTO> =
    this.map { it.toSaksoversiktDTO() }

@JvmName("toSaksoversiktDTOForSøknad")
fun List<Søknad>.toSaksoversiktDTO(): List<SaksoversiktDTO> =
    this.map { it.toSaksoversiktDTO() }

fun Behandling.toSaksoversiktDTO() = SaksoversiktDTO(
    periode = virkningsperiode?.toDTO(),
    status = status.toDTO().toString(),
    kravtidspunkt = kravtidspunkt,
    underkjent = attesteringer.any { attestering -> attestering.isUnderkjent() },
    typeBehandling = behandlingstype.toBenkBehandlingstype().toDTO(),
    fnr = fnr.verdi,
    id = id.toString(),
    saksnummer = saksnummer.toString(),
    sakId = sakId.toString(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    opprettet = opprettet,
)

fun Søknad.toSaksoversiktDTO() = SaksoversiktDTO(
    periode = null,
    status = "SØKNAD",
    kravtidspunkt = tidsstempelHosOss,
    underkjent = null,
    typeBehandling = BehandlingstypeDTO.SØKNAD,
    fnr = fnr.verdi,
    id = id.toString(),
    saksnummer = saksnummer.toString(),
    sakId = sakId.toString(),
    saksbehandler = null,
    beslutter = null,
    opprettet = opprettet,
)
