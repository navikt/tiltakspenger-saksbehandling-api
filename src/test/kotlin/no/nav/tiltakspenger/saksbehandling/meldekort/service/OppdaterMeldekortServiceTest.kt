package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.IKKE_TILTAKSDAG
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.nyOpprettetMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.meldekortbehandlingOpprettet
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppdaterMeldekortServiceTest {

    @Test
    fun `En meldeperiode kan ikke være 1 dag`() {
        val clock = TikkendeKlokke()
        val correlationId = CorrelationId.generate()
        runTest {
            withTestApplicationContext { tac ->
                val sak = tac.meldekortbehandlingOpprettet()
                val ikkeUtfyltMeldekort = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                val dager = OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                    dager = nonEmptyListOf(
                        OppdatertDag(
                            dag = ikkeUtfyltMeldekort.fraOgMed,
                            status = IKKE_RETT_TIL_TILTAKSPENGER,
                        ),
                    ),
                    kjedeId = ikkeUtfyltMeldekort.meldeperioder.first().kjedeId,
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                        OppdaterMeldekortbehandlingKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            meldeperioder = nonEmptyListOf(dager),
                            begrunnelse = null,
                            fritekstTilVedtaksbrev = null,
                            skalSendeVedtaksbrev = true,
                            skalAkkumulereMeldekort = false,
                        ),
                        clock,
                    )
                }.message shouldBe "Et meldekort må være 14 dager, men var 1"
            }
        }
    }

    @Test
    fun `innsendingsperioden kan ikke være før meldeperioden`() {
        val clock = TikkendeKlokke()
        val correlationId = CorrelationId.generate()
        runTest {
            withTestApplicationContext { tac ->
                val sak = tac.meldekortbehandlingOpprettet(
                    innvilgelsesperiode = Periode(3.januar(2023), 31.januar(2023)),
                )
                val ikkeUtfyltMeldekort = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed.minusDays(1)
                val dager = OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                    dager = dager(
                        førsteDag,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_TILTAKSDAG,
                        IKKE_TILTAKSDAG,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_TILTAKSDAG,
                    ),
                    kjedeId = ikkeUtfyltMeldekort.meldeperioder.first().kjedeId,
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                        OppdaterMeldekortbehandlingKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            meldeperioder = nonEmptyListOf(dager),
                            begrunnelse = null,
                            fritekstTilVedtaksbrev = null,
                            skalSendeVedtaksbrev = true,
                            skalAkkumulereMeldekort = false,
                        ),
                        clock,
                    )
                }.message shouldBe "Meldekortet må starte på en mandag"
            }
        }
    }

    @Test
    fun `innsendingsperioden kan ikke være etter meldeperioden`() {
        val clock = TikkendeKlokke()
        val correlationId = CorrelationId.generate()
        runTest {
            withTestApplicationContext { tac ->
                val sak = tac.meldekortbehandlingOpprettet(
                    innvilgelsesperiode = Periode(3.januar(2023), 31.januar(2023)),
                )
                val ikkeUtfyltMeldekort = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                val dager = OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                    dager = dager(
                        førsteDag,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_TILTAKSDAG,
                        IKKE_TILTAKSDAG,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_TILTAKSDAG,
                        IKKE_TILTAKSDAG,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                    ),
                    kjedeId = ikkeUtfyltMeldekort.meldeperioder.first().kjedeId,
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                        OppdaterMeldekortbehandlingKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            meldeperioder = nonEmptyListOf(dager),
                            begrunnelse = null,
                            fritekstTilVedtaksbrev = null,
                            skalSendeVedtaksbrev = true,
                            skalAkkumulereMeldekort = false,
                        ),
                        clock,
                    )
                }.message shouldBe "Et meldekort må være 14 dager, men var 15"
            }
        }
    }

    @Test
    fun `Kan ikke sende IKKE_RETT_TIL_TILTAKSPENGER på en innvilget dag`() {
        val clock = TikkendeKlokke()
        val correlationId = CorrelationId.generate()
        runTest {
            withTestApplicationContext { tac ->
                val sak = tac.meldekortbehandlingOpprettet(
                    innvilgelsesperiode = Periode(3.januar(2023), 31.januar(2023)),
                )
                val ikkeUtfyltMeldekort = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                val dager = OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                    dager = dager(
                        førsteDag,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                        IKKE_RETT_TIL_TILTAKSPENGER,
                    ),
                    kjedeId = ikkeUtfyltMeldekort.meldeperioder.first().kjedeId,
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                        OppdaterMeldekortbehandlingKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            meldeperioder = nonEmptyListOf(dager),
                            begrunnelse = null,
                            fritekstTilVedtaksbrev = null,
                            skalSendeVedtaksbrev = true,
                            skalAkkumulereMeldekort = false,
                        ),
                        clock,
                    )
                }.message shouldContain "Kan ikke endre dag til IKKE_RETT_TIL_TILTAKSPENGER"
            }
        }
    }

    @Test
    fun `Må sende IKKE_RETT_TIL_TILTAKSPENGER på en ikke-innvilget dag`() {
        val clock = TikkendeKlokke()
        val correlationId = CorrelationId.generate()
        runTest {
            withTestApplicationContext { tac ->
                val sak = tac.meldekortbehandlingOpprettet(
                    innvilgelsesperiode = Periode(3.januar(2023), 31.januar(2023)),
                )
                val ikkeUtfyltMeldekort = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                val dager = OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                    dager = dager(
                        førsteDag,
                        // Denne linjen skal gi oss feil
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_TILTAKSDAG,
                        IKKE_TILTAKSDAG,
                        IKKE_TILTAKSDAG,
                        IKKE_TILTAKSDAG,
                    ),
                    kjedeId = ikkeUtfyltMeldekort.meldeperioder.first().kjedeId,
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                        OppdaterMeldekortbehandlingKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            meldeperioder = nonEmptyListOf(dager),
                            begrunnelse = null,
                            fritekstTilVedtaksbrev = null,
                            skalSendeVedtaksbrev = true,
                            skalAkkumulereMeldekort = false,
                        ),
                        clock,
                    )
                }.message.shouldContain("Kan ikke endre dag fra IKKE_RETT_TIL_TILTAKSPENGER.")
            }
        }
    }

    @Test
    fun `IKKE_RETT_TIL_TILTAKSPENGER matcher 1 - 1`() {
        val clock = TikkendeKlokke()
        val correlationId = CorrelationId.generate()
        runTest {
            withTestApplicationContext { tac ->
                val sak = tac.meldekortbehandlingOpprettet(
                    innvilgelsesperiode = Periode(3.januar(2023), 31.januar(2023)),
                )
                val ikkeUtfyltMeldekort = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                    OppdaterMeldekortbehandlingKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        meldeperioder = nonEmptyListOf(
                            OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                                dager = dager(
                                    førsteDag,
                                    IKKE_RETT_TIL_TILTAKSPENGER,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    IKKE_TILTAKSDAG,
                                    IKKE_TILTAKSDAG,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    DELTATT_UTEN_LØNN_I_TILTAKET,
                                    IKKE_TILTAKSDAG,
                                    IKKE_TILTAKSDAG,
                                ),
                                kjedeId = ikkeUtfyltMeldekort.meldeperioder.first().kjedeId,
                            ),
                        ),
                        begrunnelse = null,
                        fritekstTilVedtaksbrev = null,
                        skalSendeVedtaksbrev = true,
                        skalAkkumulereMeldekort = false,
                    ),
                    clock,
                ).getOrFail()
            }
        }
    }

    @Test
    fun `Kan ikke oppdatere en behandling med en kjede som har en annen åpen behandling`() {
        val clock = TikkendeKlokke()
        runTest {
            withTestApplicationContext { tac ->
                val (sak) = iverksettSøknadsbehandling(
                    tac = tac,
                    innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
                        Periode(6.januar(2025), 2.februar(2025)),
                    ),
                )

                val (_, behandlingPåFørsteKjede) = tac.nyOpprettetMeldekortbehandling(
                    sakId = sak.id,
                    kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                )
                val (_, behandlingPåAndreKjede) = tac.nyOpprettetMeldekortbehandling(
                    sakId = sak.id,
                    kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                )

                val førsteKjedeId = behandlingPåFørsteKjede.kjedeIder.single()

                tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                    kommando = ObjectMother.oppdaterMeldekortKommando(
                        sakId = sak.id,
                        meldekortId = behandlingPåAndreKjede.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        meldeperioder = behandlingPåFørsteKjede
                            .tilOppdaterMeldekortKommando(ObjectMother.saksbehandler())
                            .meldeperioder,
                    ),
                    clock = clock,
                ) shouldBe KanIkkeOppdatereMeldekortbehandling.KjedeErUnderBehandling(setOf(førsteKjedeId)).left()
            }
        }
    }

    @Test
    fun `kan sette og fjerne skalAkkumulereMeldekort-flagget`() {
        val clock = TikkendeKlokke()
        runTest {
            withTestApplicationContext { tac ->
                val saksbehandler = ObjectMother.saksbehandler()
                val sak = tac.meldekortbehandlingOpprettet(saksbehandler = saksbehandler)
                val behandling = sak.meldekortbehandlinger.meldekortbehandlingerUnderBehandling.single()
                behandling.skalAkkumulereMeldekort shouldBe false

                tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                    kommando = behandling.tilOppdaterMeldekortKommando(saksbehandler, skalAkkumulereMeldekort = true),
                    clock = clock,
                ).getOrFail().second.skalAkkumulereMeldekort shouldBe true

                tac.meldekortContext.meldekortbehandlingRepo.hent(behandling.id)!!.skalAkkumulereMeldekort shouldBe true

                tac.meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                    kommando = behandling.tilOppdaterMeldekortKommando(saksbehandler, skalAkkumulereMeldekort = false),
                    clock = clock,
                ).getOrFail().second.skalAkkumulereMeldekort shouldBe false

                tac.meldekortContext.meldekortbehandlingRepo.hent(behandling.id)!!.skalAkkumulereMeldekort shouldBe false
            }
        }
    }

    private fun dager(
        førsteDag: LocalDate,
        vararg statuser: OppdaterMeldekortbehandlingKommando.Status,
    ): NonEmptyList<OppdatertDag> {
        return dager(førsteDag, statuser.toList())
    }

    private fun dager(
        førsteDag: LocalDate,
        statuser: List<OppdaterMeldekortbehandlingKommando.Status>,
    ): NonEmptyList<OppdatertDag> {
        return statuser.mapIndexed { index, status ->
            OppdatertDag(
                dag = førsteDag.plusDays(index.toLong()),
                status = status,
            )
        }.toNonEmptyListOrNull()!!
    }
}
