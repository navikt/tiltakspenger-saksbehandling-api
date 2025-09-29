package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.datadeling.DatadelingBehandlingDTO
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling

fun Søknadsbehandling.toBehandlingJson(): String {
    return DatadelingBehandlingDTO(
        behandlingId = id.toString(),
        sakId = sakId.toString(),
        saksnummer = saksnummer.verdi,
        // TODO jah: Gir det mening at [fraOgMed] og [tilOgMed] er noe annet enn null fram til virkningsperioden er satt?
        fraOgMed = virkningsperiode?.fraOgMed
            ?: saksopplysninger?.tiltaksdeltagelser?.tidligsteFraOgMed
            ?: søknad.tiltak?.deltakelseFom
            ?: søknad.tiltaksdeltagelseperiodeDetErSøktOm().fraOgMed,
        tilOgMed = virkningsperiode?.tilOgMed
            ?: saksopplysninger?.tiltaksdeltagelser?.senesteTilOgMed
            ?: søknad.tiltak?.deltakelseTom
            ?: søknad.tiltaksdeltagelseperiodeDetErSøktOm().tilOgMed,
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

fun Rammebehandlingsstatus.toDatadelingDTO(): DatadelingBehandlingDTO.Behandlingsstatus =
    when (this) {
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BESLUTNING
        Rammebehandlingsstatus.VEDTATT -> DatadelingBehandlingDTO.Behandlingsstatus.VEDTATT
        Rammebehandlingsstatus.AVBRUTT -> DatadelingBehandlingDTO.Behandlingsstatus.AVBRUTT
    }
