package no.nav.tiltakspenger.saksbehandling.service.behandling.brev

import arrow.core.getOrElse
import no.nav.tiltakspenger.felles.NavIdentClient
import no.nav.tiltakspenger.felles.PdfA
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import java.time.LocalDate

class ForhåndsvisVedtaksbrevService(
    private val sakService: SakService,
    private val genererInnvilgelsesbrevClient: GenererInnvilgelsesvedtaksbrevGateway,
    private val genererStansbrevClient: GenererStansvedtaksbrevGateway,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
) {
    suspend fun forhåndsvisInnvilgelsesvedtaksbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA {
        // hentForSakId sjekker tilgang til person og sak.
        val sak = sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Fant ikke sak eller hadde ikke tilgang til sak. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }
        val behandling = sak.hentBehandling(kommando.behandlingId)!!
        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Saksbehandler har ikke tatt behandling. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }
        return when (behandling.behandlingstype) {
            Behandlingstype.FØRSTEGANGSBEHANDLING -> {
                genererInnvilgelsesbrevClient.genererInnvilgelsesvedtaksbrevMedTilleggstekst(
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                    vedtaksdato = LocalDate.now(),
                    tilleggstekst = kommando.fritekstTilVedtaksbrev,
                    fnr = sak.fnr,
                    saksbehandlerNavIdent = behandling.saksbehandler,
                    beslutterNavIdent = behandling.beslutter,
                    tiltaksnavn = behandling.tiltaksnavn,
                    innvilgelsesperiode = behandling.virkningsperiode!!,
                    saksnummer = sak.saksnummer,
                    sakId = sak.id,
                ).fold(
                    ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
                    ifRight = { it.pdf },
                )
            }

            Behandlingstype.REVURDERING -> {
                genererStansbrevClient.genererStansvedtak(
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                    vedtaksdato = LocalDate.now(),
                    fnr = sak.fnr,
                    saksbehandlerNavIdent = behandling.saksbehandler,
                    beslutterNavIdent = behandling.beslutter,
                    tiltaksnavn = behandling.tiltaksnavn,
                    stansperiode = behandling.virkningsperiode!!,
                    saksnummer = sak.saksnummer,
                    sakId = sak.id,
                ).fold(
                    ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
                    ifRight = { it.pdf },
                )
            }
        }
    }
}
