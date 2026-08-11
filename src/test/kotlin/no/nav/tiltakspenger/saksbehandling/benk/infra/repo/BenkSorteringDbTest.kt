package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.SorteringRetning
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Sorteringsvalget blir til et `order by`-ledd, med én gren per kolonne.
 * Én benk-forespørsel har én sortering, så det ville tatt atten route-kall å nå alle grenene — for en mapping som ikke rører postgres.
 *
 * Testen pinner strengene som settes inn i spørringen.
 * Det er ikke lagret data, men navnene må matche kolonnene og aliasene i `BenkOversiktPostgresRepo`.
 * Blir de usynkrone, feiler spørringen først i produksjon.
 */
class BenkSorteringDbTest {

    @Test
    fun `sorteringskolonnene blir til kolonnenavnene i spørringen`() {
        BenkSorteringKolonne.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkSorteringKolonne.STARTET to "startet",
            BenkSorteringKolonne.SIST_ENDRET to "sist_endret",
            BenkSorteringKolonne.FRIST to "sattPåVentFrist",
            BenkSorteringKolonne.FNR to "fnr",
            BenkSorteringKolonne.BEHANDLINGSTYPE to "behandlingstype",
            BenkSorteringKolonne.STATUS to "status",
            BenkSorteringKolonne.SAKSBEHANDLER to "saksbehandler",
            BenkSorteringKolonne.BESLUTTER to "beslutter",
            BenkSorteringKolonne.BELØP to "beløp",
            BenkSorteringKolonne.RESULTAT to "resultat",
        )
    }

    @Test
    fun `sorteringsretningene blir til ASC og DESC`() {
        SorteringRetning.entries.associateWith { it.toDbString() } shouldBe mapOf(
            SorteringRetning.ASC to "ASC",
            SorteringRetning.DESC to "DESC",
        )
    }
}
