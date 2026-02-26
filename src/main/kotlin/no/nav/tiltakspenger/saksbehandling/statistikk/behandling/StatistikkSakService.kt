package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock

class StatistikkSakService(
    private val personKlient: PersonKlient,
    private val gitHash: String,
    private val clock: Clock,
) {
    suspend fun genererStatistikkForSøknadsbehandling(
        behandling: Søknadsbehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.OPPRETTET_BEHANDLING,
        )
    }

    suspend fun genererStatistikkForRevurdering(
        behandling: Revurdering,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.OPPRETTET_REVURDERING,
        )
    }

    suspend fun genererStatistikkForRammevedtak(
        rammevedtak: Rammevedtak,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForRammevedtak(
            vedtak = rammevedtak,
            gjelderKode6 = gjelderKode6(rammevedtak.fnr),
            versjon = gitHash,
            clock = clock,
        )
    }

    suspend fun genererStatistikkForSendTilBeslutter(
        behandling: Rammebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.SENDT_TIL_BESLUTTER,
        )
    }

    suspend fun genererStatistikkForUnderkjennBehandling(
        behandling: Rammebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.UNDERKJENT_BEHANDLING,
        )
    }

    suspend fun genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(
        behandling: Rammebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.OPPDATERT_SAKSBEHANDLER_BESLUTTER,
        )
    }

    suspend fun genererStatistikkForAvsluttetBehandling(
        behandling: Rammebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.AVSLUTTET_BEHANDLING,
        )
    }

    suspend fun genererStatistikkForBehandlingSattPåVent(
        behandling: Rammebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.BEHANDLING_SATT_PA_VENT,
        )
    }

    suspend fun genererStatistikkForGjenopptattBehandling(
        behandling: Rammebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.BEHANDLING_GJENOPPTATT,
        )
    }

    suspend fun genererStatistikkForSøknadSomBehandlesPåNytt(
        behandling: Søknadsbehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.SOKNAD_BEHANDLET_PA_NYTT,
        )
    }

    suspend fun genererSaksstatistikkForOpprettetKlagebehandling(
        behandling: Klagebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForKlagebehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.OPPRETTET_BEHANDLING,
        )
    }

    suspend fun genererSaksstatistikkForAvsluttetKlagebehandling(
        behandling: Klagebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForKlagebehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.AVSLUTTET_BEHANDLING,
        )
    }

    suspend fun genererSaksstatistikkForKlagebehandlingSattPåVent(
        behandling: Klagebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForKlagebehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.BEHANDLING_SATT_PA_VENT,
        )
    }

    suspend fun genererSaksstatistikkForGjenopptattKlagebehandling(
        behandling: Klagebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForKlagebehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.BEHANDLING_GJENOPPTATT,
        )
    }

    suspend fun genererSaksstatistikkForKlagebehandlingOversendtTilKabal(
        behandling: Klagebehandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForKlagebehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
            hendelse = StatistikkHendelse.OVERSENDT_KA,
        )
    }

    suspend fun genererSaksstatistikkIverksattAvvistKlagebehandling(
        behandling: Klagevedtak,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForKlagevedtak(
            vedtak = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr),
            versjon = gitHash,
            clock = clock,
        )
    }

    private suspend fun gjelderKode6(fnr: Fnr): Boolean {
        val person = personKlient.hentEnkelPerson(fnr)
        return person.strengtFortrolig || person.strengtFortroligUtland
    }
}
