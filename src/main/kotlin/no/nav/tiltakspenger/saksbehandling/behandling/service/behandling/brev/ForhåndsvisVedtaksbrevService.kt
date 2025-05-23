package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerStansDato
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererAvslagsvedtaksbrevGateway
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
    private val genererAvslagsvedtaksbrevGateway: GenererAvslagsvedtaksbrevGateway,
    private val genererStansbrevClient: GenererStansvedtaksbrevGateway,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
) {
    suspend fun forhåndsvisVedtaksbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA {
        // hentForSakId sjekker tilgang til person og sak.
        val sak = sakService.hentForSakIdEllerKast(kommando.sakId, kommando.saksbehandler, kommando.correlationId)
        val behandling = sak.hentBehandling(kommando.behandlingId)!!
        if (behandling.saksbehandler == null) {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Behandling har ingen saksbehandler. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }
        if (behandling.saksbehandler != kommando.saksbehandler.navIdent && behandling.beslutter != kommando.saksbehandler.navIdent) {
            throw IllegalStateException("Kunne ikke forhåndsvise vedtaksbrev. Saksbehandler har ikke tatt behandling, eller er ikke behandlingens beslutter. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }

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

        val utfall = kommando.utfall

        return when (behandling) {
            is Søknadsbehandling -> {
                when (utfall) {
                    SøknadsbehandlingType.INNVILGELSE -> genererInnvilgelsesbrevClient.genererInnvilgelsesvedtaksbrevMedTilleggstekst(
                        hentBrukersNavn = personService::hentNavn,
                        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                        vedtaksdato = LocalDate.now(),
                        tilleggstekst = kommando.fritekstTilVedtaksbrev,
                        fnr = sak.fnr,
                        saksbehandlerNavIdent = behandling.saksbehandler!!,
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

                    SøknadsbehandlingType.AVSLAG -> genererAvslagsvedtaksbrevGateway.genererAvslagsVedtaksbrev(
                        hentBrukersNavn = personService::hentNavn,
                        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                        avslagsgrunner = kommando.avslagsgrunner!!,
                        fnr = sak.fnr,
                        saksbehandlerNavIdent = behandling.saksbehandler!!,
                        beslutterNavIdent = behandling.beslutter,
                        avslagsperiode = virkingsperiode!!,
                        saksnummer = sak.saksnummer,
                        sakId = sak.id,
                        tilleggstekst = kommando.fritekstTilVedtaksbrev,
                        forhåndsvisning = true,
                        harSøktBarnetillegg = behandling.søknad.barnetillegg.isNotEmpty(),
                        datoForUtsending = LocalDate.now(),
                    ).fold(
                        ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
                        ifRight = { it.pdf },
                    )

                    RevurderingType.INNVILGELSE,
                    RevurderingType.STANS,
                    -> throw IllegalArgumentException("$utfall er ikke gyldig utfall for søknadsbehandling")
                }
            }

            is Revurdering -> {
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
                    saksbehandlerNavIdent = behandling.saksbehandler!!,
                    beslutterNavIdent = behandling.beslutter,
                    virkningsperiode = stansePeriode!!,
                    saksnummer = sak.saksnummer,
                    sakId = sak.id,
                    forhåndsvisning = true,
                    barnetillegg = behandling.barnetillegg != null,
                    valgtHjemmelHarIkkeRettighet = kommando.valgteHjemler,
                    tilleggstekst = kommando.fritekstTilVedtaksbrev,
                ).fold(
                    ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
                    ifRight = { it.pdf },
                )
            }
        }
    }
}
