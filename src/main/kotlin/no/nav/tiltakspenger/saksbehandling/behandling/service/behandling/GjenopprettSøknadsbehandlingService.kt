package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett.GjenopprettSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett.KanIkkeGjenoppretteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett.gjenopprettSøknad
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.startSøknadsbehandlingPåNytt
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

/**
 * Gjenoppretter en søknad som ble avsluttet uten vedtak, og oppretter en ny søknadsbehandling på den.
 *
 * Saksbehandler peker på den avbrutte søknadsbehandlingen, men det er søknaden som tas opp igjen - den avbrutte behandlingen står urørt, og erstattes av en ny.
 */
class GjenopprettSøknadsbehandlingService(
    private val sakService: SakService,
    private val søknadService: SøknadService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    suspend fun gjenopprettSøknadsbehandling(
        kommando: GjenopprettSøknadsbehandlingKommando,
    ): Either<KanIkkeGjenoppretteSøknadsbehandling, Pair<Sak, Søknadsbehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        val (sakMedGjenopprettetSøknad, gjenopprettetSøknad) = sak.gjenopprettSøknad(
            kommando = kommando,
            tidspunkt = nå(clock),
        ).getOrElse { return it.left() }

        val søknadId = (sakMedGjenopprettetSøknad.hentRammebehandling(kommando.avbruttBehandlingId) as Søknadsbehandling).søknad.id

        val (oppdatertSak, nySøknadsbehandling, statistikkhendelser) = sakMedGjenopprettetSøknad.startSøknadsbehandlingPåNytt(
            kommando = StartSøknadsbehandlingPåNyttKommando(
                søknadId = søknadId,
                sakId = sak.id,
                saksbehandler = kommando.saksbehandler,
                klagebehandlingId = null,
                correlationId = kommando.correlationId,
            ),
            clock = clock,
            hentSaksopplysninger = { fnr, correlationId, tiltaksdeltakelserDetErSøktTiltakspengerFor, aktuelleTiltaksdeltakelserForBehandlingen, inkluderOverlappendeTiltaksdeltakelserDetErSøktOm ->
                hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                    fnr = fnr,
                    correlationId = correlationId,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                )
            },
        )
        val statistikkDto = statistikkService.generer(statistikkhendelser)

        sessionFactory.withTransactionContext { tx ->
            gjenopprettetSøknad?.also { søknadService.lagreGjenopprettetSøknad(it, tx) }
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
