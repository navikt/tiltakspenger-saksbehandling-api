package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.infra.toAntallDagerTekst
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
        val sak = sakService.hentForSakId(kommando.sakId)
        val behandling = sak.hentRammebehandling(kommando.behandlingId)!!
        val vedtaksperiode = when (behandling.status) {
            Rammebehandlingsstatus.UNDER_BEHANDLING -> when (kommando.resultat) {
                RevurderingType.STANS,
                RevurderingType.INNVILGELSE,
                RevurderingType.OMGJØRING,
                SøknadsbehandlingType.INNVILGELSE,
                -> kommando.vedtaksperiode

                SøknadsbehandlingType.AVSLAG -> (behandling as Søknadsbehandling).søknad.tiltaksdeltakelseperiodeDetErSøktOm()
            }

            Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
            Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
            Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
            Rammebehandlingsstatus.UNDER_BESLUTNING,
            Rammebehandlingsstatus.VEDTATT,
            Rammebehandlingsstatus.AVBRUTT,
            -> behandling.vedtaksperiode!!
        }
        val resultat = kommando.resultat

        return when (behandling) {
            is Søknadsbehandling -> {
                when (resultat) {
                    SøknadsbehandlingType.INNVILGELSE -> genrererSøknadsbehandlingInnvilgelsesbrev(
                        kommando = kommando,
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperiode = vedtaksperiode!!,
                    )

                    SøknadsbehandlingType.AVSLAG -> genererSøknadsbehandlingAvslagsbrev(
                        kommando = kommando,
                        sak = sak,
                        behandling = behandling,
                        avslagsperiode = vedtaksperiode!!,
                    )

                    is RevurderingType -> throw IllegalArgumentException("$resultat er ikke gyldig resultat for søknadsbehandling")
                }
            }

            is Revurdering -> {
                when (resultat) {
                    RevurderingType.STANS -> genererRevurderingStansbrev(sak, kommando, behandling)
                    // Kommentar jah: Første iterasjon av omgjøring vil kun endre innvilgelsesperioden, så vi kan gjenbruke innvilgelsesbrevet.
                    RevurderingType.INNVILGELSE -> genererRevurderingInnvilgelsesbrev(
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperiode = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) kommando.vedtaksperiode!! else behandling.innvilgelsesperioder!!.totalPeriode,
                        kommando = kommando,
                    )

                    // TODO Man treffer ikke denne branchen ved omgjøring per 11.11.2025 da frontend sender feil type ved omgjøring
                    RevurderingType.OMGJØRING -> genererRevurderingInnvilgelsesbrev(
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperiode = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) kommando.vedtaksperiode!! else behandling.innvilgelsesperioder!!.totalPeriode,
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
        innvilgelsesperiode: Periode,
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA = genererInnvilgelsesbrevClient.genererInnvilgetRevurderingBrev(
        hentBrukersNavn = personService::hentNavn,
        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
        vedtaksdato = LocalDate.now(),
        fnr = sak.fnr,
        saksbehandlerNavIdent = behandling.saksbehandler!!,
        beslutterNavIdent = behandling.beslutter,
        saksnummer = sak.saksnummer,
        sakId = sak.id,
        forhåndsvisning = true,
        innvilgelsesperiode = innvilgelsesperiode,
        tilleggstekst = kommando.fritekstTilVedtaksbrev,
        barnetillegg = kommando.barnetillegg?.let {
            it.utvid(AntallBarn(0), innvilgelsesperiode) as SammenhengendePeriodisering
        },
        antallDagerTekst = toAntallDagerTekst(kommando.antallDagerPerMeldeperiode),
    ).fold(
        ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
        ifRight = { it.pdf },
    )

    private suspend fun genererRevurderingStansbrev(
        sak: Sak,
        kommando: ForhåndsvisVedtaksbrevKommando,
        behandling: Revurdering,
    ): PdfA {
        val stansperiode = kommando.hentStansperiode(sak.førsteDagSomGirRett!!, sak.sisteDagSomGirRett!!)

        @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
        return genererStansbrevClient.genererStansvedtak(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            stansperiode = stansperiode,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            forhåndsvisning = true,
            valgteHjemler = kommando.valgteHjemler as List<ValgtHjemmelForStans>,
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
        avslagsperiode: Periode,
    ): PdfA = genererVedtaksbrevForAvslagKlient.genererAvslagsVedtaksbrev(
        hentBrukersNavn = personService::hentNavn,
        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
        avslagsgrunner = kommando.avslagsgrunner!!,
        fnr = sak.fnr,
        saksbehandlerNavIdent = behandling.saksbehandler!!,
        beslutterNavIdent = behandling.beslutter,
        avslagsperiode = avslagsperiode,
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
        innvilgelsesperiode: Periode,
    ): PdfA = genererInnvilgelsesbrevClient.genererInnvilgelsesvedtaksbrevMedTilleggstekst(
        hentBrukersNavn = personService::hentNavn,
        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
        vedtaksdato = LocalDate.now(),
        tilleggstekst = kommando.fritekstTilVedtaksbrev,
        fnr = sak.fnr,
        saksbehandlerNavIdent = behandling.saksbehandler!!,
        beslutterNavIdent = behandling.beslutter,
        innvilgelsesperiode = innvilgelsesperiode,
        saksnummer = sak.saksnummer,
        sakId = sak.id,
        forhåndsvisning = true,
        barnetilleggsPerioder = kommando.barnetillegg?.let {
            it.utvid(AntallBarn(0), innvilgelsesperiode) as SammenhengendePeriodisering
        },
        antallDagerTekst = toAntallDagerTekst(kommando.antallDagerPerMeldeperiode),
    ).fold(
        ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
        ifRight = { it.pdf },
    )
}
