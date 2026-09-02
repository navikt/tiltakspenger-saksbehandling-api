package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.angre.angreBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.avbryt.kanAvbryte
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.kanOppdatere
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.kanSendeTilBeslutning
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Leseroller (veileder, utvikler) skal ikke kunne mutere en rammebehandling, uavhengig av behandlingens tilstand.
 * Domenet håndhever dette selv, slik at route-guardene ikke er eneste barriere.
 */
class LeserolleTilgangRammebehandlingTest {

    private val leseroller
        get() = listOf(
            ObjectMother.saksbehandlerUtenTilgang(),
            ObjectMother.veileder(),
            ObjectMother.utvikler(),
        )

    @Test
    fun `leserolle kan ikke avbryte en rammebehandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> { behandling.kanAvbryte(bruker) }
        }
    }

    @Test
    fun `leserolle kan ikke sende en rammebehandling til beslutning`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> { behandling.kanSendeTilBeslutning(bruker) }
        }
    }

    @Test
    fun `leserolle kan ikke oppdatere en rammebehandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> { behandling.kanOppdatere(bruker) }
        }
    }

    @Test
    fun `leserolle kan ikke angre sending til beslutning`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> { behandling.angreBehandling(bruker, ObjectMother.clock) }
        }
    }

    @Test
    fun `leserolle kan ikke opprette en søknadsbehandling`() {
        val søknad = ObjectMother.nyInnvilgbarSøknad()
        val sak = ObjectMother.nySak(søknader = listOf(søknad))
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                runBlocking {
                    Søknadsbehandling.opprett(
                        sak = sak,
                        søknad = søknad,
                        saksbehandler = bruker,
                        hentSaksopplysninger = { _, _, _, _, _ -> error("Skal ikke kalles") },
                        correlationId = CorrelationId.generate(),
                        klagebehandling = null,
                        clock = ObjectMother.clock,
                    )
                }
            }
        }
    }

    @Test
    fun `leserolle kan ikke starte en revurdering`() {
        val sak = ObjectMother.nySak()
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                runBlocking {
                    sak.startRevurdering(
                        kommando = StartRevurderingKommando(
                            sakId = SakId.random(),
                            correlationId = CorrelationId.generate(),
                            saksbehandler = bruker,
                            revurderingType = StartRevurderingType.STANS,
                            vedtakIdSomOmgjøres = null,
                            klagebehandlingId = null,
                        ),
                        clock = ObjectMother.clock,
                        hentSaksopplysninger = { _, _, _, _, _ -> error("Skal ikke kalles") },
                    )
                }
            }
        }
    }
}
