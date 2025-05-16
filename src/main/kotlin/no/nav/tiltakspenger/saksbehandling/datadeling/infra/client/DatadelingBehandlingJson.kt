package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.datadeling.DatadelingBehandlingDTO
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling

fun Søknadsbehandling.toBehandlingJson(): String {
    return DatadelingBehandlingDTO(
        behandlingId = id.toString(),
        sakId = sakId.toString(),
        saksnummer = saksnummer.verdi,
        fraOgMed = virkningsperiode?.fraOgMed ?: saksopplysningsperiode.fraOgMed,
        tilOgMed = virkningsperiode?.tilOgMed ?: saksopplysningsperiode.tilOgMed,
        behandlingStatus = status.toDatadelingDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        fnr = fnr.verdi,
        // Skal kun kalles for førstegangsbehandlinger, men det skal sjekkes lenger ut.
        søknadJournalpostId = søknad.journalpostId,
        opprettetTidspunktSaksbehandlingApi = opprettet,

    ).let { serialize(it) }
}

fun Behandlingsstatus.toDatadelingDTO(): DatadelingBehandlingDTO.Behandlingsstatus =
    when (this) {
        Behandlingsstatus.KLAR_TIL_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BEHANDLING
        Behandlingsstatus.UNDER_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BESLUTNING
        Behandlingsstatus.UNDER_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BESLUTNING
        Behandlingsstatus.VEDTATT -> DatadelingBehandlingDTO.Behandlingsstatus.VEDTATT
        Behandlingsstatus.AVBRUTT -> DatadelingBehandlingDTO.Behandlingsstatus.AVBRUTT
    }
