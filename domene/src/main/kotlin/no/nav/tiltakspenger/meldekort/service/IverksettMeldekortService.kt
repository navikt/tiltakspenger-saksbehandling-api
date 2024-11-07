package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.utbetaling.domene.opprettUtbetalingsvedtak
import no.nav.tiltakspenger.utbetaling.domene.tilStatistikk
import no.nav.tiltakspenger.utbetaling.ports.UtbetalingsvedtakRepo

class IverksettMeldekortService(
    val sakService: SakService,
    val meldekortRepo: MeldekortRepo,
    val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
) {
    suspend fun iverksettMeldekort(
        kommando: IverksettMeldekortKommando,
    ): Either<KanIkkeIverksetteMeldekort, Meldekort.UtfyltMeldekort> {
        if (!kommando.beslutter.erBeslutter()) {
            return KanIkkeIverksetteMeldekort.MåVæreBeslutter(kommando.beslutter.roller).left()
        }
        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        kastHvisIkkeTilgangTilPerson(kommando.beslutter, meldekortId, kommando.correlationId)

        val sak = sakService.hentForSakId(sakId, kommando.beslutter, kommando.correlationId)
            .getOrElse { return KanIkkeIverksetteMeldekort.KunneIkkeHenteSak(it).left() }
        val meldekort: Meldekort = sak.hentMeldekort(meldekortId)
            ?: throw IllegalArgumentException("Fant ikke meldekort med id $meldekortId i sak $sakId")
        meldekort as Meldekort.UtfyltMeldekort
        require(meldekort.beslutter == null && meldekort.status == MeldekortStatus.KLAR_TIL_BESLUTNING) {
            "Meldekort $meldekortId er allerede iverksatt"
        }
        val rammevedtak = sak.rammevedtak
            ?: throw IllegalArgumentException("Fant ikke rammevedtak for sak $sakId")

        return meldekort.iverksettMeldekort(kommando.beslutter).onRight { iverksattMeldekort ->
            val nesteMeldekort = iverksattMeldekort.opprettNesteMeldekort(rammevedtak.utfallsperioder)
            val eksisterendeUtbetalingsvedtak = sak.utbetalinger
            val utbetalingsvedtak =
                iverksattMeldekort.opprettUtbetalingsvedtak(rammevedtak, eksisterendeUtbetalingsvedtak.lastOrNull()?.id)
            val utbetalingsstatistikk = utbetalingsvedtak.tilStatistikk()
            sessionFactory.withTransactionContext { tx ->
                meldekortRepo.oppdater(iverksattMeldekort, tx)
                nesteMeldekort.onRight { meldekortRepo.lagre(it, tx) }
                utbetalingsvedtakRepo.lagre(utbetalingsvedtak, tx)
                statistikkStønadRepo.lagre(utbetalingsstatistikk, tx)
            }
        }
    }

    private suspend fun kastHvisIkkeTilgangTilPerson(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        correlationId: CorrelationId,
    ) {
        val fnr = personService.hentFnrForMeldekortId(meldekortId)
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            ).onLeft {
                throw IkkeFunnetException("Feil ved sjekk av tilgang til person. meldekortId: $meldekortId. CorrelationId: $correlationId")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person")
            }
    }
}
