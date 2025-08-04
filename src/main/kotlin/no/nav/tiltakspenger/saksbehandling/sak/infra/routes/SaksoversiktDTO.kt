package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import java.time.LocalDateTime

/**
 *
 * TODO jah: Dette er vel egentlig en behandlingsoversikt, ikke en saksoversikt. Og burde flyttes til behandling-pakken.
 * @property periode Null dersom det er en søknad.
 * @property sakId Null dersom det er en søknad.
 * @property saksnummer Er med for visning i frontend. Null dersom det er en søknad.
 * @property id Unik identifikator for behandlingen. For søknader er dette søknadId. For søknadsbehandlinger er dette behandlingId.
 */
data class SaksoversiktDTO(
    val periode: PeriodeDTO?,
    val status: String,
    val kravtidspunkt: LocalDateTime?,
    val underkjent: Boolean?,
    val typeBehandling: BehandlingstypeDTO,
    val resultat: BehandlingResultatDTO?,
    val fnr: String,
    val id: String,
    val saksnummer: String,
    val sakId: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val opprettet: LocalDateTime,
    val erSattPåVent: Boolean?,
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
    typeBehandling = behandlingstype.tilBehandlingstypeDTO(),
    resultat = this.tilBehandlingResultatDTO(),
    fnr = fnr.verdi,
    id = id.toString(),
    saksnummer = saksnummer.toString(),
    sakId = sakId.toString(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    opprettet = opprettet,
    erSattPåVent = erSattPåVent,
)

fun Søknad.toSaksoversiktDTO() = SaksoversiktDTO(
    periode = null,
    status = "SØKNAD",
    kravtidspunkt = tidsstempelHosOss,
    underkjent = null,
    typeBehandling = BehandlingstypeDTO.SØKNAD,
    resultat = null,
    fnr = fnr.verdi,
    id = id.toString(),
    saksnummer = saksnummer.toString(),
    sakId = sakId.toString(),
    saksbehandler = null,
    beslutter = null,
    opprettet = opprettet,
    erSattPåVent = null,
)
