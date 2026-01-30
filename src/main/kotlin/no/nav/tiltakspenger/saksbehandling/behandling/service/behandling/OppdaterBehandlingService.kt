package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import java.time.Clock

class OppdaterBehandlingService(
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val navkontorService: NavkontorService,
    private val clock: Clock,
    private val simulerService: SimulerService,
    private val sessionFactory: SessionFactory,
) {
    private val log = KotlinLogging.logger {}

    suspend fun oppdater(kommando: OppdaterBehandlingKommando): Either<KanIkkeOppdatereBehandling, Pair<Sak, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val behandling: Rammebehandling = sak.hentRammebehandling(kommando.behandlingId)!!

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return BehandlingenEiesAvAnnenSaksbehandler(behandling.saksbehandler).left()
        }
        if (behandling.ventestatus.erSattPåVent) {
            return KanIkkeOppdatereBehandling.ErPaVent.left()
        }

        sak.rammevedtaksliste.validerOpphør(kommando).onLeft {
            log.error { "Ugyldig opphør ved forsøk på oppdatering av behandling - $kommando" }
            return it.left()
        }

        val (utbetaling, simuleringMedMetadata) = sak.beregnOgSimulerHvisAktuelt(kommando, behandling)

        return when (kommando) {
            is OppdaterSøknadsbehandlingKommando -> sak.oppdaterSøknadsbehandling(kommando, utbetaling)
            is OppdaterRevurderingKommando -> sak.oppdaterRevurdering(kommando, utbetaling)
        }.map { oppdatertBehandling: Rammebehandling ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertBehandling)

            log.debug { "Lagrer oppdatert behandling ${behandling.id} for sak ${behandling.sakId}" }
            sessionFactory.withTransactionContext { tx ->
                rammebehandlingRepo.lagre(oppdatertBehandling, tx)
                rammebehandlingRepo.oppdaterSimuleringMetadata(
                    oppdatertBehandling.id,
                    simuleringMedMetadata?.originalResponseBody,
                    tx,
                )
            }
            oppdatertSak to oppdatertBehandling
        }
    }

    suspend fun Sak.beregnOgSimulerHvisAktuelt(
        kommando: OppdaterBehandlingKommando,
        behandling: Rammebehandling,
    ): Pair<BehandlingUtbetaling?, SimuleringMedMetadata?> {
        log.debug { "Beregner hvis aktuelt ifm oppdatering av behandling ${behandling.id} for sak ${behandling.sakId}" }
        val beregning = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Innvilgelse,
            is OppdaterRevurderingKommando.Innvilgelse,
            -> this.beregnInnvilgelse(
                behandlingId = kommando.behandlingId,
                vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
                innvilgelsesperioder = kommando.tilInnvilgelseperioder(behandling),
                barnetilleggsperioder = kommando.barnetillegg.periodisering,
            )

            is OppdaterRevurderingKommando.Omgjøring,
            -> {
                val innvilgelsesperioder = kommando.tilInnvilgelseperioder(behandling)

                this.beregnInnvilgelse(
                    behandlingId = kommando.behandlingId,
                    vedtaksperiode = Revurderingsresultat.Omgjøring.utledNyVedtaksperiode(
                        (behandling.resultat as Revurderingsresultat.Omgjøring).vedtaksperiode,
                        innvilgelsesperioder.totalPeriode,
                    ),
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetilleggsperioder = kommando.barnetillegg.periodisering,
                )
            }

            is OppdaterRevurderingKommando.Stans -> this.beregnRevurderingStans(
                behandlingId = kommando.behandlingId,
                // Vi kan ikke stanse hvis vi ikke har en rettighetsperiode.
                stansperiode = kommando.utledStansperiode(this.førsteDagSomGirRett!!, this.sisteDagSomGirRett!!),
            )

            is OppdaterSøknadsbehandlingKommando.Avslag,
            is OppdaterSøknadsbehandlingKommando.IkkeValgtResultat,
            -> null
        }

        return beregning?.let {
            val navkontor = navkontorService.hentOppfolgingsenhet(this.fnr)
            val simuleringMedMetadata = simulerService.simulerSøknadsbehandlingEllerRevurdering(
                // Merk at behandlingen vi sender inn her er som den kom fra basen. Kanskje vi heller bare skal sende inn od, sakId, fnr og saksnummer?
                behandling = behandling,
                beregning = it,
                forrigeUtbetaling = this.utbetalinger.lastOrNull(),
                meldeperiodeKjeder = this.meldeperiodeKjeder,
                saksbehandler = kommando.saksbehandler.navIdent,
                kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
            ) { navkontor }.getOrElse { null }

            BehandlingUtbetaling(
                beregning = it,
                navkontor = navkontor,
                simulering = simuleringMedMetadata?.simulering,
            ) to simuleringMedMetadata
        } ?: (null to null)
    }

    private fun Sak.oppdaterSøknadsbehandling(
        kommando: OppdaterSøknadsbehandlingKommando,
        utbetaling: BehandlingUtbetaling?,
    ): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
        val søknadsbehandling: Søknadsbehandling = this.hentRammebehandling(kommando.behandlingId) as Søknadsbehandling

        val omgjørRammevedtak = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Avslag,
            is OppdaterSøknadsbehandlingKommando.IkkeValgtResultat,
            -> OmgjørRammevedtak.empty

            is OppdaterSøknadsbehandlingKommando.Innvilgelse -> this.vedtaksliste.finnRammevedtakSomOmgjøres(
                vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            )
        }

        return søknadsbehandling.oppdater(
            kommando = kommando,
            clock = clock,
            utbetaling = utbetaling,
            omgjørRammevedtak = omgjørRammevedtak,
        )
    }

    private fun Sak.oppdaterRevurdering(
        kommando: OppdaterRevurderingKommando,
        utbetaling: BehandlingUtbetaling?,
    ): Either<KanIkkeOppdatereBehandling, Revurdering> {
        val revurdering: Revurdering = this.hentRammebehandling(kommando.behandlingId) as Revurdering

        return when (kommando) {
            is OppdaterRevurderingKommando.Omgjøring -> {
                val nyVedtaksperiode = Revurderingsresultat.Omgjøring.utledNyVedtaksperiode(
                    revurdering.omgjørRammevedtak.totalPeriode!!,
                    kommando.innvilgelsesperioder.totalPeriode,
                )

                val rammevedtakSomOmgjøres = this.vedtaksliste.finnRammevedtakSomOmgjøres(nyVedtaksperiode)

                if (rammevedtakSomOmgjøres.rammevedtakIDer.size > 1) {
                    return KanIkkeOppdatereBehandling.KanIkkeOmgjøreFlereVedtak.left()
                }

                revurdering.oppdaterOmgjøring(
                    kommando = kommando,
                    utbetaling = utbetaling,
                    clock = clock,
                    omgjørRammevedtak = this.vedtaksliste.finnRammevedtakSomOmgjøres(nyVedtaksperiode),
                )
            }

            is OppdaterRevurderingKommando.Innvilgelse -> {
                revurdering.oppdaterInnvilgelse(
                    kommando = kommando,
                    utbetaling = utbetaling,
                    clock = clock,
                    omgjørRammevedtak = this.vedtaksliste.finnRammevedtakSomOmgjøres(kommando.innvilgelsesperioder.totalPeriode),
                )
            }

            is OppdaterRevurderingKommando.Stans -> {
                val stansperiode = kommando.utledStansperiode(førsteDagSomGirRett!!, sisteDagSomGirRett!!)
                val overlappendeperiode =
                    this.vedtaksliste.rammevedtaksliste.innvilgetTidslinje.overlappendePeriode(stansperiode)
                if (overlappendeperiode.isEmpty()) {
                    throw IllegalStateException("Stansperioden $stansperiode må inneholde eksisterende innvilgede perioder på saken. Finnes ingen innvilgelser i ${this.vedtaksliste.rammevedtaksliste.innvilgelsesperioder} for sakId=$id")
                }
                revurdering.oppdaterStans(
                    kommando = kommando,
                    førsteDagSomGirRett = førsteDagSomGirRett,
                    sisteDagSomGirRett = sisteDagSomGirRett,
                    clock = clock,
                    utbetaling = utbetaling,
                    omgjørRammevedtak = this.vedtaksliste.finnRammevedtakSomOmgjøres(stansperiode),
                )
            }
        }
    }

    /**
     *  Validerer at oppdateringen ikke fører til et opphør for resultat-typer der opphør ikke støttes
     * */
    private fun Rammevedtaksliste.validerOpphør(kommando: OppdaterBehandlingKommando): Either<KanIkkeOppdatereBehandling, Unit> {
        return when (kommando) {
            is OppdaterSøknadsbehandlingKommando.IkkeValgtResultat,
            is OppdaterSøknadsbehandlingKommando.Avslag,
            is OppdaterRevurderingKommando.Stans,
            is OppdaterRevurderingKommando.Omgjøring,
            -> Unit.right()

            is OppdaterRevurderingKommando.Innvilgelse,
            is OppdaterSøknadsbehandlingKommando.Innvilgelse,
            -> {
                val eksisterendeInnvilgetPerioder =
                    innvilgetTidslinje.krymp(kommando.innvilgelsesperioder.totalPeriode).perioder

                val perioderSomOpphøres = eksisterendeInnvilgetPerioder.trekkFra(kommando.innvilgelsesperioder.perioder)

                if (perioderSomOpphøres.isEmpty()) Unit.right() else KanIkkeOppdatereBehandling.KanIkkeOpphøre.left()
            }
        }
    }
}
