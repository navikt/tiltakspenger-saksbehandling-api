package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.SOKNAD_BEHANDLES_MANUELT_GRUNN
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.SOKNAD_BEHANDLET_DELVIS_AUTOMATISK
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.SOKNAD_IKKE_BEHANDLET_AUTOMATISK
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import java.time.LocalDate

class DelautomatiskBehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
    private val navkontorService: NavkontorService,
    private val simulerService: SimulerService,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun behandleAutomatisk(behandling: Søknadsbehandling, correlationId: CorrelationId) {
        if (skalSettePaVent(behandling, correlationId)) {
            settBehandlingPaVent(behandling, correlationId)
            return
        }
        val oppdatertBehandling = if (behandling.ventestatus.erSattPåVent) {
            log.info { "Gjenopptar behandling med id ${behandling.id}. CorrelationId: $correlationId" }
            val gjenopptattBehandling = behandling.gjenoppta(
                endretAv = AUTOMATISK_SAKSBEHANDLER,
                // Den automatiske jobben oppdaterer saksopplysningene selv.
                hentSaksopplysninger = null,
                clock = clock,
            ).getOrNull()!!
            behandlingRepo.lagre(gjenopptattBehandling)
            gjenopptattBehandling as Søknadsbehandling
        } else {
            behandling
        }

        val manueltBehandlesGrunner = kanBehandleAutomatisk(oppdatertBehandling)

        if (manueltBehandlesGrunner.isEmpty()) {
            log.info { "Kan behandle behandling med id ${behandling.id} automatisk, sender til beslutning, correlationId $correlationId" }
            val sak = sakService.hentForSakId(behandling.sakId)
            sak.sendTilBeslutning(oppdatertBehandling, correlationId)
            SOKNAD_BEHANDLET_DELVIS_AUTOMATISK.inc()
        } else {
            log.info { "Kan ikke behandle behandling med id ${behandling.id} automatisk, sender til manuell behandling, correlationId $correlationId" }
            sendTilManuellBehandling(oppdatertBehandling, manueltBehandlesGrunner, correlationId)
            SOKNAD_IKKE_BEHANDLET_AUTOMATISK.inc()
        }
    }

    private fun skalSettePaVent(behandling: Søknadsbehandling, correlationId: CorrelationId): Boolean {
        val soknadstiltakFraSaksopplysning = getSoknadstiltakFraSaksopplysning(behandling) ?: return false

        if (!soknadstiltakFraSaksopplysning.deltakelseStatus.harIkkeStartet() || soknadstiltakFraSaksopplysning.deltakelseFraOgMed == null) {
            return false
        }
        if (soknadstiltakFraSaksopplysning.deltakelseFraOgMed.isAfter(LocalDate.now(clock))) {
            log.info { "Startdato for deltakelse for behandling ${behandling.id} er ikke passert, setter på vent. CorrelationId: $correlationId" }
            return true
        }
        return false
    }

    private fun settBehandlingPaVent(behandling: Søknadsbehandling, correlationId: CorrelationId) {
        val startdatoForTiltak = getSoknadstiltakFraSaksopplysning(behandling)?.deltakelseFraOgMed
            ?: throw IllegalStateException("Skal ikke sette behandling med id ${behandling.id} på vent siden startdato mangler")
        val venterTil = startdatoForTiltak.atTime(6, 0)
        if (behandling.ventestatus.erSattPåVent) {
            behandling.oppdaterVenterTil(
                nyVenterTil = venterTil,
                clock = clock,
            ).let {
                behandlingRepo.lagre(it)
            }
            log.info { "Har oppdatert venterTil for behandling med id ${behandling.id} som allerede var på vent. CorrelationId: $correlationId" }
        } else {
            behandling.settPåVent(
                endretAv = AUTOMATISK_SAKSBEHANDLER,
                begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                clock = clock,
                venterTil = venterTil,
            ).let {
                behandlingRepo.lagre(it)
            }
            log.info { "Har satt behandling med id ${behandling.id} på vent. CorrelationId: $correlationId" }
        }
    }

    private suspend fun Sak.sendTilBeslutning(behandling: Søknadsbehandling, correlationId: CorrelationId) {
        require(behandling.søknad is InnvilgbarSøknad && behandling.søknad.erDigitalSøknad()) { "Forventet at søknaden var en innvilgbar digital søknad" }
        val innvilgelsesperiode = behandling.søknad.tiltaksdeltakelseperiodeDetErSøktOm()
        val barnetillegg = utledBarnetillegg(behandling)
        val tiltaksdeltakelse = utledTiltaksdeltakelser(behandling)
        val saksbehandler = AUTOMATISK_SAKSBEHANDLER
        val oppdaterKommando = OppdaterSøknadsbehandlingKommando.Innvilgelse(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            // Kommentar jah: Det føles litt vondt og gjenbruke denne kommandoen for det tilfellet her. For automatisk behandling krever vi at det er 1 søknad for 1 tiltak og saksopplysningene bare har funnet en tiltaksdeltakelse.
            innvilgelsesperioder = listOf(
                PeriodeMedVerdi(
                    verdi = InnvilgelsesperiodeKommando(
                        periode = innvilgelsesperiode,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(
                            utledAntallDagerPerMeldeperiode(
                                behandling,
                            ),
                        ),
                        internDeltakelseId = tiltaksdeltakelse.tiltaksdeltakerId,
                    ),
                    periode = innvilgelsesperiode,
                ),
            ).tilIkkeTomPeriodisering(),
            barnetillegg = barnetillegg,
            automatiskSaksbehandlet = true,
        )

        val (utbetaling, simuleringMedMetadata) = this.beregnInnvilgelse(
            behandlingId = behandling.id,
            vedtaksperiode = innvilgelsesperiode,
            innvilgelsesperioder = oppdaterKommando.tilInnvilgelseperioder(behandling),
            barnetilleggsperioder = barnetillegg.periodisering,
        )?.let {
            val navkontor = navkontorService.hentOppfolgingsenhet(this.fnr)
            val simuleringMedMetadata = simulerService.simulerSøknadsbehandlingEllerRevurdering(
                behandling = behandling,
                beregning = it,
                forrigeUtbetaling = this.utbetalinger.lastOrNull(),
                meldeperiodeKjeder = this.meldeperiodeKjeder,
                saksbehandler = saksbehandler.navIdent,
                kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
            ) { navkontor }.getOrElse { null }

            BehandlingUtbetaling(
                beregning = it,
                navkontor = navkontor,
                simulering = simuleringMedMetadata?.simulering,
            ) to simuleringMedMetadata
        } ?: (null to null)

        val oppdatertBehandling = behandling.oppdater(
            kommando = oppdaterKommando,
            clock = clock,
            utbetaling = utbetaling,
            omgjørRammevedtak = this.vedtaksliste.finnRammevedtakSomOmgjøres(innvilgelsesperiode),
        ).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere behandling med id ${behandling.id} fordi: ${it::class.simpleName}")
        }

        val tilBeslutningKommando = SendBehandlingTilBeslutningKommando(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        )

        oppdatertBehandling.tilBeslutning(tilBeslutningKommando, clock).mapLeft {
            throw IllegalStateException("Kunne ikke sende behandling med id ${behandling.id} til beslutning fordi: ${it::class.simpleName}")
        }.map {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                behandlingRepo.oppdaterSimuleringMetadata(it.id, simuleringMedMetadata?.originalResponseBody, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
        log.info { "Behandling med id ${behandling.id} er sendt til beslutning, correlationId $correlationId" }
    }

    private suspend fun sendTilManuellBehandling(
        behandling: Søknadsbehandling,
        manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
        correlationId: CorrelationId,
    ) {
        behandling.tilManuellBehandling(manueltBehandlesGrunner, clock).also {
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
        manueltBehandlesGrunner.forEach {
            SOKNAD_BEHANDLES_MANUELT_GRUNN.labelValues(it.name).inc()
        }
        log.info { "Behandling med id ${behandling.id} er gjort klar til manuell behandling, correlationId $correlationId" }
    }

    private fun kanBehandleAutomatisk(behandling: Søknadsbehandling): List<ManueltBehandlesGrunn> {
        val manueltBehandlesGrunner = mutableListOf<ManueltBehandlesGrunn>()
        require(
            behandling.søknad is InnvilgbarSøknad &&
                behandling.søknad.erDigitalSøknad(),
        ) { "Kan bare automatisk behandle digitale søknader fra bruker ${behandling.søknad.id}" }

        if (behandling.søknad.harLivsoppholdYtelser()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_HAR_ANDRE_YTELSER)
        }
        if (behandling.søknad.harLagtTilBarnManuelt()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_HAR_LAGT_TIL_BARN_MANUELT)
        }
        if (behandling.søknad.harBarnUtenforEOS()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_BARN_UTENFOR_EOS)
        }
        if (behandling.søknad.harBarnSomFyller16FørDato(behandling.søknad.tiltaksdeltakelseperiodeDetErSøktOm().tilOgMed)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_BARN_FYLLER_16_I_SOKNADSPERIODEN)
        }
        if (behandling.søknad.harBarnSomBleFødtEtterDato(behandling.søknad.tiltaksdeltakelseperiodeDetErSøktOm().fraOgMed)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_BARN_FODT_I_SOKNADSPERIODEN)
        }
        if (behandling.søknad.harKvp()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_HAR_KVP)
        }
        if (behandling.søknad.harIntro()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_INTRO)
        }
        if (behandling.søknad.harInstitusjonsopphold()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_INSTITUSJONSOPPHOLD)
        }

        val soknadstiltakFraSaksopplysning = getSoknadstiltakFraSaksopplysning(behandling)

        if (soknadstiltakFraSaksopplysning == null) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_FANT_IKKE_TILTAK)
        } else if (tiltakManglerPeriode(soknadstiltakFraSaksopplysning)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_TILTAK_MANGLER_PERIODE)
        } else if (tiltakFraSoknadHarEndretPeriode(behandling.søknad.tiltak, soknadstiltakFraSaksopplysning)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_ULIK_TILTAKSPERIODE)
        }

        if (soknadstiltakFraSaksopplysning != null) {
            if (!soknadstiltakFraSaksopplysning.deltakelseStatus.deltarEllerHarDeltatt()) {
                manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_HAR_IKKE_DELTATT_PA_TILTAK)
            }
            val deltakelsesprosent = soknadstiltakFraSaksopplysning.getDeltakelsesprosent()
            val manglerDagerPerUke =
                soknadstiltakFraSaksopplysning.antallDagerPerUke == null || soknadstiltakFraSaksopplysning.antallDagerPerUke == 0F
            if (manglerDagerPerUke && deltakelsesprosent == null) {
                manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_TILTAK_MANGLER_DELTAKELSESMENGDE)
            }
            if (soknadstiltakFraSaksopplysning.antallDagerPerUke != null && soknadstiltakFraSaksopplysning.antallDagerPerUke > 5.0F) {
                manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_TILTAK_MER_ENN_FEM_DAGER_PER_UKE)
            }
            if (manglerDagerPerUke && deltakelsesprosent != null && deltakelsesprosent < 100.0F) {
                manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_DELTIDSTILTAK_UTEN_DAGER_PER_UKE)
            }
        }

        val tiltaksdeltakerId = behandling.søknad.tiltak.tiltaksdeltakerId
        val tiltaksperiode = Periode(behandling.søknad.tiltak.deltakelseFom, behandling.søknad.tiltak.deltakelseTom)

        if (behandling.saksopplysninger.harOverlappendeTiltaksdeltakelse(
                internDeltakelseId = tiltaksdeltakerId,
                tiltaksperiode = tiltaksperiode,
            )
        ) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_OVERLAPPENDE_TILTAK)
        } else if (behandling.saksopplysninger.harOverlappendeTiltaksdeltakelse(
                internDeltakelseId = tiltaksdeltakerId,
                tiltaksperiode = Periode(
                    tiltaksperiode.fraOgMed.minusDays(14),
                    tiltaksperiode.tilOgMed.plusDays(14),
                ),
            )
        ) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_MINDRE_ENN_14_DAGER_MELLOM_TILTAK_OG_SOKNAD)
        }

        if (behandling.saksopplysninger.harAndreYtelserEnnTiltakspenger()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_ANDRE_YTELSER)
        }

        if (behandling.saksopplysninger.harTiltakspengevedtakFraArena()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_VEDTAK_I_ARENA)
        }
        if (behandling.søknad.erUnder18ISoknadsperioden(behandling.saksopplysninger.fødselsdato)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_ER_UNDER_18_I_SOKNADSPERIODEN)
        }

        val behandlinger = behandlingRepo.hentAlleForFnr(behandling.fnr)
        if (behandlinger.any { !it.erAvsluttet && it.id != behandling.id }) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
        }
        if (behandlingOverlapperMedAnnetVedtak(behandling, behandlinger)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_VEDTAK_FOR_SAMME_PERIODE)
        }

        if (behandling.søknad.harSoktMerEnn3ManederEtterOppstart()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_HAR_SOKT_FOR_SENT)
        }

        return manueltBehandlesGrunner
    }

    private fun getSoknadstiltakFraSaksopplysning(behandling: Søknadsbehandling): Tiltaksdeltakelse? {
        require(
            behandling.søknad is InnvilgbarSøknad &&
                behandling.søknad.erDigitalSøknad(),
        ) { "Kan bare automatisk behandle digitale søknader fra bruker ${behandling.søknad.id}" }
        return behandling.saksopplysninger.getTiltaksdeltakelse(behandling.søknad.tiltak.tiltaksdeltakerId)
    }

    private fun tiltakManglerPeriode(
        tiltakFraSaksopplysning: Tiltaksdeltakelse,
    ): Boolean {
        return tiltakFraSaksopplysning.deltakelseFraOgMed == null || tiltakFraSaksopplysning.deltakelseTilOgMed == null
    }

    private fun tiltakFraSoknadHarEndretPeriode(
        tiltakFraSoknad: Søknadstiltak,
        tiltakFraSaksopplysning: Tiltaksdeltakelse,
    ): Boolean =
        tiltakFraSaksopplysning.deltakelseFraOgMed != tiltakFraSoknad.deltakelseFom ||
            tiltakFraSaksopplysning.deltakelseTilOgMed != tiltakFraSoknad.deltakelseTom

    private fun behandlingOverlapperMedAnnetVedtak(
        behandling: Søknadsbehandling,
        behandlinger: List<Rammebehandling>,
    ): Boolean {
        val vedtatteBehandlinger = behandlinger.filter { it.erVedtatt }
        // TODO jah: Denne kan smelle dersom tiltaksdeltakelsen det er søkt på mangler fom eller tom. I så fall legg det til som en [ManueltBehandlesGrunn]
        return vedtatteBehandlinger.any { it.vedtaksperiode!!.overlapperMed(behandling.saksopplysningsperiode!!) }
    }

    private fun utledBarnetillegg(behandling: Søknadsbehandling): Barnetillegg {
        require(behandling.søknad is InnvilgbarSøknad && behandling.søknad.erDigitalSøknad()) { "Forventet at søknaden var en innvilgbar digital søknad" }
        val periode = behandling.søknad.tiltaksdeltakelseperiodeDetErSøktOm()

        return if (behandling.søknad.barnetillegg.isNotEmpty()) {
            val antallBarnFraSøknad = behandling.søknad.barnetillegg.size
            Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    AntallBarn(antallBarnFraSøknad),
                    periode,
                ),
                begrunnelse = null,
            )
        } else {
            Barnetillegg.utenBarnetillegg(periode)
        }
    }

    private fun utledTiltaksdeltakelser(
        behandling: Søknadsbehandling,
    ): Søknadstiltak {
        require(behandling.søknad is InnvilgbarSøknad && behandling.søknad.erDigitalSøknad()) {
            "Forventet at søknaden var en innvilgbar digital søknad"
        }
        return behandling.søknad.tiltak
    }

    private fun utledAntallDagerPerMeldeperiode(
        behandling: Søknadsbehandling,
    ): Int {
        require(behandling.søknad is InnvilgbarSøknad && behandling.søknad.erDigitalSøknad()) { "Forventet at søknaden var en innvilgbar digital søknad. BehandlingId: ${behandling.id}" }

        val soknadstiltakFraSaksopplysning = behandling.søknad.tiltak
            .let { tiltak -> behandling.saksopplysninger.getTiltaksdeltakelse(tiltak.tiltaksdeltakerId) }
            ?: throw IllegalStateException("Må ha tiltaksdeltakelse for å kunne behandle automatisk. BehandlingId: ${behandling.id}")

        require((soknadstiltakFraSaksopplysning.antallDagerPerUke != null && soknadstiltakFraSaksopplysning.antallDagerPerUke > 0) || soknadstiltakFraSaksopplysning.getDeltakelsesprosent() == 100.0F) {
            "Tiltaksdeltakelser som mangler dagerPerUke og ikke har deltakelsesprosent 100% kan ikke behandles automatisk. BehandlingId: ${behandling.id}"
        }

        return if (soknadstiltakFraSaksopplysning.antallDagerPerUke != null && soknadstiltakFraSaksopplysning.antallDagerPerUke > 0) {
            getDagerPerMeldeperiode(soknadstiltakFraSaksopplysning.antallDagerPerUke)
        } else {
            DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
        }
    }

    private fun Tiltaksdeltakelse.getDeltakelsesprosent(): Float? {
        return deltakelseProsent
            ?: deltidsprosentGjennomforing?.toFloat()
    }

    private fun getDagerPerMeldeperiode(dagerPerUke: Float): Int {
        return ((dagerPerUke + 0.5F).toInt()) * 2
    }
}
