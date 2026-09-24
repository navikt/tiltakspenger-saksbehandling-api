package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne.GjenåpneSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne.KanIkkeGjenåpneSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne.gjenåpneSøknad
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.startSøknadsbehandlingPåNytt
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class GjenåpneSøknadsbehandlingService(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    suspend fun gjenåpneSøknadsbehandling(
        kommando: GjenåpneSøknadsbehandlingKommando,
    ): Either<KanIkkeGjenåpneSøknadsbehandling, Pair<Sak, Søknadsbehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        val (sakMedGjenåpnetSøknad, gjenåpnetSøknad) = sak.gjenåpneSøknad(
            kommando = kommando,
            tidspunkt = nå(clock),
        ).getOrElse { return it.left() }

        val søknadId =
            (sakMedGjenåpnetSøknad.hentRammebehandling(kommando.avbruttBehandlingId) as Søknadsbehandling).søknad.id

        val (oppdatertSak, nySøknadsbehandling, statistikkhendelser) = sakMedGjenåpnetSøknad.startSøknadsbehandlingPåNytt(
            kommando = StartSøknadsbehandlingPåNyttKommando(
                søknadId = søknadId,
                sakId = sak.id,
                saksbehandler = kommando.saksbehandler,
                klagebehandlingId = null,
                correlationId = kommando.correlationId,
            ),
            clock = clock,
            hentSaksopplysninger = { fnr, correlationId, tiltaksdeltakelserDetErSøktTiltakspengerFor, aktuelleTiltaksdeltakelserForBehandlingen, inkluderOverlappendeTiltaksdeltakelserDetErSøktOm, sakId ->
                hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                    fnr = fnr,
                    correlationId = correlationId,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
                    sakId = sakId,
                    saksnummer = sak.saksnummer,
                )
            },
        )
        val statistikkDto = statistikkService.generer(statistikkhendelser)

        sessionFactory.withTransactionContext { tx ->
            gjenåpnetSøknad?.also { søknadService.lagreGjenåpnetSøknad(it, tx) }
            rammebehandlingRepo.lagre(nySøknadsbehandling, tx)
            statistikkService.lagre(statistikkDto, tx)
            sakService.markerSkalSendesTilMeldekortApi(
                sakId = sak.id,
                sessionContext = tx,
            )
        }
        return (oppdatertSak to nySøknadsbehandling).right()
    }
}
