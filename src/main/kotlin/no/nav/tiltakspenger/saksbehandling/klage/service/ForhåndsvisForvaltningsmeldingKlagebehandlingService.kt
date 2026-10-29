package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.klage.domene.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KanIkkeForhåndsviseBrev
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.ForvaltningsmeldingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.forhåndsvisForvaltningsmeldingForKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.hentNavnForNavIdentEllerKast
import java.time.Clock

class ForhåndsvisForvaltningsmeldingKlagebehandlingService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
    private val genererKlagebrevKlient: GenererKlagebrevKlient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun forhåndsvis(
        kommando: ForvaltningsmeldingKommando,
    ): Either<KanIkkeForhåndsviseBrev, PdfA> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.forhåndsvisForvaltningsmeldingForKlagebehandling(
            kommando = kommando,
            generer = { saksnummer, fnr, saksbehandlerNavIdent, tilleggstekst, forhåndsvisning, innsendingsdato, brevdato ->
                genererKlagebrevKlient.genererForvaltningsmelding(
                    saksnummer = saksnummer,
                    fnr = fnr,
                    tilleggstekst = tilleggstekst,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    forhåndsvisning = forhåndsvisning,
                    innsendingsdato = innsendingsdato,
                    brevdato = brevdato,
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
                )
            },
            clock = clock,
        ).map {
            it.pdf
        }.mapLeft {
            it.feil.loggFeil(
                log,
                "generering av forhåndsvisning av forvaltningsmelding for klage",
                "SakId: ${kommando.sakId}, klagebehandlingId: ${kommando.klagebehandlingId}",
            )
            KanIkkeForhåndsviseBrev.FeilMotPdfgenrs
        }
    }
}
