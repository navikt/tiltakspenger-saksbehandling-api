package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

class SaksstatistikkSkjermingTest {

    private val fnr = Fnr.random()

    private fun skjermingsoppslag(kode6: Boolean, vikafossenIdenter: Set<String> = emptySet()) = object : Skjermingsoppslag {
        override suspend fun gjelderKode6(fnr: Fnr) = kode6

        override suspend fun harRolleForStrengtFortroligAdresse(navIdent: String) = navIdent in vikafossenIdenter
    }

    @Test
    fun `uten strengt fortrolig adresse sendes identene og Nav Tiltak Oslo`() = runTest {
        skjermingsoppslag(kode6 = false).ansvar(fnr, saksbehandler = "Z111111", ansvarligBeslutter = "Z222222") shouldBe
            SaksstatistikkAnsvar(opprettetAv = "system", saksbehandler = "Z111111", ansvarligBeslutter = "Z222222", ansvarligenhet = "0387")
    }

    @Test
    fun `strengt fortrolig adresse hos Vikafossen sender fiktiv ident og enhet 2103`() = runTest {
        skjermingsoppslag(kode6 = true, vikafossenIdenter = setOf("Z111111")).ansvar(fnr, saksbehandler = "Z111111", ansvarligBeslutter = "Z222222") shouldBe
            SaksstatistikkAnsvar(opprettetAv = SKJERMET, saksbehandler = "Z111111", ansvarligBeslutter = SKJERMET, ansvarligenhet = "2103")
    }

    @Test
    fun `strengt fortrolig adresse utenfor Vikafossen skjermer ident og enhet`() = runTest {
        skjermingsoppslag(kode6 = true).ansvar(fnr, saksbehandler = "Z111111", ansvarligBeslutter = "Z222222") shouldBe
            SaksstatistikkAnsvar(opprettetAv = SKJERMET, saksbehandler = SKJERMET, ansvarligBeslutter = SKJERMET, ansvarligenhet = SKJERMET)
    }

    @Test
    fun `strengt fortrolig adresse uten saksbehandler skjermer enheten og lar tomme felter være tomme`() = runTest {
        skjermingsoppslag(kode6 = true, vikafossenIdenter = setOf("Z222222")).ansvar(fnr, saksbehandler = null, ansvarligBeslutter = null) shouldBe
            SaksstatistikkAnsvar(opprettetAv = SKJERMET, saksbehandler = null, ansvarligBeslutter = null, ansvarligenhet = SKJERMET)
    }
}
