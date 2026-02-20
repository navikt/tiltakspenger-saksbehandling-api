package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.Either
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilInnvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForOpphørKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock
import java.time.LocalDate

class ForhåndsvisRammevedtaksbrevService(
    private val sakService: SakService,
    private val genererInnvilgelsesbrevClient: GenererVedtaksbrevForInnvilgelseKlient,
    private val genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient,
    private val genererStansbrevClient: GenererVedtaksbrevForStansKlient,
    private val genererOpphørbrevKlient: GenererVedtaksbrevForOpphørKlient,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
) {
    suspend fun forhåndsvisVedtaksbrev(
        kommando: ForhåndsvisVedtaksbrevKommando,
    ): PdfA {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val behandling: Rammebehandling = sak.hentRammebehandling(kommando.behandlingId)!!

        return when (behandling) {
            is Søknadsbehandling -> {
                when (val k = kommando as ForhåndsvisVedtaksbrevForSøknadsbehandlingKommando) {
                    is ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando -> genrererSøknadsbehandlingInnvilgelsesbrev(
                        kommando = k,
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperioder = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) {
                            k.innvilgelsesperioder.tilInnvilgelsesperioder(
                                behandling,
                            )
                        } else {
                            behandling.innvilgelsesperioder!!
                        },
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
                        innvilgelsesperioder = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) {
                            k.innvilgelsesperioder.tilInnvilgelsesperioder(
                                behandling,
                            )
                        } else {
                            behandling.innvilgelsesperioder!!
                        },
                        kommando = k,
                    )

                    is ForhåndsvisVedtaksbrevForOmgjøringInnvilgelseKommando -> genererOmgjøringInnvilgelseBrev(
                        sak = sak,
                        behandling = behandling,
                        innvilgelsesperioder = if (behandling.status == Rammebehandlingsstatus.UNDER_BEHANDLING) {
                            k.innvilgelsesperioder.tilInnvilgelsesperioder(
                                behandling,
                            )
                        } else {
                            behandling.innvilgelsesperioder!!
                        },
                        kommando = k,
                    )

                    is ForhåndsvisVedtaksbrevForOmgjøringOpphørKommando -> genererOmgjøringOpphørBrev(
                        sak = sak,
                        behandling = behandling,
                        kommando = k,
                    )
                }
            }
        }.fold(
            ifLeft = { throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it") },
            ifRight = { it.pdf },
        )
    }

    private suspend fun genererRevurderingInnvilgelsesbrev(
        sak: Sak,
        behandling: Revurdering,
        innvilgelsesperioder: Innvilgelsesperioder,
        kommando: ForhåndsvisVedtaksbrevForRevurderingInnvilgelseKommando,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return genererInnvilgelsesbrevClient.genererInnvilgetRevurderingBrevForhåndsvisning(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(clock),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            innvilgelsesperioder = innvilgelsesperioder,
            tilleggstekst = kommando.fritekstTilVedtaksbrev,
            barnetilleggsperioder = kommando.barnetillegg,
        )
    }

    private suspend fun genererRevurderingStansbrev(
        sak: Sak,
        kommando: ForhåndsvisVedtaksbrevForRevurderingStansKommando,
        behandling: Revurdering,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        val (stansperiode, valgteHjemler, tilleggstekst) = when (behandling.status) {
            Rammebehandlingsstatus.UNDER_BEHANDLING -> Triple(
                kommando.utledStansperiode(
                    sak.førsteDagSomGirRett!!,
                    sak.sisteDagSomGirRett!!,
                ),
                kommando.valgteHjemler,
                kommando.fritekstTilVedtaksbrev,
            )

            Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
            Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
            Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
            Rammebehandlingsstatus.UNDER_BESLUTNING,
            Rammebehandlingsstatus.VEDTATT,
            Rammebehandlingsstatus.AVBRUTT,
            -> {
                val resultat = behandling.resultat as Revurderingsresultat.Stans
                Triple(resultat.stansperiode!!, resultat.valgtHjemmel!!, behandling.fritekstTilVedtaksbrev)
            }
        }

        return genererStansbrevClient.genererStansBrevForhåndsvisning(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(clock),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            stansperiode = stansperiode,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            valgteHjemler = valgteHjemler,
            tilleggstekst = tilleggstekst,
            harStansetBarnetillegg = sak.harBarnetillegg(stansperiode),
        )
    }

    private suspend fun genererOmgjøringInnvilgelseBrev(
        sak: Sak,
        behandling: Revurdering,
        innvilgelsesperioder: Innvilgelsesperioder,
        kommando: ForhåndsvisVedtaksbrevForOmgjøringInnvilgelseKommando,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return genererInnvilgelsesbrevClient.genererInnvilgetRevurderingBrevForhåndsvisning(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(clock),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            tilleggstekst = kommando.fritekstTilVedtaksbrev,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetilleggsperioder = kommando.barnetillegg,
        )
    }

    private suspend fun genererOmgjøringOpphørBrev(
        sak: Sak,
        behandling: Revurdering,
        kommando: ForhåndsvisVedtaksbrevForOmgjøringOpphørKommando,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return genererOpphørbrevKlient.genererOpphørBrevForhåndsvisning(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(clock),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            tilleggstekst = kommando.fritekstTilVedtaksbrev,
            valgteHjemler = kommando.valgteHjemler,
            vedtaksperiode = kommando.vedtaksperiode,
            harOpphørtBarnetillegg = sak.harBarnetillegg(kommando.vedtaksperiode),
        )
    }

    private suspend fun genererSøknadsbehandlingAvslagsbrev(
        kommando: ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando,
        sak: Sak,
        behandling: Søknadsbehandling,
        avslagsperiode: Periode,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return genererVedtaksbrevForAvslagKlient.genererAvslagsVedtaksbrev(
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
            datoForUtsending = LocalDate.now(clock),
        )
    }

    private suspend fun genrererSøknadsbehandlingInnvilgelsesbrev(
        kommando: ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando,
        sak: Sak,
        behandling: Søknadsbehandling,
        innvilgelsesperioder: Innvilgelsesperioder,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return genererInnvilgelsesbrevClient.genererInnvilgetSøknadBrevForhåndsvisning(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
            vedtaksdato = LocalDate.now(clock),
            tilleggstekst = kommando.fritekstTilVedtaksbrev,
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetilleggsperioder = kommando.barnetillegg,
        )
    }

    private fun Sak.harBarnetillegg(periode: Periode): Boolean {
        return barnetilleggsperioder.overlappendePeriode(periode).any { it.verdi != AntallBarn.ZERO }
    }
}
