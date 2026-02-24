package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
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
            hendelse = "opprettet_behandling",
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
            hendelse = "opprettet_revurdering",
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
            hendelse = "sendt_til_beslutter",
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
            hendelse = "underkjent_behandling",
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
            hendelse = "oppdatert_saksbehandler_beslutter",
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
            hendelse = "avsluttet_behandling",
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
            hendelse = "behandling_satt_på_vent",
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
            hendelse = "behandling_gjenopptatt",
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
            hendelse = "søknad_behandlet_på_nytt",
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
            hendelse = "opprettet_klagebehandling",
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
            hendelse = "avsluttet_klagebehandling",
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
            hendelse = "klagebehandling_satt_på_vent",
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
            hendelse = "klagebehandling_gjenopptatt",
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
            hendelse = "klagebehandling_iverksatt",
        )
    }

    private suspend fun gjelderKode6(fnr: Fnr): Boolean {
        val person = personKlient.hentEnkelPerson(fnr)
        return person.strengtFortrolig || person.strengtFortroligUtland
    }
}
