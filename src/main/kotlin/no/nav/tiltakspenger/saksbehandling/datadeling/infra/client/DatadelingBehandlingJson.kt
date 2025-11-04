package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingBehandlingJson.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingBehandlingJson.Behandlingstype
import java.time.LocalDate
import java.time.LocalDateTime

data class DatadelingBehandlingJson(
    val behandlingId: String,
    val sakId: String,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val behandlingStatus: Behandlingsstatus,
    val saksbehandler: String?,
    val beslutter: String?,
    val iverksattTidspunkt: LocalDateTime?,
    val fnr: String,
    val saksnummer: String,
    val opprettetTidspunktSaksbehandlingApi: LocalDateTime,
    val behandlingstype: Behandlingstype,
    val sistEndret: LocalDateTime,
) {
    enum class Behandlingsstatus {
        UNDER_AUTOMATISK_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        IKKE_RETT_TIL_TILTAKSPENGER,
    }

    enum class Behandlingstype {
        SOKNADSBEHANDLING,
        REVURDERING,
        MELDEKORTBEHANDLING,
    }
}

fun Rammebehandling.toBehandlingJson(): String {
    return DatadelingBehandlingJson(
        behandlingId = id.toString(),
        sakId = sakId.toString(),
        saksnummer = saksnummer.verdi,
        fraOgMed = if (this is Søknadsbehandling) {
            this.getFraOgMed()
        } else {
            virkningsperiode?.fraOgMed
        },
        tilOgMed = if (this is Søknadsbehandling) {
            this.getTilOgMed()
        } else {
            virkningsperiode?.tilOgMed
        },
        behandlingStatus = status.toDatadelingDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        fnr = fnr.verdi,
        opprettetTidspunktSaksbehandlingApi = opprettet,
        behandlingstype = if (this is Søknadsbehandling) {
            Behandlingstype.SOKNADSBEHANDLING
        } else {
            Behandlingstype.REVURDERING
        },
        sistEndret = sistEndret,

    ).let { serialize(it) }
}

private fun Søknadsbehandling.getFraOgMed() = virkningsperiode?.fraOgMed
    ?: saksopplysninger.tiltaksdeltagelser.tidligsteFraOgMed
    ?: søknad.tiltak?.deltakelseFom
    ?: søknad.tiltaksdeltagelseperiodeDetErSøktOm()!!.fraOgMed

private fun Søknadsbehandling.getTilOgMed() = virkningsperiode?.tilOgMed
    ?: saksopplysninger.tiltaksdeltagelser.senesteTilOgMed
    ?: søknad.tiltak?.deltakelseTom
    ?: søknad.tiltaksdeltagelseperiodeDetErSøktOm()!!.tilOgMed

fun Rammebehandlingsstatus.toDatadelingDTO(): Behandlingsstatus =
    when (this) {
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> Behandlingsstatus.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> Behandlingsstatus.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> Behandlingsstatus.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> Behandlingsstatus.UNDER_BESLUTNING
        Rammebehandlingsstatus.VEDTATT -> Behandlingsstatus.VEDTATT
        Rammebehandlingsstatus.AVBRUTT -> Behandlingsstatus.AVBRUTT
    }
