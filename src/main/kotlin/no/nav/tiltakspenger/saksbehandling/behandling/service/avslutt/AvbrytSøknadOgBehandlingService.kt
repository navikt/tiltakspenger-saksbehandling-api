package no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.LocalDateTime

class AvbrytSøknadOgBehandlingService(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val behandlingService: BehandlingService,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {

    suspend fun avbrytSøknadOgBehandling(command: AvbrytSøknadOgBehandlingCommand): Either<KunneIkkeAvbryteSøknadOgBehandling, Sak> {
        // Validerer at saksbehandler har tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
        val sak = sakService.hentForSaksnummer(
            saksnummer = command.saksnummer,
            saksbehandler = command.avsluttetAv,
            correlationId = command.correlationId,
        )
        // Validerer saksbehandler
        val (oppdatertSak, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            command = command,
            avbruttTidspunkt = LocalDateTime.now(),
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
sealed interface KunneIkkeAvbryteSøknadOgBehandling {
    data object Feil : KunneIkkeAvbryteSøknadOgBehandling
}

data class AvbrytSøknadOgBehandlingCommand(
    val saksnummer: Saksnummer,
    val søknadId: SøknadId?,
    val behandlingId: BehandlingId?,
    val avsluttetAv: Saksbehandler,
    val correlationId: CorrelationId,
    val begrunnelse: String,
) {
    init {
        require(søknadId != null || behandlingId != null) { "Enten søknadId eller behandlingId må være satt" }
    }
}
