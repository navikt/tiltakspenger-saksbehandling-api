package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.LocalDateTime

class ForhåndsvisBrevMeldekortbehandlingService(
    val genererBrevClient: GenererVedtaksbrevForUtbetalingKlient,
    val sakService: SakService,
    val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    val navIdentClient: NavIdentClient,
) {
    suspend fun forhåndsvisBrev(command: ForhåndsvisBrevMeldekortbehandlingCommand): Either<KunneIkkeForhåndsviseBrevMeldekortbehandling, PdfOgJson> {
        val meldekortbehandling = meldekortbehandlingRepo.hent(command.meldekortbehandlingId)
            ?: return KunneIkkeForhåndsviseBrevMeldekortbehandling.FantIkkeMeldekortbehandling.left()

        val sak = sakService.hentForSakId(meldekortbehandling.sakId)

        val beregning = sak.beregnMeldekort(
            meldekortIdSomBeregnes = meldekortbehandling.id,
            meldeperioderSomBeregnes = nonEmptyListOf(command.dager.tilUtfyltMeldeperiode(meldekortbehandling.meldeperiode)),
            beregningstidspunkt = LocalDateTime.now(),
        )

        val nåværendeBeregningMedTidligereBeregning =
            beregning.map { meldeperiodeBeregning ->
                val tidligereBeregning = sak.meldeperiodeBeregninger.hentForrigeBeregningEllerSiste(
                    meldeperiodeBeregning.id,
                    meldeperiodeBeregning.kjedeId,
                )

                tidligereBeregning to meldeperiodeBeregning
            }

        val hentSaksbehandlersNavn: suspend (String) -> String =
            if (meldekortbehandling is MeldekortBehandletAutomatisk) {
                { "Automatisk behandlet" }
            } else {
                navIdentClient::hentNavnForNavIdent
            }

        return genererBrevClient.genererMeldekortvedtakBrev(
            command = GenererMeldekortVedtakBrevCommand(
                sakId = meldekortbehandling.sakId,
                saksnummer = meldekortbehandling.saksnummer,
                fnr = meldekortbehandling.fnr,
                saksbehandler = meldekortbehandling.saksbehandler,
                beslutter = meldekortbehandling.beslutter,
                meldekortbehandlingId = meldekortbehandling.id,
                beregningsperiode = beregning.let {
                    Periode(
                        fraOgMed = it.minOf { it.fraOgMed },
                        tilOgMed = it.maxOf { it.tilOgMed },
                    )
                },
                tiltaksdeltakelser = meldekortbehandling.rammevedtakIder.let {
                    sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(it)
                },
                iverksattTidspunkt = null,
                erKorrigering = meldekortbehandling.erKorrigering,
                beregninger = nåværendeBeregningMedTidligereBeregning,
                totaltBeløp = beregning.sumOf { it.totalBeløp },
                tekstTilVedtaksbrev = command.tekstTilVedtaksbrev,
                forhåndsvisning = true,
            ),
            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        ).map {
            it
        }.mapLeft {
            KunneIkkeForhåndsviseBrevMeldekortbehandling.FeilVedGenereringAvPdf(it)
        }
    }
}

sealed interface KunneIkkeForhåndsviseBrevMeldekortbehandling {
    object FantIkkeMeldekortbehandling : KunneIkkeForhåndsviseBrevMeldekortbehandling
    data class FeilVedGenereringAvPdf(val feil: KunneIkkeGenererePdf) : KunneIkkeForhåndsviseBrevMeldekortbehandling
}

data class ForhåndsvisBrevMeldekortbehandlingCommand(
    val meldekortbehandlingId: MeldekortId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val tekstTilVedtaksbrev: NonBlankString?,
    val dager: OppdatertMeldeperiode, // :_(
)
