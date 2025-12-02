package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortVedtakBrevCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo

class ForhåndsvisBrevMeldekortBehandlingService(
    val genererBrevClient: GenererVedtaksbrevForUtbetalingKlient,
    val sakService: SakService,
    val meldekortbehandlingRepo: MeldekortBehandlingRepo,
) {
    suspend fun forhåndsvisBrev(command: ForhåndsvisBrevMeldekortbehandlingCommand): Either<KunneIkkeForhåndsviseBrevMeldekortBehandling, PdfOgJson> {
        val meldekortBehandling = meldekortbehandlingRepo.hent(command.meldekortbehandlingId)
            ?: return KunneIkkeForhåndsviseBrevMeldekortBehandling.FantIkkeMeldekortbehandling.left()

        val sak = sakService.hentForSakId(meldekortBehandling.sakId)

        if (meldekortBehandling.beregning == null) {
            return KunneIkkeForhåndsviseBrevMeldekortBehandling.BehandlingMåHaBeregningForÅForhåndsviseBrev.left()
        }
        // TODO - verifiser at vi kommer alltid til å ha tiltaksdeltakelser her.
        val tiltaksdeltakelser = meldekortBehandling.rammevedtak?.let {
            sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(it)
        }

        require(tiltaksdeltakelser != null && tiltaksdeltakelser.isNotEmpty()) {
            "Forventet at et det skal finnes tiltaksdeltakelse for meldekortvedtaksperioden"
        }

        val tidligereBeregninger = meldekortBehandling.beregning!!.beregninger.map {
            val tidligereBeregning = sak.meldeperiodeBeregninger.hentForrigeBeregning(it.id, it.kjedeId)

            tidligereBeregning.getOrElse { null } to it
        }

        return genererBrevClient.genererMeldekortvedtakBrev(
            command = GenererMeldekortVedtakBrevCommand(
                sakId = meldekortBehandling.sakId,
                saksnummer = meldekortBehandling.saksnummer,
                fnr = meldekortBehandling.fnr,
                // TODO - skal hele tiden bruke saksbeahndleren som eier behandlingen, eller den som forhåndsviser brevet?
                saksbehandler = meldekortBehandling.saksbehandler ?: command.saksbehandler.navIdent,
                // TODO - vi har kanskje lyst til å injecte beslutteren som ser på brevet - ellers vises den ikke.
                beslutter = meldekortBehandling.beslutter,
                meldekortbehandlingId = meldekortBehandling.id,
                beregningsperiode = meldekortBehandling.beregning!!.periode,
                tiltaksdeltakelser = tiltaksdeltakelser,
                iverksattTidspunkt = null,
                erKorrigering = meldekortBehandling.erKorrigering,
                beregninger = tidligereBeregninger,
                totaltBeløp = meldekortBehandling.beregning!!.totalBeløp,
                tekstTilVedtaksbrev = command.tekstTilVedtaksbrev,
            ),
            hentSaksbehandlersNavn = { saksbehandlerId -> "Saksbehandler Navn for $saksbehandlerId" },
        ).map {
            it
        }.mapLeft {
            KunneIkkeForhåndsviseBrevMeldekortBehandling.FeilVedGenereringAvPdf(it)
        }
    }
}

sealed interface KunneIkkeForhåndsviseBrevMeldekortBehandling {
    object FantIkkeMeldekortbehandling : KunneIkkeForhåndsviseBrevMeldekortBehandling
    data class FeilVedGenereringAvPdf(val feil: KunneIkkeGenererePdf) : KunneIkkeForhåndsviseBrevMeldekortBehandling
    object BehandlingMåHaBeregningForÅForhåndsviseBrev : KunneIkkeForhåndsviseBrevMeldekortBehandling
}

data class ForhåndsvisBrevMeldekortbehandlingCommand(
    val meldekortbehandlingId: MeldekortId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val tekstTilVedtaksbrev: NonBlankString?,
)
