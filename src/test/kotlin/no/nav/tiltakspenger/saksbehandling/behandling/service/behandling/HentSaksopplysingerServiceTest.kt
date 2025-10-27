package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.toNonEmptyListOrThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelseDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class HentSaksopplysingerServiceTest {

    @Test
    fun `en tiltaksdeltagelse ingen ytelser`() {
        return runBlocking {
            val fnr = Fnr.random()
            val correlationId = CorrelationId.generate()
            // Lar bruker være uten tiltak første og siste dag i perioden.
            val periode: Periode = 2.januar(2023) til 30.januar(2023)
            val clock = Clock.fixed(1.februar(2023).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            val tiltaksdeltagelser = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode.fraOgMed,
                søknadTilOgMed = periode.tilOgMed,
            )
            val tiltaksdeltagelserDetErSøktTiltakspengerFor = TiltaksdeltagelserDetErSøktTiltakspengerFor(
                tiltaksdeltagelser.second,
                periode.fraOgMed.atStartOfDay(),
            )
            val aktuelleTiltaksdeltagelserForBehandlingen = listOf(tiltaksdeltagelser.first.eksternDeltagelseId)

            val tiltaksdeltagelseKlient = object : TiltaksdeltagelseKlient {
                override suspend fun hentTiltaksdeltagelser(
                    fnr: Fnr,
                    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = Tiltaksdeltagelser(tiltaksdeltagelser.first)

                override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
                    fnr: Fnr,
                    harAdressebeskyttelse: Boolean,
                    correlationId: CorrelationId,
                ): List<TiltaksdeltakelseMedArrangørnavn> {
                    return emptyList()
                }
            }
            val sokosUtbetaldataClient = object : SokosUtbetaldataClient {
                override suspend fun hentYtelserFraUtbetaldata(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<Ytelse>()
            }
            val tiltakspengerArenaClient = object : TiltakspengerArenaClient {
                override suspend fun hentTiltakspengevedtakFraArena(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<ArenaTPVedtak>()
            }

            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
            )

            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltagelserForBehandlingen = aktuelleTiltaksdeltagelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = false,
            )

            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltagelser shouldBeEqual Tiltaksdeltagelser(tiltaksdeltagelser.first)
            result.periode!! shouldBeEqual (2 til 30.januar(2023))
            result.ytelser shouldBeEqual Ytelser.IngenTreff(
                oppslagsperiode = 1.desember(2022) til 31.januar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
            result.tiltakspengevedtakFraArena shouldBeEqual TiltakspengevedtakFraArena.IngenTreff(
                oppslagsperiode = 2 til 30.januar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
        }
    }

    @Test
    fun `mangler fraOgMed og tilOgMed i registre`() {
        return runBlocking {
            val fnr = Fnr.random()
            val correlationId = CorrelationId.generate()
            // Lar bruker være uten tiltak første og siste dag i perioden.
            val periode: Periode = 2.januar(2023) til 30.januar(2023)
            val clock = Clock.fixed(1.februar(2023).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            val tiltaksdeltagelser = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode.fraOgMed,
                søknadTilOgMed = periode.tilOgMed,
                registerFraOgMed = null,
                registerTilOgMed = null,
            )
            val tiltaksdeltagelserDetErSøktTiltakspengerFor = TiltaksdeltagelserDetErSøktTiltakspengerFor(
                tiltaksdeltagelser.second,
                periode.fraOgMed.atStartOfDay(),
            )
            val aktuelleTiltaksdeltagelserForBehandlingen = listOf(tiltaksdeltagelser.first.eksternDeltagelseId)

            val tiltaksdeltagelseKlient = object : TiltaksdeltagelseKlient {
                override suspend fun hentTiltaksdeltagelser(
                    fnr: Fnr,
                    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = Tiltaksdeltagelser(tiltaksdeltagelser.first)

                override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
                    fnr: Fnr,
                    harAdressebeskyttelse: Boolean,
                    correlationId: CorrelationId,
                ): List<TiltaksdeltakelseMedArrangørnavn> {
                    return emptyList()
                }
            }
            val sokosUtbetaldataClient = object : SokosUtbetaldataClient {
                override suspend fun hentYtelserFraUtbetaldata(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<Ytelse>()
            }
            val tiltakspengerArenaClient = object : TiltakspengerArenaClient {
                override suspend fun hentTiltakspengevedtakFraArena(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<ArenaTPVedtak>()
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltagelserForBehandlingen = aktuelleTiltaksdeltagelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = true,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltagelser shouldBeEqual Tiltaksdeltagelser(tiltaksdeltagelser.first)
            result.periode.shouldBeNull()
            result.ytelser shouldBeEqual Ytelser.IkkeBehandlingsgrunnlag
            result.tiltakspengevedtakFraArena shouldBeEqual TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag
        }
    }

    @Test
    fun `søknadsperiode er en subperiode av registerperiode`() {
        return runBlocking {
            val fnr = Fnr.random()
            val correlationId = CorrelationId.generate()
            // Lar bruker være uten tiltak første og siste dag i perioden.
            val søknadsperiode: Periode = 3.januar(2023) til 29.januar(2023)
            val registerperiode: Periode = 2.januar(2023) til 30.januar(2023)
            val clock = Clock.fixed(1.februar(2023).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            val tiltaksdeltagelser = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = søknadsperiode.fraOgMed,
                søknadTilOgMed = søknadsperiode.tilOgMed,
                registerFraOgMed = registerperiode.fraOgMed,
                registerTilOgMed = registerperiode.tilOgMed,
            )
            val tiltaksdeltagelserDetErSøktTiltakspengerFor = TiltaksdeltagelserDetErSøktTiltakspengerFor(
                tiltaksdeltagelser.second,
                søknadsperiode.fraOgMed.atStartOfDay(),
            )
            val aktuelleTiltaksdeltagelserForBehandlingen = listOf(tiltaksdeltagelser.first.eksternDeltagelseId)

            val tiltaksdeltagelseKlient = object : TiltaksdeltagelseKlient {
                override suspend fun hentTiltaksdeltagelser(
                    fnr: Fnr,
                    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = Tiltaksdeltagelser(tiltaksdeltagelser.first)

                override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
                    fnr: Fnr,
                    harAdressebeskyttelse: Boolean,
                    correlationId: CorrelationId,
                ): List<TiltaksdeltakelseMedArrangørnavn> {
                    return emptyList()
                }
            }
            val sokosUtbetaldataClient = object : SokosUtbetaldataClient {
                override suspend fun hentYtelserFraUtbetaldata(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<Ytelse>()
            }
            val tiltakspengerArenaClient = object : TiltakspengerArenaClient {
                override suspend fun hentTiltakspengevedtakFraArena(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = listOf(
                    ArenaTPVedtak(
                        fraOgMed = 12.desember(2022),
                        tilOgMed = 15.januar(2023),
                        rettighet = ArenaTPVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = 123L,
                    ),
                )
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltagelserForBehandlingen = aktuelleTiltaksdeltagelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = true,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltagelser shouldBeEqual Tiltaksdeltagelser(tiltaksdeltagelser.first)
            result.periode!! shouldBeEqual (2.januar(2023) til 30.januar(2023))
            result.ytelser shouldBeEqual Ytelser.IngenTreff(
                oppslagsperiode = 1.desember(2022) til 31.januar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
            result.tiltakspengevedtakFraArena shouldBeEqual TiltakspengevedtakFraArena.Treff(
                value = listOf(
                    ArenaTPVedtak(
                        fraOgMed = 12.desember(2022),
                        tilOgMed = 15.januar(2023),
                        rettighet = ArenaTPVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = 123L,
                    ),
                ).toNonEmptyListOrThrow(),
                oppslagsperiode = 2 til 30.januar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
        }
    }

    @Test
    fun `2 tiltak med overlapp`() {
        return runBlocking {
            val fnr = Fnr.random()
            val correlationId = CorrelationId.generate()
            // Lar bruker være uten tiltak første og siste dag i perioden.
            val periode1: Periode = 3.januar(2023) til 29.januar(2023)
            val periode2: Periode = 26.januar(2023) til 15.februar(2023)
            val clock = Clock.fixed(1.februar(2023).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            val tiltak1 = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode1.fraOgMed,
                søknadTilOgMed = periode1.tilOgMed,
            )
            val tiltak2 = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode2.fraOgMed,
                søknadTilOgMed = periode2.tilOgMed,
            )
            val tiltaksdeltagelserDetErSøktTiltakspengerFor = TiltaksdeltagelserDetErSøktTiltakspengerFor(
                listOf(
                    TiltaksdeltagelseDetErSøktTiltakspengerFor(tiltak1.second, periode1.fraOgMed.atStartOfDay()),
                    TiltaksdeltagelseDetErSøktTiltakspengerFor(tiltak2.second, periode2.fraOgMed.atStartOfDay()),
                ),
            )
            // Simulerer at vi søknadsbehandler tiltak1. Tiltak2 overlapper og algoritmen vil da hente begge tiltakene.
            val aktuelleTiltaksdeltagelserForBehandlingen = listOf(
                tiltak1.first.eksternDeltagelseId,
            )

            val tiltaksdeltagelseKlient = object : TiltaksdeltagelseKlient {
                override suspend fun hentTiltaksdeltagelser(
                    fnr: Fnr,
                    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = Tiltaksdeltagelser(listOf(tiltak1.first, tiltak2.first))

                override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
                    fnr: Fnr,
                    harAdressebeskyttelse: Boolean,
                    correlationId: CorrelationId,
                ): List<TiltaksdeltakelseMedArrangørnavn> {
                    return emptyList()
                }
            }
            val sokosUtbetaldataClient = object : SokosUtbetaldataClient {
                override suspend fun hentYtelserFraUtbetaldata(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<Ytelse>()
            }
            val tiltakspengerArenaClient = object : TiltakspengerArenaClient {
                override suspend fun hentTiltakspengevedtakFraArena(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<ArenaTPVedtak>()
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltagelserForBehandlingen = aktuelleTiltaksdeltagelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = true,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltagelser shouldBeEqual Tiltaksdeltagelser(listOf(tiltak1.first, tiltak2.first))
            result.periode!! shouldBeEqual (3.januar(2023) til 15.februar(2023))
            result.ytelser shouldBeEqual Ytelser.IngenTreff(
                // Dagens dato er 1. februar
                oppslagsperiode = 1.desember(2022) til 1.februar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
            result.tiltakspengevedtakFraArena shouldBeEqual TiltakspengevedtakFraArena.IngenTreff(
                oppslagsperiode = 3.januar(2023) til 15.februar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
        }
    }

    @Test
    fun `2 tiltak uten overlapp`() {
        // Kommentar jah: Dette er ikke aktuelt for søknadsbehandling, men for revurering. Foreløpig behandler vi denne som om de overlapper, men bør på sikt endre det.
        return runBlocking {
            val fnr = Fnr.random()
            val correlationId = CorrelationId.generate()
            // Lar bruker være uten tiltak første og siste dag i perioden.
            val periode1: Periode = 3.januar(2023) til 24.januar(2023)
            val periode2: Periode = 26.januar(2023) til 15.februar(2023)
            val clock = Clock.fixed(1.februar(2023).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            val tiltak1 = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode1.fraOgMed,
                søknadTilOgMed = periode1.tilOgMed,
            )
            val tiltak2 = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode2.fraOgMed,
                søknadTilOgMed = periode2.tilOgMed,
            )
            val tiltaksdeltagelserDetErSøktTiltakspengerFor = TiltaksdeltagelserDetErSøktTiltakspengerFor(
                listOf(
                    TiltaksdeltagelseDetErSøktTiltakspengerFor(tiltak1.second, periode1.fraOgMed.atStartOfDay()),
                    TiltaksdeltagelseDetErSøktTiltakspengerFor(tiltak2.second, periode2.fraOgMed.atStartOfDay()),
                ),
            )
            val aktuelleTiltaksdeltagelserForBehandlingen = listOf(
                tiltak1.first.eksternDeltagelseId,
                tiltak2.first.eksternDeltagelseId,
            )

            val tiltaksdeltagelseKlient = object : TiltaksdeltagelseKlient {
                override suspend fun hentTiltaksdeltagelser(
                    fnr: Fnr,
                    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = Tiltaksdeltagelser(listOf(tiltak1.first, tiltak2.first))

                override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
                    fnr: Fnr,
                    harAdressebeskyttelse: Boolean,
                    correlationId: CorrelationId,
                ): List<TiltaksdeltakelseMedArrangørnavn> {
                    return emptyList()
                }
            }
            val sokosUtbetaldataClient = object : SokosUtbetaldataClient {
                override suspend fun hentYtelserFraUtbetaldata(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<Ytelse>()
            }
            val tiltakspengerArenaClient = object : TiltakspengerArenaClient {
                override suspend fun hentTiltakspengevedtakFraArena(
                    fnr: Fnr,
                    periode: Periode,
                    correlationId: CorrelationId,
                ) = emptyList<ArenaTPVedtak>()
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltagelserForBehandlingen = aktuelleTiltaksdeltagelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = false,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltagelser shouldBeEqual Tiltaksdeltagelser(listOf(tiltak1.first, tiltak2.first))
            result.periode!! shouldBeEqual (3.januar(2023) til 15.februar(2023))
            result.ytelser shouldBeEqual Ytelser.IngenTreff(
                // Dagens dato er 1. februar
                oppslagsperiode = 1.desember(2022) til 1.februar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
            result.tiltakspengevedtakFraArena shouldBeEqual TiltakspengevedtakFraArena.IngenTreff(
                oppslagsperiode = 3.januar(2023) til 15.februar(2023),
                oppslagstidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0),
            )
        }
    }
}
