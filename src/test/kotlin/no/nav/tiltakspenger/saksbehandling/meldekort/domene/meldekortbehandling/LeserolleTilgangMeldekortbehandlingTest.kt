package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.kotest.assertions.throwables.shouldThrow
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.kanAvbryte
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Leseroller (veileder, utvikler) skal ikke kunne mutere en meldekortbehandling, uavhengig av behandlingens tilstand.
 * Domenet håndhever dette selv, slik at route-guardene ikke er eneste barriere.
 */
class LeserolleTilgangMeldekortbehandlingTest {

    private val leseroller
        get() = listOf(
            ObjectMother.saksbehandlerUtenTilgang(),
            ObjectMother.veileder(),
            ObjectMother.utvikler(),
        )

    @Test
    fun `leserolle kan ikke avbryte en meldekortbehandling`() {
        val behandling = ObjectMother.meldekortUnderBehandling()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> { behandling.kanAvbryte(bruker) }
        }
    }

    @Test
    fun `leserolle kan ikke sende en meldekortbehandling til beslutning`() {
        val behandling = ObjectMother.meldekortUnderBehandling()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                behandling.sendTilBeslutter(
                    kommando = ObjectMother.sendMeldekortTilBeslutterKommando(
                        sakId = behandling.sakId,
                        meldekortId = behandling.id,
                        saksbehandler = bruker,
                    ),
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke iverksette en meldekortbehandling`() {
        val behandling = ObjectMother.meldekortBehandletManuelt()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                behandling.iverksettMeldekort(
                    beslutter = bruker,
                    clock = ObjectMother.clock,
                    correlationId = CorrelationId.generate(),
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke underkjenne en meldekortbehandling`() {
        val behandling = ObjectMother.meldekortBehandletManuelt()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                behandling.underkjenn(
                    besluttersBegrunnelse = "begrunnelse".toNonBlankString(),
                    beslutter = bruker,
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke opprette en manuell meldekortbehandling`() {
        val sak = ObjectMother.nySak()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                sak.opprettManuellMeldekortbehandling(
                    kjedeId = MeldeperiodeKjedeId.fraPeriode(
                        Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19)),
                    ),
                    navkontor = ObjectMother.navkontor(),
                    saksbehandler = bruker,
                    klagebehandlingId = null,
                    clock = ObjectMother.clock,
                )
            }
        }
    }
}
