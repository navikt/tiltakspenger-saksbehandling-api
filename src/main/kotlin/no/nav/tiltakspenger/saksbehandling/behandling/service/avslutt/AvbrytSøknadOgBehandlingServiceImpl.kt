package no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingServiceImpl(
    private val sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    private val søknadService: no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService,
    private val behandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService,
    private val sessionFactory: SessionFactory,
) : no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService {
    override suspend fun avbrytSøknadOgBehandling(command: no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.KunneIkkeAvbryteSøknadOgBehandling, Sak> {
        val sak =
            sakService.hentForSaksnummer(command.saksnummer, command.avsluttetAv, command.correlationId).getOrElse {
                return no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.KunneIkkeAvbryteSøknadOgBehandling.Feil.left()
            }

        val (oppdatertSak, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            command,
            LocalDateTime.now(),
        )

        sessionFactory.withTransactionContext { tx ->
            avbruttSøknad?.let { søknadService.lagreAvbruttSøknad(it, tx) }
            avbruttBehandling?.let { behandlingService.lagreBehandling(it, tx) }
        }.also {
            return oppdatertSak.right()
        }
    }
}
