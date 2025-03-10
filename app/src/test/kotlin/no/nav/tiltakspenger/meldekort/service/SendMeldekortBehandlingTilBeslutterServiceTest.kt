package no.nav.tiltakspenger.meldekort.service

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando.Dager.Dag
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.SPERRET
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.meldekortBehandlingOpprettet
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
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutningKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = SendMeldekortTilBeslutningKommando.Dager(
                            dager = nonEmptyListOf(
                                Dag(
                                    dag = ikkeUtfyltMeldekort.fraOgMed,
                                    status = SPERRET,
                                ),
                            ),
                        ),
                    ),
                ) shouldBe KanIkkeSendeMeldekortTilBeslutning.InnsendteDagerMåMatcheMeldeperiode.left()
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
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutningKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = SendMeldekortTilBeslutningKommando.Dager(
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
                    ),
                ) shouldBe KanIkkeSendeMeldekortTilBeslutning.InnsendteDagerMåMatcheMeldeperiode.left()
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
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutningKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = SendMeldekortTilBeslutningKommando.Dager(
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
                    ),
                ) shouldBe KanIkkeSendeMeldekortTilBeslutning.InnsendteDagerMåMatcheMeldeperiode.left()
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
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutningKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = SendMeldekortTilBeslutningKommando.Dager(
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
                    ),
                ) shouldBe KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagTilSperret.left()
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
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutningKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = SendMeldekortTilBeslutningKommando.Dager(
                            dager = dager(
                                førsteDag,
                                DELTATT_UTEN_LØNN_I_TILTAKET,
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
                    ),
                ) shouldBe KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagFraSperret.left()
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
                    SendMeldekortTilBeslutningKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        dager = SendMeldekortTilBeslutningKommando.Dager(
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
                    ),
                ).getOrNull()!!
            }
        }
    }

    private fun dager(
        førsteDag: LocalDate,
        vararg statuser: SendMeldekortTilBeslutningKommando.Status,
    ): NonEmptyList<Dag> {
        return dager(førsteDag, statuser.toList())
    }

    private fun dager(
        førsteDag: LocalDate,
        statuser: List<SendMeldekortTilBeslutningKommando.Status>,
    ): NonEmptyList<Dag> {
        return statuser.mapIndexed { index, status ->
            Dag(
                dag = førsteDag.plusDays(index.toLong()),
                status = status,
            )
        }.toNonEmptyListOrNull()!!
    }
}
