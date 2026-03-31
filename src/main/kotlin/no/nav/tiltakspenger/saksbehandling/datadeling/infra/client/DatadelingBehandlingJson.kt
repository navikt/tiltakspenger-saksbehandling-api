package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingBehandlingJson.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingBehandlingJson.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
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
            vedtaksperiode?.fraOgMed
        },
        tilOgMed = if (this is Søknadsbehandling) {
            this.getTilOgMed()
        } else {
            vedtaksperiode?.tilOgMed
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

fun Meldekortbehandling.toBehandlingJson(): String {
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

private fun Søknadsbehandling.getFraOgMed() = vedtaksperiode?.fraOgMed
    ?: saksopplysninger.tiltaksdeltakelser.tidligsteFraOgMed
    ?: søknad.tiltak?.deltakelseFom
    ?: søknad.tiltaksdeltakelseperiodeDetErSøktOm()!!.fraOgMed

private fun Søknadsbehandling.getTilOgMed() = vedtaksperiode?.tilOgMed
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

fun MeldekortbehandlingStatus.toDatadelingStatus(): Behandlingsstatus =
    when (this) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> Behandlingsstatus.KLAR_TIL_BEHANDLING
        MeldekortbehandlingStatus.UNDER_BEHANDLING -> Behandlingsstatus.UNDER_BEHANDLING
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> Behandlingsstatus.KLAR_TIL_BESLUTNING
        MeldekortbehandlingStatus.UNDER_BESLUTNING -> Behandlingsstatus.UNDER_BESLUTNING
        MeldekortbehandlingStatus.GODKJENT -> Behandlingsstatus.GODKJENT
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> Behandlingsstatus.AUTOMATISK_BEHANDLET
        MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> Behandlingsstatus.IKKE_RETT_TIL_TILTAKSPENGER
        MeldekortbehandlingStatus.AVBRUTT -> Behandlingsstatus.AVBRUTT
    }
