package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KanIkkeForhåndsviseBrev
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.forhåndsvisKlagebrev
import no.nav.tiltakspenger.saksbehandling.klage.ports.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.hentNavnForNavIdentEllerKast
import java.time.Clock
import java.time.LocalDate

class ForhåndsvisBrevKlagebehandlingService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
    private val genererKlagebrevKlient: GenererKlagebrevKlient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun forhåndsvisBrev(
        kommando: KlagebehandlingBrevKommando,
    ): Either<KanIkkeForhåndsviseBrev, Pair<PdfA, PdfA?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.forhåndsvisKlagebrev(
            kommando = kommando,
            genererAvvisningsbrev = { saksnummer, fnr, saksbehandlerNavIdent, tilleggstekst, forhåndsvisning ->
                genererKlagebrevKlient.genererAvvisningsvedtak(
                    saksnummer = saksnummer,
                    fnr = fnr,
                    tilleggstekst = tilleggstekst,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    forhåndsvisning = forhåndsvisning,
                    vedtaksdato = LocalDate.now(clock),
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
                )
            },
            genererKlageInnstillingsbrev = { saksnummer, fnr, saksbehandlerNavIdent, tilleggstekst, forhåndsvisning, innsendingsdato, datoVedtak ->
                genererKlagebrevKlient.genererInnstillingsbrev(
                    saksnummer = saksnummer,
                    fnr = fnr,
                    tilleggstekst = tilleggstekst,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    forhåndsvisning = forhåndsvisning,
                    vedtaksdato = datoVedtak,
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
                    innsendingsdato = innsendingsdato,
                    clock = clock,
                )
            },
        ).map {
            Pair(it.first.pdf, it.second?.pdf)
        }.mapLeft {
            it.feil.loggFeil(log, "generering av forhåndsvisning av klagebrev", "SakId: ${kommando.sakId}, klagebehandlingId: ${kommando.klagebehandlingId}")
            KanIkkeForhåndsviseBrev.FeilMotPdfgen
        }
    }
}
