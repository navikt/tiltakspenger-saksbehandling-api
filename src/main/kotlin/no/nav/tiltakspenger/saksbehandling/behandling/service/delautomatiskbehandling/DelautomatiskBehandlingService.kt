package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.SOKNAD_BEHANDLES_MANUELT_GRUNN
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.SOKNAD_BEHANDLET_DELVIS_AUTOMATISK
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.SOKNAD_IKKE_BEHANDLET_AUTOMATISK
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.Clock

class DelautomatiskBehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun behandleAutomatisk(behandling: Søknadsbehandling, correlationId: CorrelationId) {
        val manueltBehandlesGrunner = kanBehandleAutomatisk(behandling)

        if (manueltBehandlesGrunner.isEmpty()) {
            log.info { "Kan behandle behandling med id ${behandling.id} automatisk, sender til beslutning, correlationId $correlationId" }
            sendTilBeslutning(behandling, correlationId)
            SOKNAD_BEHANDLET_DELVIS_AUTOMATISK.inc()
        } else {
            log.info { "Kan ikke behandle behandling med id ${behandling.id} automatisk, sender til manuell behandling, correlationId $correlationId" }
            sendTilManuellBehandling(behandling, manueltBehandlesGrunner, correlationId)
            SOKNAD_IKKE_BEHANDLET_AUTOMATISK.inc()
        }
    }

    private suspend fun sendTilBeslutning(behandling: Søknadsbehandling, correlationId: CorrelationId) {
        val oppdaterKommando = OppdaterSøknadsbehandlingKommando.Innvilgelse(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            innvilgelsesperiode = behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm(),
            barnetillegg = utledBarnetillegg(behandling),
            tiltaksdeltakelser = utledTiltaksdeltakelser(behandling),
            antallDagerPerMeldeperiode = utledAntallDagerPerMeldeperiode(behandling),
            automatiskSaksbehandlet = true,
        )

        val oppdatertBehandling = behandling.oppdater(oppdaterKommando, clock).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere behandling med id ${behandling.id} fordi: ${it::class.simpleName}")
        }

        val tilBeslutningKommando = SendBehandlingTilBeslutningKommando(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = correlationId,
        )

        oppdatertBehandling.tilBeslutning(tilBeslutningKommando, clock).mapLeft {
            throw IllegalStateException("Kunne ikke sende behandling med id ${behandling.id} til beslutning fordi: ${it::class.simpleName}")
        }.map {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
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

        if (behandling.søknad.harLivsoppholdYtelser()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_HAR_ANDRE_YTELSER)
        }
        if (behandling.søknad.harLagtTilBarnManuelt()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_HAR_LAGT_TIL_BARN_MANUELT)
        }
        if (behandling.søknad.harBarnUtenforEOS()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_BARN_UTENFOR_EOS)
        }
        if (behandling.søknad.harBarnSomFyller16FørDato(behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm().tilOgMed)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_BARN_FYLLER_16_I_SOKNADSPERIODEN)
        }
        if (behandling.søknad.harBarnSomBleFødtEtterDato(behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm().fraOgMed)) {
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

        val soknadstiltakFraSaksopplysning =
            behandling.saksopplysninger.getTiltaksdeltagelse(behandling.søknad.tiltak.id)

        if (soknadstiltakFraSaksopplysning == null) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_FANT_IKKE_TILTAK)
        } else if (tiltakFraSoknadHarEndretPeriode(behandling.søknad.tiltak, soknadstiltakFraSaksopplysning)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_ULIK_TILTAKSPERIODE)
        }

        val tiltaksid = behandling.søknad.tiltak.id
        val tiltaksperiode = Periode(behandling.søknad.tiltak.deltakelseFom, behandling.søknad.tiltak.deltakelseTom)

        if (behandling.saksopplysninger.harOverlappendeTiltaksdeltakelse(
                eksternDeltakelseId = tiltaksid,
                tiltaksperiode = tiltaksperiode,
            )
        ) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_OVERLAPPENDE_TILTAK)
        } else if (behandling.saksopplysninger.harOverlappendeTiltaksdeltakelse(
                eksternDeltakelseId = tiltaksid,
                tiltaksperiode = Periode(tiltaksperiode.fraOgMed.minusDays(14), tiltaksperiode.tilOgMed.plusDays(14)),
            )
        ) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_MINDRE_ENN_14_DAGER_MELLOM_TILTAK_OG_SOKNAD)
        }

        if (behandling.saksopplysninger.harAndreYtelser()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SAKSOPPLYSNING_ANDRE_YTELSER)
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

        if (behandling.søknad.erUnder18ISoknadsperioden(behandling.saksopplysninger.fødselsdato)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_ER_UNDER_18_I_SOKNADSPERIODEN)
        }

        return manueltBehandlesGrunner
    }

    // TODO False positive løst i kotlin version 2.2.10-RC
    // https://youtrack.jetbrains.com/issue/KT-78352/False-positive-IDENTITYSENSITIVEOPERATIONSWITHVALUETYPE-when-comparing-with-equality-operator
    @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
    private fun tiltakFraSoknadHarEndretPeriode(
        tiltakFraSoknad: Søknadstiltak,
        tiltakFraSaksopplysning: Tiltaksdeltagelse,
    ): Boolean =
        tiltakFraSaksopplysning.deltagelseFraOgMed != tiltakFraSoknad.deltakelseFom ||
            tiltakFraSaksopplysning.deltagelseTilOgMed != tiltakFraSoknad.deltakelseTom

    private fun behandlingOverlapperMedAnnetVedtak(
        behandling: Søknadsbehandling,
        behandlinger: List<Behandling>,
    ): Boolean {
        val vedtatteBehandlinger = behandlinger.filter { it.erVedtatt }
        // TODO jah: Denne kan smelle dersom tiltaksdeltagelsen det er søkt på mangler fom eller tom. I så fall legg det til som en [ManueltBehandlesGrunn]
        return vedtatteBehandlinger.any { it.virkningsperiode!!.overlapperMed(behandling.saksopplysningsperiode!!) }
    }

    private fun utledBarnetillegg(behandling: Søknadsbehandling): Barnetillegg? {
        return if (behandling.søknad.barnetillegg.isNotEmpty()) {
            val periode = behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm()
            val antallBarnFraSøknad = behandling.søknad.barnetillegg.size
            Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    AntallBarn(antallBarnFraSøknad),
                    periode,
                ),
                begrunnelse = null,
            )
        } else {
            null
        }
    }

    private fun utledTiltaksdeltakelser(
        behandling: Søknadsbehandling,
    ): List<Pair<Periode, String>> {
        return listOf(
            Pair(
                behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm(),
                behandling.søknad.tiltak.id,
            ),
        )
    }

    private fun utledAntallDagerPerMeldeperiode(
        behandling: Søknadsbehandling,
    ): SammenhengendePeriodisering<AntallDagerForMeldeperiode> {
        val soknadstiltakFraSaksopplysning =
            behandling.saksopplysninger.getTiltaksdeltagelse(behandling.søknad.tiltak.id)
                ?: throw IllegalStateException("Må ha tiltaksdeltakelse for å kunne behandle automatisk")
        return if (soknadstiltakFraSaksopplysning.antallDagerPerUke != null && soknadstiltakFraSaksopplysning.antallDagerPerUke > 0) {
            SammenhengendePeriodisering(
                AntallDagerForMeldeperiode((soknadstiltakFraSaksopplysning.antallDagerPerUke * 2).toInt()),
                behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm(),
            )
        } else {
            SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(10),
                behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm(),
            )
        }
    }
}
