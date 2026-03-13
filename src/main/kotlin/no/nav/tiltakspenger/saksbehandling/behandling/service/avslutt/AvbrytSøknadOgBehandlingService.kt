package no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkSakDTO
import java.time.Clock
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingService(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val behandlingService: RammebehandlingService,
    private val saksstatistikkService: SaksstatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {

    suspend fun avbrytSøknadOgBehandling(command: AvbrytRammebehandlingKommando): Sak {
        val sak = sakService.hentForSaksnummer(command.saksnummer)
        val avbruttTidspunkt = LocalDateTime.now(clock)
        val (oppdatertSak, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            command = command,
            avbruttTidspunkt = avbruttTidspunkt,
        )

        val behandlingOgStatistikk: Pair<Rammebehandling, StatistikkSakDTO> = avbruttBehandling.let {
            it to saksstatistikkService.genererStatistikkForAvsluttetBehandling(it)
        }

        sessionFactory.withTransactionContext { tx ->
            avbruttSøknad?.also {
                søknadService.lagreAvbruttSøknad(it, tx)
            }
            behandlingOgStatistikk.also {
                behandlingService.lagreMedStatistikk(
                    behandling = it.first,
                    statistikk = it.second,
                    // Potensielt tilhørende klage blir ikke avbrutt av dette, så vi genererer ikke statistikk
                    klageStatistikk = null,
                    tx = tx,
                )
            }
            sakService.markerSkalSendesTilMeldekortApi(
                sakId = sak.id,
                sessionContext = tx,
            )
        }
        return oppdatertSak
    }
}

sealed interface KunneIkkeAvbryteSøknadOgBehandling {
    data object Feil : KunneIkkeAvbryteSøknadOgBehandling
}

/**
 * Avbryter kun tilhørende søknad dersom dette er den første søknadsbehandlingen som vurderer den søknaden.
 */
data class AvbrytRammebehandlingKommando(
    val saksnummer: Saksnummer,
    val behandlingId: BehandlingId,
    val avsluttetAv: Saksbehandler,
    val correlationId: CorrelationId,
    val begrunnelse: NonBlankString,
)
