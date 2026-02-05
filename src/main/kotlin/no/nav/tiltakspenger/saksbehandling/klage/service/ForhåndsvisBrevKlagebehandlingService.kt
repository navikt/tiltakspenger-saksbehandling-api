package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KanIkkeForhåndsviseBrev
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.forhåndsvisKlagebrev
import no.nav.tiltakspenger.saksbehandling.klage.ports.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock
import java.time.LocalDate

class ForhåndsvisBrevKlagebehandlingService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
    private val genererKlagebrevKlient: GenererKlagebrevKlient,
) {
    suspend fun forhåndsvisBrev(
        kommando: KlagebehandlingBrevKommando,
    ): Either<KanIkkeForhåndsviseBrev, PdfA> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.forhåndsvisKlagebrev(
            kommando = kommando,
            genererAvvisningsbrev = suspend { saksnummer, fnr, saksbehandlerNavIdent, tilleggstekst, forhåndsvisning ->
                genererKlagebrevKlient.genererAvvisningsvedtak(
                    saksnummer = saksnummer,
                    fnr = fnr,
                    tilleggstekst = tilleggstekst,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    forhåndsvisning = forhåndsvisning,
                    vedtaksdato = LocalDate.now(clock),
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                )
            },
        ).map { it.pdf }.mapLeft {
            KanIkkeForhåndsviseBrev.FeilMotPdfgen
        }
    }
}
