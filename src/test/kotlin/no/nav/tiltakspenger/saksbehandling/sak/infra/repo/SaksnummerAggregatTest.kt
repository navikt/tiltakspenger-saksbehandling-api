package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for tildeling av saksnummer, jf. testtaksonomien i `AGENTS.md`.
 *
 * `SakPostgresRepo.hentNesteSaksnummer` slår opp høyeste løpenummer for dagens prefiks på tvers av alle saker.
 * Den er derfor avhengig av hvilke saker som finnes i databasen, og må kjøre isolert.
 *
 * Klokka står på 1. januar 2021 med vilje: [no.nav.tiltakspenger.libs.common.SaksnummerGeneratorForTest] lager alltid saksnummer med prefikset for den datoen, mens prodkoden slår opp på prefikset for dagens dato.
 * Med en annen klokke finner oppslaget aldri noe, og vi ville testet fallbacken til generatoren i stedet for den prodstien som faktisk teller opp løpenummeret.
 */
class SaksnummerAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `neste saksnummer teller videre fra høyeste løpenummer for dagens prefiks`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2021)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            // Databasen er tom, så den første saken får saksnummeret sitt fra generatoren.
            val første = hentEllerOpprettSakForSystembruker(tac = tac, fnr = ObjectMother.gyldigFnr())

            // Den andre saken finner den første i databasen og teller løpenummeret opp derfra.
            val andre = hentEllerOpprettSakForSystembruker(tac = tac, fnr = ObjectMother.gyldigFnr())
            andre shouldBe første.nesteSaksnummer()

            // Neste ledige saksnummer følger den sist tildelte, ikke generatoren.
            tac.sakContext.sakRepo.hentNesteSaksnummer() shouldBe andre.nesteSaksnummer()
        }
    }
}
