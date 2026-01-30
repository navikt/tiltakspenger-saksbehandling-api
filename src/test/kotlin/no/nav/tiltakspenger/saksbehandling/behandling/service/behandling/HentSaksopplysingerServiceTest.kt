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
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelseDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelserFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.toTiltaksdeltakelseFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class HentSaksopplysingerServiceTest {

    @Test
    fun `en tiltaksdeltakelse ingen ytelser`() {
        return runBlocking {
            val fnr = Fnr.random()
            val correlationId = CorrelationId.generate()
            // Lar bruker være uten tiltak første og siste dag i perioden.
            val periode: Periode = 2.januar(2023) til 30.januar(2023)
            val clock = Clock.fixed(1.februar(2023).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            val tiltaksdeltakelser = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode.fraOgMed,
                søknadTilOgMed = periode.tilOgMed,
            )
            val tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
                tiltaksdeltakelser.second,
                periode.fraOgMed.atStartOfDay(),
            )
            val aktuelleTiltaksdeltakelserForBehandlingen = listOf(tiltaksdeltakelser.first.internDeltakelseId)

            val tiltaksdeltakelseKlient = object : TiltaksdeltakelseKlient {
                override suspend fun hentTiltaksdeltakelser(
                    fnr: Fnr,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = TiltaksdeltakelserFraRegister(tiltaksdeltakelser.first.toTiltaksdeltakelseFraRegister())

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
            val tiltaksdeltakerRepo = object : TiltaksdeltakerRepo {
                override fun hentEllerLagre(
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ): TiltaksdeltakerId {
                    return tiltaksdeltakelser.first.internDeltakelseId
                }
                override fun lagre(
                    id: TiltaksdeltakerId,
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ) {}
                override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
                    return null
                }
                override fun hentEksternId(id: TiltaksdeltakerId, sessionContext: SessionContext?): String {
                    return tiltaksdeltakelser.first.eksternDeltakelseId
                }
                override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
                    return null
                }
                override fun oppdaterEksternIdForTiltaksdeltaker(
                    tiltaksdeltaker: Tiltaksdeltaker,
                    sessionContext: SessionContext?,
                ) {
                }
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
                tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            )

            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = false,
            )

            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltakelser shouldBeEqual Tiltaksdeltakelser(tiltaksdeltakelser.first)
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
            val tiltaksdeltakelser = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = periode.fraOgMed,
                søknadTilOgMed = periode.tilOgMed,
                registerFraOgMed = null,
                registerTilOgMed = null,
            )
            val tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
                tiltaksdeltakelser.second,
                periode.fraOgMed.atStartOfDay(),
            )
            val aktuelleTiltaksdeltakelserForBehandlingen = listOf(tiltaksdeltakelser.first.internDeltakelseId)

            val tiltaksdeltakelseKlient = object : TiltaksdeltakelseKlient {
                override suspend fun hentTiltaksdeltakelser(
                    fnr: Fnr,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = TiltaksdeltakelserFraRegister(tiltaksdeltakelser.first.toTiltaksdeltakelseFraRegister())

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
            val tiltaksdeltakerRepo = object : TiltaksdeltakerRepo {
                override fun hentEllerLagre(
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ): TiltaksdeltakerId {
                    return tiltaksdeltakelser.first.internDeltakelseId
                }
                override fun lagre(
                    id: TiltaksdeltakerId,
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ) {}
                override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
                    return null
                }
                override fun hentEksternId(id: TiltaksdeltakerId, sessionContext: SessionContext?): String {
                    return tiltaksdeltakelser.first.eksternDeltakelseId
                }
                override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
                    return null
                }
                override fun oppdaterEksternIdForTiltaksdeltaker(
                    tiltaksdeltaker: Tiltaksdeltaker,
                    sessionContext: SessionContext?,
                ) {
                }
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
                tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = true,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltakelser shouldBeEqual Tiltaksdeltakelser(tiltaksdeltakelser.first)
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
            val tiltaksdeltakelser = ObjectMother.tiltakOgSøknadstiltak(
                søknadFraOgMed = søknadsperiode.fraOgMed,
                søknadTilOgMed = søknadsperiode.tilOgMed,
                registerFraOgMed = registerperiode.fraOgMed,
                registerTilOgMed = registerperiode.tilOgMed,
            )
            val tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
                tiltaksdeltakelser.second,
                søknadsperiode.fraOgMed.atStartOfDay(),
            )
            val aktuelleTiltaksdeltakelserForBehandlingen = listOf(tiltaksdeltakelser.first.internDeltakelseId)

            val tiltaksdeltakelseKlient = object : TiltaksdeltakelseKlient {
                override suspend fun hentTiltaksdeltakelser(
                    fnr: Fnr,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = TiltaksdeltakelserFraRegister(tiltaksdeltakelser.first.toTiltaksdeltakelseFraRegister())

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
            val tiltaksdeltakerRepo = object : TiltaksdeltakerRepo {
                override fun hentEllerLagre(
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ): TiltaksdeltakerId {
                    return tiltaksdeltakelser.first.internDeltakelseId
                }
                override fun lagre(
                    id: TiltaksdeltakerId,
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ) {}
                override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
                    return null
                }
                override fun hentEksternId(id: TiltaksdeltakerId, sessionContext: SessionContext?): String {
                    return tiltaksdeltakelser.first.eksternDeltakelseId
                }
                override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
                    return null
                }
                override fun oppdaterEksternIdForTiltaksdeltaker(
                    tiltaksdeltaker: Tiltaksdeltaker,
                    sessionContext: SessionContext?,
                ) {
                }
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
                tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = true,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltakelser shouldBeEqual Tiltaksdeltakelser(tiltaksdeltakelser.first)
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
            val tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
                listOf(
                    TiltaksdeltakelseDetErSøktTiltakspengerFor(tiltak1.second, periode1.fraOgMed.atStartOfDay()),
                    TiltaksdeltakelseDetErSøktTiltakspengerFor(tiltak2.second, periode2.fraOgMed.atStartOfDay()),
                ),
            )
            // Simulerer at vi søknadsbehandler tiltak1. Tiltak2 overlapper og algoritmen vil da hente begge tiltakene.
            val aktuelleTiltaksdeltakelserForBehandlingen = listOf(
                tiltak1.first.internDeltakelseId,
            )

            val tiltaksdeltakelseKlient = object : TiltaksdeltakelseKlient {
                override suspend fun hentTiltaksdeltakelser(
                    fnr: Fnr,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = TiltaksdeltakelserFraRegister(listOf(tiltak1.first.toTiltaksdeltakelseFraRegister(), tiltak2.first.toTiltaksdeltakelseFraRegister()))

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
            val tiltaksdeltakerRepo = object : TiltaksdeltakerRepo {
                override fun hentEllerLagre(
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ): TiltaksdeltakerId {
                    return when (eksternId) {
                        tiltak1.first.eksternDeltakelseId -> {
                            tiltak1.first.internDeltakelseId
                        }
                        tiltak2.first.eksternDeltakelseId -> {
                            tiltak2.first.internDeltakelseId
                        }
                        else -> {
                            throw IllegalArgumentException("Ukjent tiltak")
                        }
                    }
                }
                override fun lagre(
                    id: TiltaksdeltakerId,
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ) {}
                override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
                    return null
                }
                override fun hentEksternId(id: TiltaksdeltakerId, sessionContext: SessionContext?): String {
                    return when (id) {
                        tiltak1.first.internDeltakelseId -> {
                            tiltak1.first.eksternDeltakelseId
                        }
                        tiltak2.first.internDeltakelseId -> {
                            tiltak2.first.eksternDeltakelseId
                        }
                        else -> {
                            throw IllegalArgumentException("Ukjent eksternId")
                        }
                    }
                }
                override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
                    return null
                }
                override fun oppdaterEksternIdForTiltaksdeltaker(
                    tiltaksdeltaker: Tiltaksdeltaker,
                    sessionContext: SessionContext?,
                ) {
                }
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
                tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = true,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltakelser shouldBeEqual Tiltaksdeltakelser(listOf(tiltak1.first, tiltak2.first))
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
            val tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
                listOf(
                    TiltaksdeltakelseDetErSøktTiltakspengerFor(tiltak1.second, periode1.fraOgMed.atStartOfDay()),
                    TiltaksdeltakelseDetErSøktTiltakspengerFor(tiltak2.second, periode2.fraOgMed.atStartOfDay()),
                ),
            )
            val aktuelleTiltaksdeltakelserForBehandlingen = listOf(
                tiltak1.first.internDeltakelseId,
                tiltak2.first.internDeltakelseId,
            )

            val tiltaksdeltakelseKlient = object : TiltaksdeltakelseKlient {
                override suspend fun hentTiltaksdeltakelser(
                    fnr: Fnr,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
                    correlationId: CorrelationId,
                ) = TiltaksdeltakelserFraRegister(listOf(tiltak1.first.toTiltaksdeltakelseFraRegister(), tiltak2.first.toTiltaksdeltakelseFraRegister()))

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
            val tiltaksdeltakerRepo = object : TiltaksdeltakerRepo {
                override fun hentEllerLagre(
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ): TiltaksdeltakerId {
                    return when (eksternId) {
                        tiltak1.first.eksternDeltakelseId -> {
                            tiltak1.first.internDeltakelseId
                        }
                        tiltak2.first.eksternDeltakelseId -> {
                            tiltak2.first.internDeltakelseId
                        }
                        else -> {
                            throw IllegalArgumentException("Ukjent tiltak")
                        }
                    }
                }
                override fun lagre(
                    id: TiltaksdeltakerId,
                    eksternId: String,
                    tiltakstype: TiltakResponsDTO.TiltakType,
                    sessionContext: SessionContext?,
                ) {}
                override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
                    return null
                }
                override fun hentEksternId(id: TiltaksdeltakerId, sessionContext: SessionContext?): String {
                    return when (id) {
                        tiltak1.first.internDeltakelseId -> {
                            tiltak1.first.eksternDeltakelseId
                        }
                        tiltak2.first.internDeltakelseId -> {
                            tiltak2.first.eksternDeltakelseId
                        }
                        else -> {
                            throw IllegalArgumentException("Ukjent eksternId")
                        }
                    }
                }
                override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
                    return null
                }
                override fun oppdaterEksternIdForTiltaksdeltaker(
                    tiltaksdeltaker: Tiltaksdeltaker,
                    sessionContext: SessionContext?,
                ) {
                }
            }
            val fyr = ObjectMother.personopplysningKjedeligFyr(fnr = fnr)
            val service = HentSaksopplysingerService(
                hentPersonopplysninger = { fyr },
                tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
                sokosUtbetaldataClient = sokosUtbetaldataClient,
                clock = clock,
                tiltakspengerArenaClient = tiltakspengerArenaClient,
                tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            )
            val result = service.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = false,
            )
            result.fødselsdato shouldBeEqual fyr.fødselsdato
            result.tiltaksdeltakelser shouldBeEqual Tiltaksdeltakelser(listOf(tiltak1.first, tiltak2.first))
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
