package no.nav.tiltakspenger.saksbehandling.service

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager.Dag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.SPERRET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.meldekortBehandlingOpprettet
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SendMeldekortBehandlingTilBeslutterServiceTest {

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
                            status = SPERRET,
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
                        SPERRET,
                        SPERRET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_DELTATT,
                        IKKE_DELTATT,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_DELTATT,
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
                        SPERRET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_DELTATT,
                        IKKE_DELTATT,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        DELTATT_UTEN_LØNN_I_TILTAKET,
                        IKKE_DELTATT,
                        IKKE_DELTATT,
                        SPERRET,
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
    fun `Kan ikke sende sperret på en ikke-sperret dag`() {
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
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
                        SPERRET,
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
                }.message shouldContain "Kan ikke endre dag til sperret"
            }
        }
    }

    @Test
    fun `Må sende sperret på en sperret dag`() {
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
                        IKKE_DELTATT,
                        IKKE_DELTATT,
                        IKKE_DELTATT,
                        IKKE_DELTATT,
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
                }.message.shouldContain("Kan ikke endre dag fra sperret.")
            }
        }
    }

    @Test
    fun `Sperret matcher 1 - 1`() {
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
                                SPERRET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                IKKE_DELTATT,
                                IKKE_DELTATT,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                IKKE_DELTATT,
                                IKKE_DELTATT,
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
