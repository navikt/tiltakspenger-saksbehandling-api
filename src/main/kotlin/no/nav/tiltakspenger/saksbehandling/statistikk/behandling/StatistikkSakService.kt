package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock

class StatistikkSakService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val gitHash: String,
    private val clock: Clock,
) {
    suspend fun genererStatistikkForSøknadsbehandling(
        behandling: Søknadsbehandling,
        søknadId: SøknadId,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr, "SøknadId: $søknadId"),
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
            gjelderKode6 = gjelderKode6(behandling.fnr, "BehandlingId: ${behandling.id}"),
            versjon = gitHash,
            clock = clock,
            hendelse = "opprettet_revurdering",
        )
    }

    suspend fun genererStatistikkForRammevedtak(
        rammevedtak: Rammevedtak,
        behandlingId: BehandlingId,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForRammevedtak(
            vedtak = rammevedtak,
            gjelderKode6 = gjelderKode6(rammevedtak.fnr, "BehandlingId: $behandlingId"),
            versjon = gitHash,
            clock = clock,
        )
    }

    suspend fun genererStatistikkForSendTilBeslutter(
        behandling: Behandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr, "BehandlingId: ${behandling.id}"),
            versjon = gitHash,
            clock = clock,
            hendelse = "sendt_til_beslutter",
        )
    }

    suspend fun genererStatistikkForUnderkjennBehandling(
        behandling: Behandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr, "BehandlingId: ${behandling.id}"),
            versjon = gitHash,
            clock = clock,
            hendelse = "underkjent_behandling",
        )
    }

    suspend fun genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(
        behandling: Behandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr, "BehandlingId: ${behandling.id}"),
            versjon = gitHash,
            clock = clock,
            hendelse = "oppdatert_saksbehandler_beslutter",
        )
    }

    suspend fun genererStatistikkForAvsluttetBehandling(
        behandling: Behandling,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr, "BehandlingId: ${behandling.id}"),
            versjon = gitHash,
            clock = clock,
            hendelse = "avsluttet_behandling",
        )
    }

    suspend fun genererStatistikkForSøknadSomBehandlesPåNytt(
        behandling: Søknadsbehandling,
        søknadId: SøknadId,
    ): StatistikkSakDTO {
        return genererSaksstatistikkForBehandling(
            behandling = behandling,
            gjelderKode6 = gjelderKode6(behandling.fnr, "SøknadId: $søknadId"),
            versjon = gitHash,
            clock = clock,
            hendelse = "søknad_behandlet_på_nytt",
        )
    }

    private suspend fun gjelderKode6(fnr: Fnr, sporingsinformasjon: String): Boolean {
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr)
                .getOrElse {
                    throw IllegalArgumentException(
                        "Kunne ikke hente adressebeskyttelsegradering for person. $sporingsinformasjon",
                    )
                }
        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. $sporingsinformasjon" }

        return adressebeskyttelseGradering.harStrengtFortroligAdresse()
    }
}
