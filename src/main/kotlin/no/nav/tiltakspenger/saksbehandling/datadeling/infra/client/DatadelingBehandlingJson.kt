package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingBehandlingJson.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingBehandlingJson.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
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
        behandlingStatus = status.toDatadelingStatus(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        opprettetTidspunktSaksbehandlingApi = opprettet,
        behandlingstype = if (this is Søknadsbehandling) {
            Behandlingstype.SOKNADSBEHANDLING
        } else {
            Behandlingstype.REVURDERING
        },
        sistEndret = sistEndret,

    ).let { serialize(it) }
}

fun MeldekortBehandling.toBehandlingJson(): String {
    return DatadelingBehandlingJson(
        behandlingId = id.toString(),
        sakId = sakId.toString(),
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        behandlingStatus = status.toDatadelingStatus(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        opprettetTidspunktSaksbehandlingApi = opprettet,
        behandlingstype = Behandlingstype.MELDEKORTBEHANDLING,
        sistEndret = sistEndret,

    ).let { serialize(it) }
}

private fun Søknadsbehandling.getFraOgMed() = virkningsperiode?.fraOgMed
    ?: saksopplysninger.tiltaksdeltakelser.tidligsteFraOgMed
    ?: søknad.tiltak?.deltakelseFom
    ?: søknad.tiltaksdeltakelseperiodeDetErSøktOm()!!.fraOgMed

private fun Søknadsbehandling.getTilOgMed() = virkningsperiode?.tilOgMed
    ?: saksopplysninger.tiltaksdeltakelser.senesteTilOgMed
    ?: søknad.tiltak?.deltakelseTom
    ?: søknad.tiltaksdeltakelseperiodeDetErSøktOm()!!.tilOgMed

fun Rammebehandlingsstatus.toDatadelingStatus(): Behandlingsstatus =
    when (this) {
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> Behandlingsstatus.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> Behandlingsstatus.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> Behandlingsstatus.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> Behandlingsstatus.UNDER_BESLUTNING
        Rammebehandlingsstatus.VEDTATT -> Behandlingsstatus.VEDTATT
        Rammebehandlingsstatus.AVBRUTT -> Behandlingsstatus.AVBRUTT
    }

fun MeldekortBehandlingStatus.toDatadelingStatus(): Behandlingsstatus =
    when (this) {
        MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> Behandlingsstatus.KLAR_TIL_BEHANDLING
        MeldekortBehandlingStatus.UNDER_BEHANDLING -> Behandlingsstatus.UNDER_BEHANDLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> Behandlingsstatus.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.UNDER_BESLUTNING -> Behandlingsstatus.UNDER_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> Behandlingsstatus.GODKJENT
        MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> Behandlingsstatus.AUTOMATISK_BEHANDLET
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> Behandlingsstatus.IKKE_RETT_TIL_TILTAKSPENGER
        MeldekortBehandlingStatus.AVBRUTT -> Behandlingsstatus.AVBRUTT
    }
