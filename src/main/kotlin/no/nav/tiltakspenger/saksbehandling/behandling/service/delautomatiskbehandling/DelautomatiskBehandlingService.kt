package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
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
            log.info { "Kan behandle behandling med id ${behandling.id} automatisk, sender til beslutning" }
            sendTilBeslutning(behandling, correlationId)
            SOKNAD_BEHANDLET_DELVIS_AUTOMATISK.inc()
        } else {
            log.info { "Kan ikke behandle behandling med id ${behandling.id} automatisk, sender til manuell behandling" }
            sendTilManuellBehandling(behandling, manueltBehandlesGrunner)
            SOKNAD_IKKE_BEHANDLET_AUTOMATISK.inc()
        }
    }

    private suspend fun sendTilBeslutning(behandling: Søknadsbehandling, correlationId: CorrelationId) {
        val kommando = SendSøknadsbehandlingTilBeslutningKommando(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            behandlingsperiode = behandling.søknad.vurderingsperiode(),
            barnetillegg = utledBarnetillegg(behandling),
            tiltaksdeltakelser = utledTiltaksdeltakelser(behandling),
            antallDagerPerMeldeperiode = utledAntallDagerPerMeldeperiode(behandling),
            avslagsgrunner = null,
            resultat = SøknadsbehandlingType.INNVILGELSE,
            automatiskSaksbehandlet = true,
        )
        behandling.tilBeslutning(kommando, clock).also {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
        log.info { "Behandling med id ${behandling.id} er sendt til beslutning" }
    }

    private suspend fun sendTilManuellBehandling(
        behandling: Søknadsbehandling,
        manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
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
        log.info { "Behandling med id ${behandling.id} er gjort klar til manuell behandling" }
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
        if (behandling.søknad.harBarnSomFyller16FørDato(behandling.søknad.vurderingsperiode().tilOgMed)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_BARN_FYLLER_16_I_SOKNADSPERIODEN)
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
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.SOKNAD_HAR_ANDRE_YTELSER)
        }

        val behandlinger = behandlingRepo.hentAlleForFnr(behandling.fnr)
        if (behandlinger.any { !it.erAvsluttet && it.id != behandling.id }) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
        }
        if (behandlingOverlapperMedAnnenBehandling(behandling, behandlinger)) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_BEHANDLING_FOR_SAMME_PERIODE)
        }

        if (behandling.søknad.harSoktMerEnn3ManederEtterOppstart()) {
            manueltBehandlesGrunner.add(ManueltBehandlesGrunn.ANNET_HAR_SOKT_FOR_SENT)
        }

        return manueltBehandlesGrunner
    }

    private fun tiltakFraSoknadHarEndretPeriode(
        tiltakFraSoknad: Søknadstiltak,
        tiltakFraSaksopplysning: Tiltaksdeltagelse,
    ): Boolean =
        tiltakFraSaksopplysning.deltagelseFraOgMed != tiltakFraSoknad.deltakelseFom ||
            tiltakFraSaksopplysning.deltagelseTilOgMed != tiltakFraSoknad.deltakelseTom

    private fun behandlingOverlapperMedAnnenBehandling(
        behandling: Søknadsbehandling,
        behandlinger: List<Behandling>,
    ): Boolean {
        val vedtatteBehandlinger = behandlinger.filter { it.erVedtatt }
        return vedtatteBehandlinger.any { it.virkningsperiode!!.overlapperMed(behandling.saksopplysningsperiode) }
    }

    private fun utledBarnetillegg(behandling: Søknadsbehandling): Barnetillegg? {
        return if (behandling.søknad.barnetillegg.isNotEmpty()) {
            val periode = behandling.søknad.vurderingsperiode()
            Barnetillegg.periodiserOgFyllUtHullMed0(
                begrunnelse = null,
                perioder = listOf(Pair(periode, AntallBarn(behandling.søknad.barnetillegg.size))),
                virkningsperiode = periode,
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
                behandling.søknad.vurderingsperiode(),
                behandling.søknad.tiltak.id,
            ),
        )
    }

    private fun utledAntallDagerPerMeldeperiode(
        behandling: Søknadsbehandling,
    ): Int {
        val soknadstiltakFraSaksopplysning =
            behandling.saksopplysninger.getTiltaksdeltagelse(behandling.søknad.tiltak.id)
                ?: throw IllegalStateException("Må ha tiltaksdeltakelse for å kunne behandle automatisk")
        return if (soknadstiltakFraSaksopplysning.antallDagerPerUke != null && soknadstiltakFraSaksopplysning.antallDagerPerUke > 0) {
            (soknadstiltakFraSaksopplysning.antallDagerPerUke * 2).toInt()
        } else {
            10
        }
    }
}
