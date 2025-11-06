package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.fraOgMedDatoIkkeBesvart
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.fraOgMedDatoJa
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.fraOgMedDatoNei
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.ikkeBesvart
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.ja
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nei
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.periodeIkkeBesvart
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.periodeJa
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.periodeNei
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøknadPostgresRepoTest {
    @Nested
    inner class LagreSøknad {
        @Test
        fun `lagre og hente en søknad`() {
            withMigratedDb { testDataHelper ->
                val søknadRepo = testDataHelper.søknadRepo
                val fnr = Fnr.random()
                val søknad = testDataHelper.persisterSakOgSøknad(fnr = fnr)

                søknadRepo.hentSøknaderForFnr(fnr).also {
                    it.singleOrNullOrThrow() shouldBe søknad
                }
            }
        }

        @Test
        fun `lagre og hente en innvilgbar søknad`() {
            withMigratedDb { testDataHelper ->
                val søknadRepo = testDataHelper.søknadRepo
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                val persistertSøknad = testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        fnr = fnr,
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )

                søknadRepo.hentSøknaderForFnr(fnr).also { hentetSøknad ->
                    hentetSøknad.singleOrNullOrThrow() shouldBe persistertSøknad
                }
            }
        }

        @Test
        fun `lagre og hente en ikke innvilgbar søknad`() {
            withMigratedDb { testDataHelper ->
                val søknadRepo = testDataHelper.søknadRepo
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                val persistertSøknad = testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyIkkeInnvilgbarSøknad(
                        fnr = fnr,
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )

                søknadRepo.hentSøknaderForFnr(fnr).also { hentetSøknad ->
                    hentetSøknad.singleOrNullOrThrow() shouldBe persistertSøknad
                }
            }
        }

        @Test
        fun `søknad med nei på alle spørsmål`() {
            withMigratedDb { testDataHelper ->
                val søknadRepo = testDataHelper.søknadRepo
                val fnr = Fnr.random()
                val deltakelseFom = 1.januar(2023)
                val deltakelseTom = 31.mars(2023)
                val tiltak = ObjectMother.søknadstiltak(deltakelseFom = deltakelseFom, deltakelseTom = deltakelseTom)
                val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())

                val søknad = ObjectMother.nyInnvilgbarSøknad(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    periode = Periode(deltakelseFom, deltakelseTom),
                    søknadstiltak = tiltak,
                    fnr = fnr,
                    kvp = periodeNei(),
                    intro = periodeNei(),
                    institusjon = periodeNei(),
                    trygdOgPensjon = periodeNei(),
                    etterlønn = nei(),
                    gjenlevendepensjon = periodeNei(),
                    alderspensjon = fraOgMedDatoNei(),
                    sykepenger = periodeNei(),
                    supplerendeStønadAlder = periodeNei(),
                    supplerendeStønadFlyktning = periodeNei(),
                    jobbsjansen = periodeNei(),
                )

                val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                    søknader.single().also { hentetSøknad ->
                        hentetSøknad shouldBe persistertSøknad
                        hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                        // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                        hentetSøknad.kvp shouldBe periodeNei()
                        hentetSøknad.intro shouldBe periodeNei()
                        hentetSøknad.institusjon shouldBe periodeNei()
                        hentetSøknad.trygdOgPensjon shouldBe periodeNei()
                        hentetSøknad.etterlønn shouldBe nei()
                        hentetSøknad.gjenlevendepensjon shouldBe periodeNei()
                        hentetSøknad.alderspensjon shouldBe fraOgMedDatoNei()
                        hentetSøknad.sykepenger shouldBe periodeNei()
                        hentetSøknad.supplerendeStønadAlder shouldBe periodeNei()
                        hentetSøknad.supplerendeStønadFlyktning shouldBe periodeNei()
                        hentetSøknad.jobbsjansen shouldBe periodeNei()
                    }
                }
            }
        }

        @Nested
        inner class JaNeiSpm {
            @Test
            fun `kan svare ja`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        etterlønn = ja(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.etterlønn shouldBe ja()
                        }
                    }
                }
            }

            @Test
            fun `kan svare nei`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        etterlønn = nei(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.etterlønn shouldBe nei()
                        }
                    }
                }
            }

            @Test
            fun `kan mangle svar`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        etterlønn = ikkeBesvart(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.etterlønn shouldBe ikkeBesvart()
                        }
                    }
                }
            }
        }

        @Nested
        inner class PeriodeSpm {
            @Test
            fun `kan svare ja`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        kvp = periodeJa(),
                        intro = periodeJa(),
                        institusjon = periodeJa(),
                        trygdOgPensjon = periodeJa(),
                        gjenlevendepensjon = periodeJa(),
                        sykepenger = periodeJa(),
                        supplerendeStønadAlder = periodeJa(),
                        supplerendeStønadFlyktning = periodeJa(),
                        jobbsjansen = periodeJa(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.kvp shouldBe periodeJa()
                            hentetSøknad.intro shouldBe periodeJa()
                            hentetSøknad.institusjon shouldBe periodeJa()
                            hentetSøknad.trygdOgPensjon shouldBe periodeJa()
                            hentetSøknad.gjenlevendepensjon shouldBe periodeJa()
                            hentetSøknad.sykepenger shouldBe periodeJa()
                            hentetSøknad.supplerendeStønadAlder shouldBe periodeJa()
                            hentetSøknad.supplerendeStønadFlyktning shouldBe periodeJa()
                            hentetSøknad.jobbsjansen shouldBe periodeJa()
                        }
                    }
                }
            }

            @Test
            fun `kan svare nei`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        kvp = periodeNei(),
                        intro = periodeNei(),
                        institusjon = periodeNei(),
                        trygdOgPensjon = periodeNei(),
                        gjenlevendepensjon = periodeNei(),
                        sykepenger = periodeNei(),
                        supplerendeStønadAlder = periodeNei(),
                        supplerendeStønadFlyktning = periodeNei(),
                        jobbsjansen = periodeNei(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.kvp shouldBe periodeNei()
                            hentetSøknad.intro shouldBe periodeNei()
                            hentetSøknad.institusjon shouldBe periodeNei()
                            hentetSøknad.trygdOgPensjon shouldBe periodeNei()
                            hentetSøknad.gjenlevendepensjon shouldBe periodeNei()
                            hentetSøknad.sykepenger shouldBe periodeNei()
                            hentetSøknad.supplerendeStønadAlder shouldBe periodeNei()
                            hentetSøknad.supplerendeStønadFlyktning shouldBe periodeNei()
                            hentetSøknad.jobbsjansen shouldBe periodeNei()
                        }
                    }
                }
            }

            @Test
            fun `kan mangle svar`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        kvp = periodeIkkeBesvart(),
                        intro = periodeIkkeBesvart(),
                        institusjon = periodeIkkeBesvart(),
                        trygdOgPensjon = periodeIkkeBesvart(),
                        gjenlevendepensjon = periodeIkkeBesvart(),
                        sykepenger = periodeIkkeBesvart(),
                        supplerendeStønadAlder = periodeIkkeBesvart(),
                        supplerendeStønadFlyktning = periodeIkkeBesvart(),
                        jobbsjansen = periodeIkkeBesvart(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.kvp shouldBe periodeIkkeBesvart()
                            hentetSøknad.intro shouldBe periodeIkkeBesvart()
                            hentetSøknad.institusjon shouldBe periodeIkkeBesvart()
                            hentetSøknad.trygdOgPensjon shouldBe periodeIkkeBesvart()
                            hentetSøknad.gjenlevendepensjon shouldBe periodeIkkeBesvart()
                            hentetSøknad.sykepenger shouldBe periodeIkkeBesvart()
                            hentetSøknad.supplerendeStønadAlder shouldBe periodeIkkeBesvart()
                            hentetSøknad.supplerendeStønadFlyktning shouldBe periodeIkkeBesvart()
                            hentetSøknad.jobbsjansen shouldBe periodeIkkeBesvart()
                        }
                    }
                }
            }
        }

        @Nested
        inner class FraOgMedDatoSpm {
            @Test
            fun `kan svare ja`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        alderspensjon = fraOgMedDatoJa(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.alderspensjon shouldBe fraOgMedDatoJa()
                        }
                    }
                }
            }

            @Test
            fun `kan svare nei`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        alderspensjon = fraOgMedDatoNei(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.alderspensjon shouldBe fraOgMedDatoNei()
                        }
                    }
                }
            }

            @Test
            fun `kan mangle svar`() {
                withMigratedDb { testDataHelper ->
                    val søknadRepo = testDataHelper.søknadRepo
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))
                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        alderspensjon = fraOgMedDatoIkkeBesvart(),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    søknadRepo.hentSøknaderForFnr(fnr).also { søknader ->
                        søknader.single().also { hentetSøknad ->
                            hentetSøknad shouldBe persistertSøknad
                            hentetSøknad.tiltak shouldBe persistertSøknad.tiltak
                            // Burde være dekket av objektsammenligningen, men i tilfelle den skulle brekke!
                            hentetSøknad.alderspensjon shouldBe fraOgMedDatoIkkeBesvart()
                        }
                    }
                }
            }
        }

        @Nested
        inner class Barnetillegg {
            @Test
            fun `lagrer barnetillegg med fnr dersom fnr eksisterer`() {
                withMigratedDb { testDataHelper ->
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))

                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        alderspensjon = fraOgMedDatoIkkeBesvart(),
                        barnetillegg = listOf(
                            BarnetilleggFraSøknad.FraPdl(
                                oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                                fornavn = "skal lagre",
                                mellomnavn = "fnr i basen",
                                etternavn = "og hente den ut igjen",
                                fødselsdato = LocalDate.now(fixedClock),
                                fnr = Fnr.random(),
                            ),
                            BarnetilleggFraSøknad.Manuell(
                                oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                                fornavn = "barn lagt inn manuell",
                                mellomnavn = "Denne har ikke fnr",
                                etternavn = "så blir hentet ut uten fnr :)",
                                fødselsdato = LocalDate.now(fixedClock),
                            ),
                        ),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    persistertSøknad shouldBe søknad
                }
            }

            @Test
            fun `lagrer barnetillegg uten fnr dersom fnr er null`() {
                withMigratedDb { testDataHelper ->
                    val fnr = Fnr.random()
                    val sak = ObjectMother.nySak(fnr = fnr, saksnummer = testDataHelper.saksnummerGenerator.neste())
                    val tiltak =
                        ObjectMother.søknadstiltak(deltakelseFom = 1.januar(2023), deltakelseTom = 31.mars(2023))

                    val søknad = ObjectMother.nyInnvilgbarSøknad(
                        periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        søknadstiltak = tiltak,
                        fnr = fnr,
                        alderspensjon = fraOgMedDatoIkkeBesvart(),
                        barnetillegg = listOf(
                            BarnetilleggFraSøknad.FraPdl(
                                oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                                fornavn = "skal lagre",
                                mellomnavn = "tom fnr i basen",
                                etternavn = "og hente den ut igjen",
                                fødselsdato = LocalDate.now(fixedClock),
                                fnr = null,
                            ),
                            BarnetilleggFraSøknad.Manuell(
                                oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                                fornavn = "barn lagt inn manuell",
                                mellomnavn = "Denne har ikke fnr",
                                etternavn = "så blir hentet ut uten fnr :)",
                                fødselsdato = LocalDate.now(fixedClock),
                            ),
                        ),
                    )

                    val persistertSøknad = testDataHelper.persisterSakOgSøknad(fnr = fnr, sak = sak, søknad = søknad)

                    persistertSøknad shouldBe søknad
                }
            }
        }
    }
}
