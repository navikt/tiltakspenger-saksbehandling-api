package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import java.time.Clock

class IverksettMeldekortService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val utbetalingRepo: UtbetalingRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun iverksettMeldekort(
        kommando: IverksettMeldekortKommando,
    ): Either<KanIkkeIverksetteMeldekort, Pair<Sak, MeldekortBehandling>> {
        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        val sak = sakService.hentForSakId(sakId)
        val meldekortBehandling: MeldekortBehandling = sak.hentMeldekortBehandling(meldekortId)
            ?: throw IllegalArgumentException("Fant ikke meldekort med id $meldekortId i sak $sakId")

        require(meldekortBehandling is MeldekortBehandletManuelt) {
            "Meldekortet må være behandlet for å iverksettes"
        }
        require(meldekortBehandling.beslutter != null && meldekortBehandling.status == MeldekortBehandlingStatus.UNDER_BESLUTNING) {
            return KanIkkeIverksetteMeldekort.BehandlingenErIkkeUnderBeslutning.left()
        }

        val meldeperiode = meldekortBehandling.meldeperiode
        check(sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            "Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: $sakId, meldekortId: $meldekortId"
        }

        return meldekortBehandling.iverksettMeldekort(kommando.beslutter, clock).map { iverksattMeldekortbehandling ->
            val meldekortvedtak = iverksattMeldekortbehandling.opprettVedtak(
                forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                clock = clock,
            )

            val statistikkDTO = statistikkService.generer(iverksattMeldekortbehandling.tilStatistikkMeldekortDTO(clock))
            val oppdatertSak = sak.oppdaterMeldekortbehandling(iverksattMeldekortbehandling)
                .leggTilMeldekortvedtak(meldekortvedtak)
            sessionFactory.withTransactionContext { tx ->
                meldekortBehandlingRepo.oppdater(iverksattMeldekortbehandling, tx)
                meldekortvedtakRepo.lagre(meldekortvedtak, tx)
                statistikkService.lagre(statistikkDTO, tx)

                runBlocking {
                    tx.onSuccess {
                        if (meldekortvedtak.meldekortBehandling.harFeilutbetaling()) {
                            logger.warn { "Meldekort med feilutbetaling har blitt iverksatt - Meldekort-id $meldekortId - vedtak-id: ${meldekortvedtak.id} - sak-id: $sakId" }
                            MetricRegister.IVERKSATT_MELDEKORT_MED_FEILUTBETALING.inc()
                        }
                    }
                }
            }
            oppdatertSak to iverksattMeldekortbehandling
        }
    }
}
