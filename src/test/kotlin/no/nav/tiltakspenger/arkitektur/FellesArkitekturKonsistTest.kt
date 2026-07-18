package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import no.nav.tiltakspenger.libs.konsist.EnSetningPerLinje
import no.nav.tiltakspenger.libs.konsist.IngenJUnit4
import no.nav.tiltakspenger.libs.konsist.IngenJackson2
import no.nav.tiltakspenger.libs.konsist.IngenJupiterAsserts
import no.nav.tiltakspenger.libs.konsist.IngenLokaleJacksonMappere
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Kjører de delte arkitekturreglene fra tiltakspenger-libs (`konsist-regler`) på dette repoet.
 *
 * Bevisst ikke aktivert ennå (målt 2026-07-17, se navikt/tiltakspenger#30):
 * `InfraImport` har 75 brudd og `BoundaryKlasser` 12 — lagdelingsgjeld som krever arkitekturbeslutninger, ikke mekanisk fiks.
 * `DomeneImportWhitelist` venter på de to over (75 distinkte importprefikser i domenet i dag).
 */
class FellesArkitekturKonsistTest {
    @Test
    fun `produksjonskode bruker Jackson 3, ikke Jackson 2`() {
        IngenJackson2.assert(Konsist.scopeFromProduction())
    }

    @Test
    fun `testkode bruker JUnit 5, ikke JUnit 4`() {
        IngenJUnit4.assert(Konsist.scopeFromTest())
    }

    @Test
    fun `testkode bruker Kotest assertions, ikke Jupiter Assertions`() {
        IngenJupiterAsserts.assert(Konsist.scopeFromTest())
    }

    @Test
    fun `ingen lokale Jackson-mappere — bruk objectMapper fra libs-json`() {
        IngenLokaleJacksonMappere.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `kdoc og kommentarer har maks en setning per linje`() {
        EnSetningPerLinje.assertFlereSetningerIKommentarer(Konsist.scopeFromProject())
    }

    @Test
    fun `kdoc og kommentarer brekker ikke en setning over flere linjer`() {
        EnSetningPerLinje.assertBrukneSetningerIKommentarer(Konsist.scopeFromProject())
    }

    @Test
    fun `markdown-filer har maks en setning per linje`() {
        EnSetningPerLinje.assertFlereSetningerIMarkdown(repoRot())
    }

    @Test
    fun `markdown-filer brekker ikke en setning over flere linjer`() {
        EnSetningPerLinje.assertBrukneSetningerIMarkdown(repoRot())
    }

    /** Enmodul-repo: testens arbeidskatalog er repo-rota. */
    private fun repoRot(): Path = Path.of(System.getProperty("user.dir"))
}
