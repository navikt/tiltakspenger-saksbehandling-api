package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForOpphørKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.barnSomFyller16
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.utledForlengelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilInnvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.hentNavnForNavIdentEllerKast
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
    private val log = KotlinLogging.logger {}

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

                    is ForhåndsvisVedtaksbrevForForlengelseKommando -> genererForlengelseBrev(
                        sak = sak,
                        behandling = behandling,
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
            ifLeft = {
                // TODO jah: Unngå dobbel logging.
                it.feil.loggFeil(log, "generering av forhåndsvisning av vedtaksbrev", "SakId: ${kommando.sakId}, behandlingId: ${kommando.behandlingId}")
                throw IllegalStateException("Kunne ikke generere vedtaksbrev. Underliggende feil: $it")
            },
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
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
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

    /**
     * Under behandling utledes perioden og barnetillegget fra skjemaet, som ved oppdatering av forlengelsen.
     * Ellers brukes verdiene som er lagret på behandlingen.
     */
    private suspend fun genererForlengelseBrev(
        sak: Sak,
        behandling: Revurdering,
        kommando: ForhåndsvisVedtaksbrevForForlengelseKommando,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        val resultat = behandling.resultat
        require(resultat is Revurderingsresultat.Forlengelse) {
            "Behandlingen ${behandling.id} er ikke en forlengelse, men ${resultat?.javaClass?.simpleName}"
        }
        val forlengetVedtak = sak.hentRammevedtakForId(resultat.forlenger.rammevedtakId)
        val (innvilgelsesperioder, barnetillegg, barnSomFyller16) = when (behandling.status) {
            Rammebehandlingsstatus.UNDER_BEHANDLING -> {
                val utledet = sak.utledForlengelse(
                    revurdering = behandling,
                    tilOgMed = kommando.tilOgMed,
                    barnetillegg = kommando.barnetillegg,
                ).getOrElse {
                    throw IllegalArgumentException("Kan ikke forhåndsvise vedtaksbrev for forlengelsen ${behandling.id}: $it")
                }
                Triple(
                    utledet.innvilgelsesperioder.tilInnvilgelsesperioder(behandling),
                    utledet.barnetillegg,
                    when {
                        kommando.barnetillegg != null || utledet.kanIkkeUtledeBarnetillegg -> null
                        else -> resultat.forlenger.barnSomFyller16(forlengetVedtak, utledet.barnetillegg.periodisering)
                    },
                )
            }

            Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
            Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
            Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
            Rammebehandlingsstatus.UNDER_BESLUTNING,
            Rammebehandlingsstatus.VEDTATT,
            Rammebehandlingsstatus.AVBRUTT,
            -> Triple(behandling.innvilgelsesperioder!!, behandling.barnetillegg, resultat.barnSomFyller16(forlengetVedtak))
        }

        return genererInnvilgelsesbrevClient.genererForlengelseBrevForhåndsvisning(
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
            vedtaksdato = LocalDate.now(clock),
            fnr = sak.fnr,
            saksbehandlerNavIdent = behandling.saksbehandler!!,
            beslutterNavIdent = behandling.beslutter,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetilleggsperioder = barnetillegg?.periodisering,
            barnSomFyller16 = barnSomFyller16,
            tilleggstekst = kommando.fritekstTilVedtaksbrev,
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
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
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
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
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
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
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
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
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
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
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
