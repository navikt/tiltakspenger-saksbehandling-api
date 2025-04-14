package no.nav.tiltakspenger.saksbehandling.service

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager.Dag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.SPERRET
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
                tac.meldekortContext.oppdaterMeldekortService.sendMeldekortTilBeslutter(
                    OppdaterMeldekortKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = OppdaterMeldekortKommando.Dager(
                            dager = nonEmptyListOf(
                                Dag(
                                    dag = ikkeUtfyltMeldekort.fraOgMed,
                                    status = SPERRET,
                                ),
                            ),
                        ),
                        begrunnelse = null,
                    ),
                ) shouldBe KanIkkeOppdatereMeldekort.InnsendteDagerMåMatcheMeldeperiode.left()
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
                tac.meldekortContext.oppdaterMeldekortService.sendMeldekortTilBeslutter(
                    OppdaterMeldekortKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = OppdaterMeldekortKommando.Dager(
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
                                IKKE_DELTATT,
                            ),
                        ),
                        begrunnelse = null,
                    ),
                ) shouldBe KanIkkeOppdatereMeldekort.InnsendteDagerMåMatcheMeldeperiode.left()
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
                tac.meldekortContext.oppdaterMeldekortService.sendMeldekortTilBeslutter(
                    OppdaterMeldekortKommando(
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
                                SPERRET,
                            ),
                        ),
                        begrunnelse = null,
                    ),
                ) shouldBe KanIkkeOppdatereMeldekort.InnsendteDagerMåMatcheMeldeperiode.left()
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
                tac.meldekortContext.oppdaterMeldekortService.sendMeldekortTilBeslutter(
                    OppdaterMeldekortKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = OppdaterMeldekortKommando.Dager(
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
                        ),
                        begrunnelse = null,
                    ),
                ) shouldBe KanIkkeOppdatereMeldekort.KanIkkeEndreDagTilSperret.left()
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
                tac.meldekortContext.oppdaterMeldekortService.sendMeldekortTilBeslutter(
                    OppdaterMeldekortKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = OppdaterMeldekortKommando.Dager(
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
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
                            ),
                        ),
                        begrunnelse = null,
                    ),
                ) shouldBe KanIkkeOppdatereMeldekort.KanIkkeEndreDagFraSperret.left()
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
                tac.meldekortContext.oppdaterMeldekortService.sendMeldekortTilBeslutter(
                    OppdaterMeldekortKommando(
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
