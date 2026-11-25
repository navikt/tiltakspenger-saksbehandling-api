package no.nav.tiltakspenger.saksbehandling.saksbehandler

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.util.UUID

class RolleForStrengtFortroligAdresseServiceTest {

    private val gruppeId = UUID.randomUUID()
    private val entraProxyClient = EntraProxyFakeClient()
    private val service = RolleForStrengtFortroligAdresseService(entraProxyClient, gruppeId)

    @Test
    fun `har rollen når identen er medlem av gruppen`() = runTest {
        entraProxyClient.leggTil("Z111111", EntraGruppe(UUID.randomUUID(), "annen"), EntraGruppe(gruppeId, "strengt fortrolig"))

        service.harRolleForStrengtFortroligAdresse("Z111111") shouldBe true.right()
    }

    @Test
    fun `har ikke rollen når identen bare er medlem av andre grupper`() = runTest {
        entraProxyClient.leggTil("Z222222", EntraGruppe(UUID.randomUUID(), "annen"))

        service.harRolleForStrengtFortroligAdresse("Z222222") shouldBe false.right()
    }

    @Test
    fun `gir feilen videre når oppslaget feiler`() = runTest {
        val feil = KanIkkeHenteGrupper.KallFeilet(ObjectMother.httpKlientUventetStatus())
        entraProxyClient.feilFor("Z333333", feil)

        service.harRolleForStrengtFortroligAdresse("Z333333") shouldBe feil.left()
    }
}
