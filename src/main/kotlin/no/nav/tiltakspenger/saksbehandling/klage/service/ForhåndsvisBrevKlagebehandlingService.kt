package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.klage.domene.ForhåndsvisBrevKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeForhåndsviseBrev
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
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
        kommando: ForhåndsvisBrevKlagebehandlingKommando,
    ): Either<KanIkkeForhåndsviseBrev, PdfA> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val klagebehandling: Klagebehandling = sak.hentKlagebehandling(kommando.klagebehandlingId)
        require(klagebehandling.erAvvisning) {
            "Kan kun forhåndsvise avvisningsvedtak i første versjon. sakId =${kommando.sakId}, klagebehandlingId=${kommando.klagebehandlingId}"
        }
        return genererAvvisningsvedtak(
            kommando = kommando,
            klagebehandling = klagebehandling,
        )
    }

    private suspend fun genererAvvisningsvedtak(
        kommando: ForhåndsvisBrevKlagebehandlingKommando,
        klagebehandling: Klagebehandling,
    ): Either<KanIkkeForhåndsviseBrev, PdfA> = genererKlagebrevKlient.genererAvvisningsvedtak(
        hentBrukersNavn = personService::hentNavn,
        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
        vedtaksdato = LocalDate.now(clock),
        klagebehandling = klagebehandling,
        kommando = kommando,
    ).map {
        it.pdf
    }.mapLeft {
        KanIkkeForhåndsviseBrev.FeilMotPdfgen
    }
}
