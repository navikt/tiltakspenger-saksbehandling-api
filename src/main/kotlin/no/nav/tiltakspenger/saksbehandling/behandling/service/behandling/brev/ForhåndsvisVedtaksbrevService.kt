package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerStansDato
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.LocalDate

class ForhåndsvisVedtaksbrevService(
    private val sakService: SakService,
    private val genererInnvilgelsesbrevClient: GenererInnvilgelsesvedtaksbrevGateway,
    private val genererStansbrevClient: GenererStansvedtaksbrevGateway,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
) {
    suspend fun forhåndsvisVedtaksbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA {
        // hentForSakId sjekker tilgang til person og sak.
        val sak = sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Fant ikke sak eller hadde ikke tilgang til sak. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }
        val behandling = sak.hentBehandling(kommando.behandlingId)!!
        if (behandling.saksbehandler == null) {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Behandling har ingen saksbehandler. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }
        if (behandling.saksbehandler != kommando.saksbehandler.navIdent && behandling.beslutter != kommando.saksbehandler.navIdent) {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Saksbehandler har ikke tatt behandling, eller er ikke behandlingens beslutter. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }

        // TODO - må ignorere perioden hvis vedtaket er avslag.
        val virkingsperiode = when (behandling.status) {
            Behandlingsstatus.KLAR_TIL_BEHANDLING,
            Behandlingsstatus.UNDER_BEHANDLING,
            // gir det mening at man har lyst til å se innvilgelsesbrevet hvis behandlingen er avbrutt?
            Behandlingsstatus.AVBRUTT,
            -> kommando.virkingsperiode ?: behandling.virkningsperiode

            Behandlingsstatus.KLAR_TIL_BESLUTNING,
            Behandlingsstatus.UNDER_BESLUTNING,
            Behandlingsstatus.VEDTATT,
            -> behandling.virkningsperiode!!
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
                    innvilgelsesperiode = virkingsperiode!!,
                    saksnummer = sak.saksnummer,
                    sakId = sak.id,
                    forhåndsvisning = true,
                    barnetilleggsPerioder = kommando.barnetillegg?.let { if (it.isEmpty()) null else it },
                ).fold(
                    ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
                    ifRight = { it.pdf },
                )
            }

            Behandlingstype.REVURDERING -> {
                sak.validerStansDato(kommando.stansDato)
                val stansePeriode = kommando.stansDato?.let { stansDato ->
                    sak.vedtaksliste.sisteDagSomGirRett?.let { sisteDagSomGirRett ->
                        Periode(stansDato, sisteDagSomGirRett)
                    }
                }

                genererStansbrevClient.genererStansvedtak(
                    hentBrukersNavn = personService::hentNavn,
                    hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                    vedtaksdato = LocalDate.now(),
                    fnr = sak.fnr,
                    saksbehandlerNavIdent = behandling.saksbehandler,
                    beslutterNavIdent = behandling.beslutter,
                    virkningsperiode = stansePeriode!!,
                    saksnummer = sak.saksnummer,
                    sakId = sak.id,
                    forhåndsvisning = true,
                    barnetillegg = behandling.barnetillegg != null,
                    valgtHjemmelHarIkkeRettighet = kommando.toValgtHjemmelHarIkkeRettighet(),
                    tilleggstekst = kommando.fritekstTilVedtaksbrev,
                ).fold(
                    ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
                    ifRight = { it.pdf },
                )
            }
        }
    }
}
