package no.nav.tiltakspenger.meldekort.service

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Dager.Dag
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.SPERRET
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingIverksatt
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SendMeldekortBehandlingTilBeslutterServiceTest {

    @Test
    fun `En meldeperiode kan ikke være 1 dag`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(correlationId = correlationId)
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.ikkeUtfyltMeldekort!!
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        navkontor = ObjectMother.navkontor(),
                        dager = SendMeldekortTilBeslutterKommando.Dager(
                            dager = nonEmptyListOf(
                                Dag(
                                    dag = ikkeUtfyltMeldekort.fraOgMed,
                                    status = SPERRET,
                                ),
                            ),
                        ),
                    ),
                ) shouldBeLeft KanIkkeSendeMeldekortTilBeslutter.InnsendteDagerMåMatcheMeldeperiode
            }
        }
    }

    @Test
    fun `innsendingsperioden kan ikke være før meldeperioden`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.ikkeUtfyltMeldekort!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed.minusDays(1)
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        navkontor = ObjectMother.navkontor(),
                        dager = SendMeldekortTilBeslutterKommando.Dager(
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
                ) shouldBeLeft KanIkkeSendeMeldekortTilBeslutter.InnsendteDagerMåMatcheMeldeperiode
            }
        }
    }

    @Test
    fun `innsendingsperioden kan ikke være etter meldeperioden`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.ikkeUtfyltMeldekort!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        navkontor = ObjectMother.navkontor(),
                        dager = SendMeldekortTilBeslutterKommando.Dager(
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
                ) shouldBeLeft KanIkkeSendeMeldekortTilBeslutter.InnsendteDagerMåMatcheMeldeperiode
            }
        }
    }

    @Test
    fun `Kan ikke sende sperret på en ikke-sperret dag`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.ikkeUtfyltMeldekort!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        navkontor = ObjectMother.navkontor(),
                        dager = SendMeldekortTilBeslutterKommando.Dager(
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
                ) shouldBeLeft KanIkkeSendeMeldekortTilBeslutter.KanIkkeEndreDagTilSperret
            }
        }
    }

    @Test
    fun `Må sende sperret på en sperret dag`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.ikkeUtfyltMeldekort!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        navkontor = ObjectMother.navkontor(),
                        dager = SendMeldekortTilBeslutterKommando.Dager(
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
                ) shouldBeLeft KanIkkeSendeMeldekortTilBeslutter.KanIkkeEndreDagFraSperret
            }
        }
    }

    @Test
    fun `Sperret matcher 1 - 1`() {
        val correlationId = CorrelationId.generate()
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(
                    periode = Periode(3.januar(2023), 31.januar(2023)),
                    correlationId = correlationId,
                )
                val ikkeUtfyltMeldekort = sak.meldekortBehandlinger.ikkeUtfyltMeldekort!!
                val førsteDag = ikkeUtfyltMeldekort.fraOgMed
                tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        sakId = sak.id,
                        meldekortId = ikkeUtfyltMeldekort.id,
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = correlationId,
                        navkontor = ObjectMother.navkontor(),
                        dager = SendMeldekortTilBeslutterKommando.Dager(
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
                ).shouldBeRight()
            }
        }
    }

    private fun dager(
        førsteDag: LocalDate,
        vararg statuser: SendMeldekortTilBeslutterKommando.Status,
    ): NonEmptyList<Dag> {
        return dager(førsteDag, statuser.toList())
    }

    private fun dager(
        førsteDag: LocalDate,
        statuser: List<SendMeldekortTilBeslutterKommando.Status>,
    ): NonEmptyList<Dag> {
        return statuser.mapIndexed { index, status ->
            Dag(
                dag = førsteDag.plusDays(index.toLong()),
                status = status,
            )
        }.toNonEmptyListOrNull()!!
    }
}
