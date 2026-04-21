package no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingService(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
    private val rammebehandlingRepo: RammebehandlingRepo,
) {

    suspend fun avbrytSøknadOgBehandling(command: AvbrytRammebehandlingKommando): Sak {
        val sak = sakService.hentForSaksnummer(command.saksnummer)
        val avbruttTidspunkt = LocalDateTime.now(clock)
        val (oppdatertSak, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            command = command,
            avbruttTidspunkt = avbruttTidspunkt,
        )
        val statistikkhendelser = Statistikkhendelser(
            avbruttBehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING),
        )
        val statistikkDto = statistikkService.generer(statistikkhendelser)
        sessionFactory.withTransactionContext { tx ->
            avbruttSøknad?.also { søknadService.lagreAvbruttSøknad(it, tx) }
            rammebehandlingRepo.lagre(avbruttBehandling, tx)
            statistikkService.lagre(statistikkDto, tx)
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
    val behandlingId: RammebehandlingId,
    val avsluttetAv: Saksbehandler,
    val correlationId: CorrelationId,
    val begrunnelse: NonBlankString,
)
