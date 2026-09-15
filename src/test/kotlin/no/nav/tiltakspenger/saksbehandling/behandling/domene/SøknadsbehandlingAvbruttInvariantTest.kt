package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.avbryt.avbryt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Verifiserer [Rammebehandlingsstatus.AVBRUTT]-grenen i `init`, som en søknadsbehandling arver fra [Rammebehandling].
 *
 * Statusen og avbruddet er to felter som må følges ad: en behandling er ikke avbrutt uten at vi vet hvem som avbrøt den, når og hvorfor.
 * Vakten fanger en `copy` som setter status uten å sette avbruddet.
 */
class SøknadsbehandlingAvbruttInvariantTest {

    private val saksbehandler = ObjectMother.saksbehandler()

    @Test
    fun `en avbrutt søknadsbehandling må ha et avbrudd`() {
        val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.AVBRUTT
        behandling.avbrutt!!.saksbehandler shouldBe saksbehandler.navIdent
    }

    @Test
    fun `status avbrutt uten avbrudd avvises`() {
        val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

        shouldThrowWithMessage<IllegalArgumentException>(
            "En avbrutt behandling må ha et avbrudd. sakId: ${behandling.sakId}, saksnummer: ${behandling.saksnummer}, rammebehandlingId: ${behandling.id}",
        ) {
            behandling.copy(avbrutt = null)
        }
    }

    /**
     * Avbrytes søknaden sammen med behandlingen, deler de tidspunkt - `avbryt` sender det samme tidspunktet begge veier.
     * Det er koblingen mellom behandlingens avbrudd og hendelsen i søknadens historikk.
     */
    @Test
    fun `søknaden og behandlingen deler tidspunkt når de avbrytes sammen`() {
        val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.søknad.erAvbrutt shouldBe true
        behandling.søknad.avbrutt.single().tidspunkt shouldBe behandling.avbrutt!!.tidspunkt
    }

    /**
     * **Regresjonsvakt.**
     * `Sak.avbrytSøknadOgBehandling` lar søknaden stå urørt når den har en annen behandling som lever - typisk en omgjøring etter klage, der søknaden allerede er innvilget.
     * Behandlingen får da et avbrudd uten at det finnes en hendelse i søknadens historikk, og en invariant som krever tidspunkt-match ville brutt denne prodstien.
     */
    @Test
    fun `en behandling kan avbrytes uten at søknaden avbrytes`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)

        val avbrutt = behandling.avbryt(
            avbruttAv = saksbehandler,
            begrunnelse = "søknaden lever videre på en annen behandling".toNonBlankString(),
            tidspunkt = nå(ObjectMother.clock),
            skalAvbryteSøknad = false,
        ).getOrFail() as Søknadsbehandling

        avbrutt.status shouldBe Rammebehandlingsstatus.AVBRUTT
        avbrutt.avbrutt shouldNotBe null
        avbrutt.søknad.erAvbrutt shouldBe false
        avbrutt.søknad.avbrutt.shouldBeEmpty()
    }
}
