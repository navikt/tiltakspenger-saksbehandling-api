package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortVedtakBrevCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient

class ForhåndsvisBrevMeldekortBehandlingService(
    val genererBrevClient: GenererVedtaksbrevForUtbetalingKlient,
    val sakService: SakService,
    val meldekortbehandlingRepo: MeldekortBehandlingRepo,
    val navIdentClient: NavIdentClient,
) {
    suspend fun forhåndsvisBrev(command: ForhåndsvisBrevMeldekortbehandlingCommand): Either<KunneIkkeForhåndsviseBrevMeldekortBehandling, PdfOgJson> {
        val meldekortBehandling = meldekortbehandlingRepo.hent(command.meldekortbehandlingId)
            ?: return KunneIkkeForhåndsviseBrevMeldekortBehandling.FantIkkeMeldekortbehandling.left()

        val sak = sakService.hentForSakId(meldekortBehandling.sakId)

        val beregning = if (command.dager?.tilMeldekortDager(meldekortBehandling.meldeperiode) == null) {
            null
        } else {
            sak.beregnMeldekort(
                meldekortIdSomBeregnes = meldekortBehandling.id,
                meldeperiodeSomBeregnes = command.dager.tilMeldekortDager(meldekortBehandling.meldeperiode),
            )
        }

        val nåværendeBeregningMedTidligereBeregning =
            beregning?.map { meldeperiodeBeregning ->
                val tidligereBeregning = sak.meldeperiodeBeregninger.hentForrigeBeregningEllerSiste(
                    meldeperiodeBeregning.id,
                    meldeperiodeBeregning.kjedeId,
                )

                tidligereBeregning to meldeperiodeBeregning
            }

        val hentSaksbehandlersNavn: suspend (String) -> String =
            if (meldekortBehandling is MeldekortBehandletAutomatisk) {
                { "Automatisk behandlet" }
            } else {
                navIdentClient::hentNavnForNavIdent
            }

        return genererBrevClient.genererMeldekortvedtakBrev(
            command = GenererMeldekortVedtakBrevCommand(
                sakId = meldekortBehandling.sakId,
                saksnummer = meldekortBehandling.saksnummer,
                fnr = meldekortBehandling.fnr,
                saksbehandler = meldekortBehandling.saksbehandler,
                beslutter = meldekortBehandling.beslutter,
                meldekortbehandlingId = meldekortBehandling.id,
                beregningsperiode = beregning?.let {
                    Periode(
                        fraOgMed = it.minOf { it.fraOgMed },
                        tilOgMed = it.maxOf { it.tilOgMed },
                    )
                },
                tiltaksdeltakelser = meldekortBehandling.rammevedtak!!.let {
                    sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(it)
                },
                iverksattTidspunkt = null,
                erKorrigering = meldekortBehandling.erKorrigering,
                beregninger = nåværendeBeregningMedTidligereBeregning,
                totaltBeløp = beregning?.sumOf { it.totalBeløp },
                tekstTilVedtaksbrev = command.tekstTilVedtaksbrev,
                forhåndsvisning = true,
            ),
            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
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
}

data class ForhåndsvisBrevMeldekortbehandlingCommand(
    val meldekortbehandlingId: MeldekortId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val tekstTilVedtaksbrev: NonBlankString?,
    val dager: Dager?,
)
