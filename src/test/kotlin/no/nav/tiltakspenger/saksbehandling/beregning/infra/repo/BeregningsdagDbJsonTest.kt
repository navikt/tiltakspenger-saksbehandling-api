package no.nav.tiltakspenger.saksbehandling.beregning.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * `antallBarn` og `beløpBarnetillegg` er null i rader skrevet før feltene fantes, mens dagens skrivesti alltid setter begge.
 * Null-grenene kan dermed bare nås fra lagret json, ikke gjennom prodstiene — og mappingen rører ikke postgres.
 */
class BeregningsdagDbJsonTest {

    @Test
    fun `en dag lagret uten barnetillegg-feltene leses som null barn og null kroner i barnetillegg`() {
        val gammelDag = BeregningsdagDbJson(
            beløp = 285,
            prosent = 100,
            satsdag = SatsdagDbJson(
                sats = 285,
                satsRedusert = 214,
                satsBarnetillegg = 53,
                satsBarnetilleggRedusert = 40,
                dato = 6.januar(2025),
            ),
            dato = 6.januar(2025),
            antallBarn = null,
            beløpBarnetillegg = null,
        )

        val dag = gammelDag.toBeregningsdag()

        dag.antallBarn shouldBe AntallBarn.ZERO
        dag.beløpBarnetillegg shouldBe 0
    }
}
