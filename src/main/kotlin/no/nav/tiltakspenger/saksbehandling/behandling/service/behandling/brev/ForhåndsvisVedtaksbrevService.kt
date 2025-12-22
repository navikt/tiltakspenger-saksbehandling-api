package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
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
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return when (val behandling: Rammebehandling = sak.hentRammebehandling(kommando.behandlingId)!!) {
            is Søknadsbehandling -> {
                when (val k = kommando as ForhåndsvisVedtaksbrevForSøknadsbehandlingKommando) {
                    is ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando -> genrererSøknadsbehandlingInnvilgelsesbrev(
                        kommando = k,
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperiode = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) k.innvilgelsesperioder.totalPeriode else behandling.innvilgelsesperioder!!.totalPeriode,
                    )

                    is ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando -> genererSøknadsbehandlingAvslagsbrev(
                        kommando = k,
                        sak = sak,
                        behandling = behandling,
                        avslagsperiode = behandling.søknad.tiltaksdeltakelseperiodeDetErSøktOm()!!,
                    )
                }
            }

            is Revurdering -> {
                when (val k = kommando as ForhåndsvisVedtaksbrevForRevurderingKommando) {
                    is ForhåndsvisVedtaksbrevForRevurderingStansKommando -> genererRevurderingStansbrev(
                        sak = sak,
                        kommando = k,
                        behandling = behandling,
                    )

                    is ForhåndsvisVedtaksbrevForRevurderingInnvilgelseKommando -> genererRevurderingInnvilgelsesbrev(
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperiode = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) k.innvilgelsesperioder.totalPeriode else behandling.innvilgelsesperioder!!.totalPeriode,
                        kommando = k,
                    )

                    is ForhåndsvisVedtaksbrevForRevurderingOmgjøringKommando -> genererRevurderingOmgjøringsbrev(
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperiode = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) k.innvilgelsesperioder.totalPeriode else behandling.innvilgelsesperioder!!.totalPeriode,
                        kommando = k,
                    )
                }
            }
        }
    }

    private suspend fun genererRevurderingInnvilgelsesbrev(
        sak: Sak,
        behandling: Revurdering,
        innvilgelsesperiode: Periode,
        kommando: ForhåndsvisVedtaksbrevForRevurderingInnvilgelseKommando,
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
        kommando: ForhåndsvisVedtaksbrevForRevurderingStansKommando,
        behandling: Revurdering,
    ): PdfA {
        val stansperiode = kommando.utledStansperiode(sak.førsteDagSomGirRett!!, sak.sisteDagSomGirRett!!)

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
            valgteHjemler = kommando.valgteHjemler,
            tilleggstekst = kommando.fritekstTilVedtaksbrev,

        ).fold(
            ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
            ifRight = { it.pdf },
        )
    }

    private suspend fun genererRevurderingOmgjøringsbrev(
        sak: Sak,
        behandling: Revurdering,
        innvilgelsesperiode: Periode,
        kommando: ForhåndsvisVedtaksbrevForRevurderingOmgjøringKommando,
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

    private suspend fun genererSøknadsbehandlingAvslagsbrev(
        kommando: ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando,
        sak: Sak,
        behandling: Søknadsbehandling,
        avslagsperiode: Periode,
    ): PdfA = genererVedtaksbrevForAvslagKlient.genererAvslagsVedtaksbrev(
        hentBrukersNavn = personService::hentNavn,
        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
        avslagsgrunner = kommando.avslagsgrunner,
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
        kommando: ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando,
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
