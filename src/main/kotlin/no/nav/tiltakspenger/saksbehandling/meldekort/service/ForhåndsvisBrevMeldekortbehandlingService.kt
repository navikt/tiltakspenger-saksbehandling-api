package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortvedtakBrevKommando
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortvedtakBrevKommandoV2
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock

class ForhåndsvisBrevMeldekortbehandlingService(
    val genererBrevClient: GenererVedtaksbrevForMeldekortKlient,
    val sakService: SakService,
    val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    val navIdentClient: NavIdentClient,
    val clock: Clock,
    val brukMeldekortvedtakBrevV2: Boolean,
) {

    suspend fun forhåndsvisBrev(kommando: ForhåndsvisBrevMeldekortbehandlingKommando): Either<KunneIkkeForhåndsviseBrevMeldekortbehandling, PdfOgJson> {
        val meldekortbehandling = meldekortbehandlingRepo.hent(kommando.meldekortbehandlingId)
            ?: return KunneIkkeForhåndsviseBrevMeldekortbehandling.FantIkkeMeldekortbehandling.left()

        val sak = sakService.hentForSakId(meldekortbehandling.sakId)

        val beregning = sak.beregnMeldekort(
            meldekortIdSomBeregnes = meldekortbehandling.id,
            meldeperioderSomBeregnes = kommando.meldeperioder
                .map {
                    it.tilUtfyltMeldeperiode(
                        sak.meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(it.kjedeId),
                    )
                }
                .toNonEmptyListOrThrow(),
            beregningstidspunkt = nå(clock),
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

        val beregningsperiode = beregning.let {
            Periode(
                fraOgMed = it.minOf { it.fraOgMed },
                tilOgMed = it.maxOf { it.tilOgMed },
            )
        }
        val tiltaksdeltakelser = meldekortbehandling.rammevedtakIder.let {
            sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(it)
        }

        return if (brukMeldekortvedtakBrevV2) {
            genererBrevClient.genererMeldekortvedtakBrevV2(
                kommando = GenererMeldekortvedtakBrevKommandoV2(
                    sakId = meldekortbehandling.sakId,
                    saksnummer = meldekortbehandling.saksnummer,
                    fnr = meldekortbehandling.fnr,
                    saksbehandler = meldekortbehandling.saksbehandler,
                    beslutter = meldekortbehandling.beslutter,
                    meldekortbehandlingId = meldekortbehandling.id,
                    beregningsperiode = beregningsperiode,
                    tiltaksdeltakelser = tiltaksdeltakelser,
                    iverksattTidspunkt = null,
                    erKorrigering = meldekortbehandling.harKorrigering,
                    beregninger = nåværendeBeregningMedTidligereBeregning,
                    totaltBeløp = beregning.sumOf { it.totalBeløp },
                    tekstTilVedtaksbrev = kommando.tekstTilVedtaksbrev,
                    forhåndsvisning = true,
                ),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            ).map { it.first }
        } else {
            genererBrevClient.genererMeldekortvedtakBrev(
                kommando = GenererMeldekortvedtakBrevKommando(
                    sakId = meldekortbehandling.sakId,
                    saksnummer = meldekortbehandling.saksnummer,
                    fnr = meldekortbehandling.fnr,
                    saksbehandler = meldekortbehandling.saksbehandler,
                    beslutter = meldekortbehandling.beslutter,
                    meldekortbehandlingId = meldekortbehandling.id,
                    beregningsperiode = beregningsperiode,
                    tiltaksdeltakelser = tiltaksdeltakelser,
                    iverksattTidspunkt = null,
                    erKorrigering = meldekortbehandling.harKorrigering,
                    beregninger = nåværendeBeregningMedTidligereBeregning,
                    totaltBeløp = beregning.sumOf { it.totalBeløp },
                    tekstTilVedtaksbrev = kommando.tekstTilVedtaksbrev,
                    forhåndsvisning = true,
                ),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }.map {
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

data class ForhåndsvisBrevMeldekortbehandlingKommando(
    val meldekortbehandlingId: MeldekortId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val tekstTilVedtaksbrev: NonBlankString?,
    val meldeperioder: List<OppdatertMeldeperiode>,
)
