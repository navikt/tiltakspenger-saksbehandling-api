package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.benk.toBenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import java.time.LocalDateTime

/**
 *
 * TODO jah: Dette er vel egentlig en behandlingsoversikt, ikke en saksoversikt. Og burde flyttes til behandling-pakken.
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

internal fun Saksoversikt.toSaksoversiktDTO(): List<SaksoversiktDTO> = this.map { it.toSaksoversiktDTO() }

fun BehandlingEllerSøknadForSaksoversikt.toSaksoversiktDTO() = SaksoversiktDTO(
    periode = periode?.toDTO(),
    status =
    when (val s = status) {
        is BehandlingEllerSøknadForSaksoversikt.Status.Søknad -> "SØKNAD"
        is BehandlingEllerSøknadForSaksoversikt.Status.Behandling -> s.behandlingsstatus.toBehandlingsstatusDTO().toString()
    },
    underkjent = underkjent,
    kravtidspunkt = kravtidspunkt,
    typeBehandling = behandlingstype.toBehandlingstypeDTO(),
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
    status = status.toBehandlingsstatusDTO().toString(),
    kravtidspunkt = if (this is Søknadsbehandling) kravtidspunkt else null,
    underkjent = attesteringer.any { attestering -> attestering.isUnderkjent() },
    typeBehandling = behandlingstype.toBenkBehandlingstype().toBehandlingstypeDTO(),
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
