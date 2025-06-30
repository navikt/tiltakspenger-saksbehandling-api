package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager.Dag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.meldekortBehandlingOpprettet
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SendMeldekortTilBeslutterServiceTest {

    @Test
    fun `En meldeperiode kan ikke være 1 dag`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.meldekortBehandlingOpprettet(correlationId = correlationId)
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.meldekortUnderBehandling!!
                val dager = OppdaterMeldekortKommando.Dager(
                    dager = nonEmptyListOf(
                        Dag(
                            dag = ikkeUtfyltMeldekort.fraOgMed,
                            status = IKKE_RETT_TIL_TILTAKSPENGER,
                        ),
                    ),
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                        SendMeldekortTilBeslutterKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            dager = dager,
                            begrunnelse = null,
                        ),
                    )
                }.message shouldBe "Et meldekort må være 14 dager, men var 1"
            }
        }
    }

    @Test
    fun `innsendingsperioden kan ikke være før meldeperioden`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.meldekortBehandlingOpprettet(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.meldekortUnderBehandling!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed.minusDays(1)
                val dager = OppdaterMeldekortKommando.Dager(
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
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                        SendMeldekortTilBeslutterKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            dager = dager,
                            begrunnelse = null,
                        ),
                    )
                }.message shouldBe "Meldekortet må starte på en mandag"
            }
        }
    }

    @Test
    fun `innsendingsperioden kan ikke være etter meldeperioden`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.meldekortBehandlingOpprettet(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.meldekortUnderBehandling!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                val dager = OppdaterMeldekortKommando.Dager(
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
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                        SendMeldekortTilBeslutterKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            dager = dager,
                            begrunnelse = null,
                        ),
                    )
                }.message shouldBe "Et meldekort må være 14 dager, men var 15"
            }
        }
    }

    @Test
    fun `Kan ikke sende IKKE_RETT_TIL_TILTAKSPENGER på en innvilget dag`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.meldekortBehandlingOpprettet(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.meldekortUnderBehandling!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                val dager = OppdaterMeldekortKommando.Dager(
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
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                        SendMeldekortTilBeslutterKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            dager = dager,
                            begrunnelse = null,
                        ),
                    )
                }.message shouldContain "Kan ikke endre dag til IKKE_RETT_TIL_TILTAKSPENGER"
            }
        }
    }

    @Test
    fun `Må sende IKKE_RETT_TIL_TILTAKSPENGER på en ikke-innvilget dag`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.meldekortBehandlingOpprettet(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.meldekortUnderBehandling!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                val dager = OppdaterMeldekortKommando.Dager(
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
                )
                shouldThrow<IllegalArgumentException> {
                    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                        SendMeldekortTilBeslutterKommando(
                            sakId = sak.id,
                            meldekortId = ikkeUtfyltMeldekort.id,
                            saksbehandler = ObjectMother.saksbehandler(),
                            correlationId = correlationId,
                            dager = dager,
                            begrunnelse = null,
                        ),
                    )
                }.message.shouldContain("Kan ikke endre dag fra IKKE_RETT_TIL_TILTAKSPENGER.")
            }
        }
    }

    @Test
    fun `IKKE_RETT_TIL_TILTAKSPENGER matcher 1 - 1`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.meldekortBehandlingOpprettet(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.meldekortUnderBehandling!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = OppdaterMeldekortKommando.Dager(
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
                        ),
                        begrunnelse = null,
                    ),
                ).getOrFail()
            }
        }
    }

    private fun dager(
        førsteDag: LocalDate,
        vararg statuser: OppdaterMeldekortKommando.Status,
    ): NonEmptyList<Dag> {
        return dager(førsteDag, statuser.toList())
    }

    private fun dager(
        førsteDag: LocalDate,
        statuser: List<OppdaterMeldekortKommando.Status>,
    ): NonEmptyList<Dag> {
        return statuser.mapIndexed { index, status ->
            Dag(
                dag = førsteDag.plusDays(index.toLong()),
                status = status,
            )
        }.toNonEmptyListOrNull()!!
    }
}
