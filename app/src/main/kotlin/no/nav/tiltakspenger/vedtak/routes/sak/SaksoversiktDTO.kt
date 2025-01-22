package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.benk.toBenkBehandlingstype
import no.nav.tiltakspenger.vedtak.routes.behandling.BehandlingstypeDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.toDTO

/**
 * @property periode Null dersom det er en søknad.
 * @property sakId Null dersom det er en søknad.
 * @property saksnummer Er med for visning i frontend. Null dersom det er en søknad.
 * @property id Unik identifikator for behandlingen. For søknader er dette søknadId. For førstegangsbehandlinger er dette behandlingId.
 */
data class SaksoversiktDTO(
    val periode: PeriodeDTO?,
    val status: String,
    val kravtidspunkt: String,
    val underkjent: Boolean?,
    val typeBehandling: BehandlingstypeDTO,
    val fnr: String,
    val id: String,
    val saksnummer: String?,
    val sakId: String?,
    val saksbehandler: String?,
    val beslutter: String?,
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
    kravtidspunkt = kravtidspunkt.toString(),
    typeBehandling = behandlingstype.toDTO(),
    fnr = fnr.verdi,
    saksnummer = saksnummer.toString(),
    id = id.toString(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    sakId = sakId.toString(),
)

fun List<Behandling>.toSaksoversiktDTO(): List<SaksoversiktDTO> =
    this.map { it.toSaksoversiktDTO() }

fun Behandling.toSaksoversiktDTO() = SaksoversiktDTO(
    periode = vurderingsperiode.toDTO(),
    status = status.toDTO().toString(),
    kravtidspunkt = vilkårssett.kravfristVilkår.avklartSaksopplysning.kravdato.toString(),
    underkjent = attesteringer.any { attestering -> attestering.isUnderkjent() },
    typeBehandling = behandlingstype.toBenkBehandlingstype().toDTO(),
    fnr = fnr.verdi,
    id = id.toString(),
    saksnummer = saksnummer.toString(),
    sakId = sakId.toString(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,

)
