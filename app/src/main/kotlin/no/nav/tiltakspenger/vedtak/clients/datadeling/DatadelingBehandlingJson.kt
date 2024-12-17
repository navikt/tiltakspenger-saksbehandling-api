package no.nav.tiltakspenger.vedtak.clients.datadeling

import no.nav.tiltakspenger.libs.datadeling.DatadelingBehandlingDTO
import no.nav.tiltakspenger.libs.datadeling.DatadelingTiltakDTO
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus

fun Behandling.toBehandlingJson(): String {
    return DatadelingBehandlingDTO(
        behandlingId = id.toString(),
        sakId = sakId.toString(),
        saksnummer = saksnummer.verdi,
        fraOgMed = vurderingsperiode.fraOgMed,
        tilOgMed = vurderingsperiode.tilOgMed,
        behandlingStatus = status.toDatadelingDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        tiltak = DatadelingTiltakDTO(
            tiltakNavn = tiltaksnavn,
            eksternTiltakdeltakerId = tiltaksid,
            gjennomføringId = gjennomføringId,
        ),
        fnr = fnr.verdi,
        // Skal kun kalles for førstegangsbehandlinger, men det skal sjekkes lenger ut.
        søknadJournalpostId = søknad!!.journalpostId,
        opprettetTidspunktSaksbehandlingApi = opprettet,

    ).let { serialize(it) }
}

fun Behandlingsstatus.toDatadelingDTO(): DatadelingBehandlingDTO.Behandlingsstatus =
    when (this) {
        Behandlingsstatus.KLAR_TIL_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BEHANDLING
        Behandlingsstatus.UNDER_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BESLUTNING
        Behandlingsstatus.UNDER_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BESLUTNING
        // TODO behandlingsstatus jah: Endre grensesnittet og deretter dette feltet i både saksbehandling-api+datadeling samtidig.
        Behandlingsstatus.VEDTATT -> DatadelingBehandlingDTO.Behandlingsstatus.INNVILGET
    }
