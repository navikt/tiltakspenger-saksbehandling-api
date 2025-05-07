package no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingServiceImpl(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val behandlingService: BehandlingService,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) : AvbrytSøknadOgBehandlingService {
    override suspend fun avbrytSøknadOgBehandling(command: AvbrytSøknadOgBehandlingCommand): Either<KunneIkkeAvbryteSøknadOgBehandling, Sak> {
        val sak =
            sakService.hentForSaksnummer(command.saksnummer, command.avsluttetAv, command.correlationId).getOrElse {
                return KunneIkkeAvbryteSøknadOgBehandling.Feil.left()
            }

        val (oppdatertSak, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            command,
            LocalDateTime.now(),
        )

        val statistikk = avbruttBehandling?.let { statistikkSakService.genererStatistikkForAvsluttetBehandling(it) }

        sessionFactory.withTransactionContext { tx ->
            avbruttSøknad?.let { søknadService.lagreAvbruttSøknad(it, tx) }
            avbruttBehandling?.let { behandlingService.lagreBehandling(it, tx) }
            statistikk?.let { statistikkSakRepo.lagre(it, tx) }
        }.also {
            return oppdatertSak.right()
        }
    }
}
