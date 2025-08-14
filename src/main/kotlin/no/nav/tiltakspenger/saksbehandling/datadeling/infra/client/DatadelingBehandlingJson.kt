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
        // TODO jah: Gir det mening at [fraOgMed] og [tilOgMed] er noe annet enn null fram til virkningsperioden er satt?
        fraOgMed = virkningsperiode?.fraOgMed ?: saksopplysninger.tiltaksdeltagelser.tidligsteFraOgMed
            ?: søknad.tiltak.deltakelseFom,
        tilOgMed = virkningsperiode?.tilOgMed ?: saksopplysninger.tiltaksdeltagelser.senesteTilOgMed
            ?: søknad.tiltak.deltakelseTom,
        behandlingStatus = status.toDatadelingDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        fnr = fnr.verdi,
        // Skal kun kalles for søknadsbehandlinger, men det skal sjekkes lenger ut.
        søknadJournalpostId = søknad.journalpostId,
        opprettetTidspunktSaksbehandlingApi = opprettet,

    ).let { serialize(it) }
}

fun Behandlingsstatus.toDatadelingDTO(): DatadelingBehandlingDTO.Behandlingsstatus =
    when (this) {
        Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BEHANDLING
        Behandlingsstatus.UNDER_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BESLUTNING
        Behandlingsstatus.UNDER_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BESLUTNING
        Behandlingsstatus.VEDTATT -> DatadelingBehandlingDTO.Behandlingsstatus.VEDTATT
        Behandlingsstatus.AVBRUTT -> DatadelingBehandlingDTO.Behandlingsstatus.AVBRUTT
    }
