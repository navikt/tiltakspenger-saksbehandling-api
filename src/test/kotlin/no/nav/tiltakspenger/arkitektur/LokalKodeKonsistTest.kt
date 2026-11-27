package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Håndhever at koden for lokal kjøring i `lokal`-pakka ikke brukes av testene eller testfixtures.
 *
 * Modulen `lokal` inneholder `LokalMain`, fakes, dev-endepunkter og tabeller som kun finnes ved lokal kjøring, og holdes utenfor prod- og dev-distribusjonen.
 * Testene skal gå mot produksjonskoden og testfakene, ikke mot den lokale infrastrukturen.
 * Uten en regel er det fristende å gjenbruke en lokal fake eller hjelper i en test, og da blir lokal kjøring plutselig en del av det testene verifiserer.
 * Avhengigheten går bare én vei: `lokal` kan bruke testfixtures, men testene og testfixtures kan ikke bruke `lokal`.
 * Testene har `lokal` på classpath bare ved kjøring, slik at `LocalConfig` kan lastes.
 */
class LokalKodeKonsistTest {

    @Test
    fun `tester og testfixtures refererer ikke til lokal`() {
        assertIngenBrudd(
            testfiler()
                .flatMap { fil -> fil.lokalreferanser().map { "${fil.file.path}: refererer til $it" } },
            "`$LOKALPAKKE` er kode for lokal kjøring og skal ikke brukes av testene eller testfixtures. Bruk testfakene og produksjonskoden, eller flytt det som deles ut av `lokal`.",
        )
    }

    private data class Testfil(val file: KoFileDeclaration) {
        /**
         * Importer fra `lokal`, og fullt kvalifiserte referanser til `lokal` i koden.
         * Kommentarer tas ikke med, slik at KDoc fortsatt kan lenke til den lokale motparten til en testfake.
         */
        fun lokalreferanser(): List<String> {
            val importer = file.imports.map { it.name }.filter { it == LOKALPAKKE || it.startsWith("$LOKALPAKKE.") }
            val fulltKvalifiserte = file.text
                .replace(BLOKKOMMENTAR, "")
                .lineSequence()
                .filterNot { it.trimStart().startsWith("import ") }
                .map { it.substringBefore("//") }
                .flatMap { linje -> FULLT_KVALIFISERT_LOKALREFERANSE.findAll(linje).map { it.value } }
                .toList()
            return importer + fulltKvalifiserte
        }
    }

    /**
     * Bare `src/test` og `src/testFixtures` rett under arbeidskatalogen tas med.
     * Det holder lokalmodulen, andre moduler og arbeidstrær under repo-rota utenfor.
     * Begge katalogene må gi treff, slik at regelen ikke blir grønn uten å ha sjekket noe.
     */
    private fun testfiler(): List<Testfil> {
        val arbeidskatalog = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        val alleFiler = Konsist.scopeFromProject().kildefiler()
        return listOf("src/test", "src/testFixtures").flatMap { katalog ->
            val rot = arbeidskatalog.resolve(katalog)
            alleFiler
                .filter { Path.of(it.path).startsWith(rot) }
                .also { check(it.isNotEmpty()) { "Fant ingen kildefiler under $rot." } }
                .map { Testfil(it) }
        }
    }

    private companion object {
        const val LOKALPAKKE = "no.nav.tiltakspenger.saksbehandling.lokal"
        val BLOKKOMMENTAR = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val FULLT_KVALIFISERT_LOKALREFERANSE = Regex("""\bno\.nav\.tiltakspenger\.saksbehandling\.lokal\.[\wæøåÆØÅ.]+""")
    }
}
