package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
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
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val typeBehandling: RammebehandlingstypeDTO,
    val opprettet: LocalDateTime,
    val periode: PeriodeDTO?,
    val status: String?,
    val kravtidspunkt: LocalDateTime?,
    val underkjent: Boolean?,
    val resultat: RammebehandlingResultatTypeDTO?,
    val saksbehandler: String?,
    val beslutter: String?,
    val erSattPåVent: Boolean?,
)

fun List<Rammebehandling>.toSaksoversiktDTO(): List<SaksoversiktDTO> =
    this.map { it.toSaksoversiktDTO() }

@JvmName("toSaksoversiktDTOForSøknad")
fun List<Søknad>.toSaksoversiktDTO(): List<SaksoversiktDTO> =
    this.map { it.toSaksoversiktDTO() }

fun Rammebehandling.toSaksoversiktDTO() = SaksoversiktDTO(
    id = id.toString(),
    sakId = sakId.toString(),
    saksnummer = saksnummer.toString(),
    typeBehandling = behandlingstype.tilBehandlingstypeDTO(),
    opprettet = opprettet,
    periode = virkningsperiode?.toDTO(),
    status = status.toBehandlingsstatusDTO().toString(),
    kravtidspunkt = if (this is Søknadsbehandling) kravtidspunkt else null,
    underkjent = attesteringer.any { attestering -> attestering.isUnderkjent() },
    resultat = this.resultat.tilBehandlingResultatDTO(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    erSattPåVent = ventestatus.erSattPåVent,
)

fun Søknad.toSaksoversiktDTO() = SaksoversiktDTO(
    id = id.toString(),
    sakId = sakId.toString(),
    saksnummer = saksnummer.toString(),
    typeBehandling = RammebehandlingstypeDTO.SØKNAD,
    opprettet = opprettet,
    periode = null,
    resultat = null,
    status = null,
    kravtidspunkt = tidsstempelHosOss,
    underkjent = null,
    saksbehandler = null,
    beslutter = null,
    erSattPåVent = null,
)
