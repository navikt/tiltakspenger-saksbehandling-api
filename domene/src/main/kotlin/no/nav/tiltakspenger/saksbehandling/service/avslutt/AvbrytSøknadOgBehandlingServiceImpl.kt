package no.nav.tiltakspenger.saksbehandling.service.avslutt

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingServiceImpl(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val behandlingService: BehandlingService,
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

        sessionFactory.withTransactionContext { tx ->
            søknadService.lagreSøknad(avbruttSøknad, tx)
            avbruttBehandling?.let { behandlingService.lagreBehandling(it, tx) }
        }.also {
            return oppdatertSak.right()
        }
    }
}
