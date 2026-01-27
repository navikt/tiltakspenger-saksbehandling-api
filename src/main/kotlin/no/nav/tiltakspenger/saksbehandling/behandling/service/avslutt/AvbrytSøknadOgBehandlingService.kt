package no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingService(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val behandlingService: BehandlingService,
    private val statistikkSakService: StatistikkSakService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
    private val klagebehandlingRepo: KlagebehandlingRepo,
) {

    suspend fun avbrytSøknadOgBehandling(command: AvbrytRammebehandlingKommando): Sak {
        val sak = sakService.hentForSaksnummer(command.saksnummer)
        val (oppdatertSak, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            command = command,
            avbruttTidspunkt = LocalDateTime.now(clock),
        )

        val behandlingOgStatistikk: Pair<Rammebehandling, StatistikkSakDTO> = avbruttBehandling.let {
            it to statistikkSakService.genererStatistikkForAvsluttetBehandling(it)
        }
        // Dersom denne rammebehandlingen er opprettet fra en klagebehandling til omgjøring, vil vi ha denne knytningen.
        // Saksbehandler må få lov til å angre seg og avbryte rammebehandlingen, men kun når rammebehandlingen er klar til/under behandling.
        val klagebehandling = klagebehandlingRepo
            .hentForRammebehandlingId(avbruttBehandling.id)
            ?.fjernRammebehandlingId(command.avsluttetAv, avbruttBehandling.id)

        sessionFactory.withTransactionContext { tx ->
            avbruttSøknad?.also {
                søknadService.lagreAvbruttSøknad(it, tx)
            }
            behandlingOgStatistikk.also {
                behandlingService.lagreMedStatistikk(it.first, it.second, tx)
            }
            if (klagebehandling != null) {
                klagebehandlingRepo.lagreKlagebehandling(klagebehandling)
            }
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sak.id,
                skalSendesTilMeldekortApi = true,
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
