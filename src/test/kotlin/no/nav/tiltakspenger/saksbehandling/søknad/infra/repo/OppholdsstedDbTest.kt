package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Oppholdssted
import org.junit.jupiter.api.Test

/**
 * Pinner de faktiske strengene i `oppholdssted`-kolonnen, ikke bare rundturen.
 * En ren rundtur ville passert selv om en variant ble omdøpt i begge retninger samtidig, og da blir lagrede søknader ulesbare.
 */
class OppholdsstedDbTest {

    @Test
    fun `oppholdssted lagres med sitt avtalte navn`() {
        Oppholdssted.NORGE.toDbValue() shouldBe "NORGE"
        Oppholdssted.ANNET_EØS_LAND.toDbValue() shouldBe "ANNET_EØS_LAND"
        Oppholdssted.IKKE_EØS_LAND.toDbValue() shouldBe "IKKE_EØS_LAND"
    }

    @Test
    fun `oppholdssted leses tilbake fra lagret verdi`() {
        "NORGE".toOppholdssted() shouldBe Oppholdssted.NORGE
        "ANNET_EØS_LAND".toOppholdssted() shouldBe Oppholdssted.ANNET_EØS_LAND
        "IKKE_EØS_LAND".toOppholdssted() shouldBe Oppholdssted.IKKE_EØS_LAND
    }

    @Test
    fun `ukjent oppholdssted gir feil`() {
        shouldThrow<IllegalArgumentException> {
            "SVERIGE".toOppholdssted()
        }
    }
}
