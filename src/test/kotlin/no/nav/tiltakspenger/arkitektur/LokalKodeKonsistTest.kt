package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Håndhever at koden for lokal kjøring i `lokal`-pakka ikke brukes av testene.
 *
 * `lokal` inneholder `LokalMain`, fakes, dev-endepunkter og tabeller som kun finnes ved lokal kjøring, og ligger i testkildene fordi den aldri skal havne i prod-artefaktet.
 * Testene skal gå mot prodstien og testfakene, ikke mot den lokale infrastrukturen.
 * Uten en regel er det fristende å gjenbruke en lokal fake eller hjelper i en test, og da blir lokal kjøring plutselig en del av det testene verifiserer.
 * Avhengigheten går bare én vei: `lokal` kan bruke testinfrastrukturen, men testene kan ikke bruke `lokal`.
 */
class LokalKodeKonsistTest {

    @Test
    fun `tester refererer ikke til lokal`() {
        assertIngenBrudd(
            testfiler()
                .filterNot { it.iLokal }
                .flatMap { fil -> fil.lokalreferanser().map { "${fil.file.path}: refererer til $it" } },
            "`$LOKALPAKKE` er kode for lokal kjøring og skal ikke brukes av testene. Bruk testfakene og prodstien, eller flytt det som deles ut av `lokal`.",
        )
    }

    private data class Testfil(val file: KoFileDeclaration, val pakke: String) {
        val iLokal: Boolean = pakke == LOKALPAKKE || pakke.startsWith("$LOKALPAKKE.")

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

    private fun testfiler(): List<Testfil> =
        Konsist.scopeFromTest().kildefiler()
            .filterNot { it.path.iEtArbeidstre() }
            .map { Testfil(file = it, pakke = it.packagee?.name.orEmpty()) }

    /**
     * Et git-arbeidstre som ligger under repo-rota er en egen utsjekk av det samme repoet.
     * Konsist walker inn i det, og regelen ville da rapportert brudd på filer som ikke hører til utsjekken vi kjører i.
     *
     * Sjekken er relativ til arbeidskatalogen, ikke absolutt.
     * Kjører du bygget *inne* i et arbeidstre, er arbeidskatalogen selve arbeidstreet, og da skal regelen kjøre som vanlig — det er kun arbeidstrær under oss som skal utelates.
     */
    private fun String.iEtArbeidstre(): Boolean {
        val relativ = runCatching { arbeidskatalog.relativize(Path.of(this)) }.getOrNull() ?: return false
        return relativ.any { segment -> segment.toString() in arbeidstrekataloger }
    }

    private val arbeidskatalog: Path = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()

    private val arbeidstrekataloger = setOf(".worktrees", ".worktree")

    private companion object {
        const val LOKALPAKKE = "no.nav.tiltakspenger.saksbehandling.lokal"
        val BLOKKOMMENTAR = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val FULLT_KVALIFISERT_LOKALREFERANSE = Regex("""\bno\.nav\.tiltakspenger\.saksbehandling\.lokal\.[\wæøåÆØÅ.]+""")
    }
}
