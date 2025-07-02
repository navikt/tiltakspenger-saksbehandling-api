package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerStansDato
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.LocalDate

class ForhåndsvisVedtaksbrevService(
    private val sakService: SakService,
    private val genererInnvilgelsesbrevClient: GenererVedtaksbrevForInnvilgelseKlient,
    private val genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient,
    private val genererStansbrevClient: GenererVedtaksbrevForStansKlient,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
) {
    suspend fun forhåndsvisVedtaksbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA {
        // hentForSakId sjekker tilgang til person og sak.
        val sak = sakService.sjekkTilgangOgHentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId)
        val behandling = sak.hentBehandling(kommando.behandlingId)!!
        val virkingsperiode = when (behandling.status) {
            Behandlingsstatus.KLAR_TIL_BEHANDLING,
            Behandlingsstatus.UNDER_BEHANDLING,
            // gir det mening at man har lyst til å se innvilgelsesbrevet hvis behandlingen er avbrutt eller til automatisk behandling?
            Behandlingsstatus.AVBRUTT,
            Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
            -> kommando.virkningsperiode ?: behandling.virkningsperiode

            Behandlingsstatus.KLAR_TIL_BESLUTNING,
            Behandlingsstatus.UNDER_BESLUTNING,
            Behandlingsstatus.VEDTATT,
            -> behandling.virkningsperiode!!
        }
        val resultat = kommando.resultat

        return when (behandling) {
            is Søknadsbehandling -> {
                when (resultat) {
                    SøknadsbehandlingType.INNVILGELSE -> genrererSøknadsbehandlingInnvilgelsesbrev(
                        kommando = kommando,
                        sak = sak,
                        behandling = behandling,
                        virkingsperiode = virkingsperiode,
                    )

                    SøknadsbehandlingType.AVSLAG -> genererSøknadsbehandlingAvslagsbrev(
                        kommando = kommando,
                        sak = sak,
                        behandling = behandling,
                        virkingsperiode = virkingsperiode,
                    )

                    is RevurderingType -> throw IllegalArgumentException("$resultat er ikke gyldig resultat for søknadsbehandling")
                }
            }

            is Revurdering -> {
                when (resultat) {
                    RevurderingType.STANS -> genererRevurderingStansbrev(sak, kommando, behandling)
                    RevurderingType.INNVILGELSE -> genererRevurderingInnvilgelsesbrev(
                        sak = sak,
                        behandling = behandling,
                        virkingsperiode = virkingsperiode,
                        kommando = kommando,
                    )

                    is SøknadsbehandlingType -> throw IllegalArgumentException("$resultat er ikke gyldig resultat for revurdering")
                }
            }
        }
    }

    private suspend fun genererRevurderingInnvilgelsesbrev(
        sak: Sak,
        behandling: Revurdering,
        virkingsperiode: Periode?,
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA = genererInnvilgelsesbrevClient.genererInnvilgetRevurderingBrev(
        hentBrukersNavn = personService::hentNavn,
        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
        fnr = sak.fnr,
        saksbehandlerNavIdent = behandling.saksbehandler!!,
        beslutterNavIdent = behandling.beslutter,
        saksnummer = sak.saksnummer,
        sakId = sak.id,
        forhåndsvisning = true,
        vurderingsperiode = virkingsperiode!!,
        saksbehandlersVurdering = kommando.fritekstTilVedtaksbrev,
        barnetillegg = kommando.barnetillegg?.let {
            it.utvid(AntallBarn(0), virkingsperiode) as SammenhengendePeriodisering
        },
    ).fold(
        ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
        ifRight = { it.pdf },
    )

    private suspend fun genererRevurderingStansbrev(
        sak: Sak,
        kommando: ForhåndsvisVedtaksbrevKommando,
        behandling: Revurdering,
    ): PdfA {
        sak.validerStansDato(kommando.stansDato)
        val stansperiode = kommando.stansDato?.let { stansDato ->
            sak.vedtaksliste.sisteDagSomGirRett?.let { sisteDagSomGirRett ->
                Periode(stansDato, sisteDagSomGirRett)
            }
        }

        return genererStansbrevClient.genererStansvedtak(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            virkningsperiode = stansperiode!!,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            forhåndsvisning = true,
            barnetillegg = behandling.barnetillegg != null,
            valgtHjemmelHarIkkeRettighet = kommando.valgteHjemler!!,
            tilleggstekst = kommando.fritekstTilVedtaksbrev,
        ).fold(
            ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
            ifRight = { it.pdf },
        )
    }

    private suspend fun genererSøknadsbehandlingAvslagsbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
        sak: Sak,
        behandling: Søknadsbehandling,
        virkingsperiode: Periode?,
    ): PdfA = genererVedtaksbrevForAvslagKlient.genererAvslagsVedtaksbrev(
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

    private suspend fun genrererSøknadsbehandlingInnvilgelsesbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
        sak: Sak,
        behandling: Søknadsbehandling,
        virkingsperiode: Periode?,
    ): PdfA = genererInnvilgelsesbrevClient.genererInnvilgelsesvedtaksbrevMedTilleggstekst(
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
        barnetilleggsPerioder = kommando.barnetillegg?.let {
            it.utvid(AntallBarn(0), virkingsperiode) as SammenhengendePeriodisering
        },
    ).fold(
        ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
        ifRight = { it.pdf },
    )
}
