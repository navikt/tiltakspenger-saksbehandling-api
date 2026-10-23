package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn.KanIkkeUnderkjenneMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn.UnderkjennMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class UnderkjennMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val clock: Clock,
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    suspend fun underkjenn(
        command: UnderkjennMeldekortbehandlingKommando,
    ): Either<KanIkkeUnderkjenneMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortbehandling: Meldekortbehandling = meldekortbehandlingRepo.hent(command.meldekortId)!!

        if (meldekortbehandling !is MeldekortbehandlingManuell) {
            return KanIkkeUnderkjenneMeldekortbehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        val begrunnelse = Either.catch { command.begrunnelse.toNonBlankString() }.getOrElse {
            return KanIkkeUnderkjenneMeldekortbehandling.BegrunnelseMåVæreUtfylt.left()
        }
        return meldekortbehandling.underkjenn(
            besluttersBegrunnelse = begrunnelse,
            beslutter = command.saksbehandler,
            clock = clock,
        ).map { oppdatertMeldekortbehandling ->
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(oppdatertMeldekortbehandling, StatistikkhendelseType.UNDERKJENT_BEHANDLING),
            )
            val oppdatert = sessionFactory.withTransactionContext { tx ->
                meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                    oppdatertMeldekortbehandling,
                    utøvendeBeslutter = command.saksbehandler,
                    transactionContext = tx,
                ).also { if (it) statistikkService.lagre(statistikk, tx) }
            }
            if (!oppdatert) {
                return KanIkkeUnderkjenneMeldekortbehandling.BehandlingenErIkkeLengerUnderBeslutning.left()
            }
            sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling) to oppdatertMeldekortbehandling
        }
    }
}
