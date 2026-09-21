package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsfeiltype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import org.junit.jupiter.api.Test

/**
 * Enhetstest i stedet for databasetest fordi mappingen ikke rører Postgres, og hver verdi ellers ville krevd sin egen konstruerte feil gjennom jobben.
 * Testen sier ikke at hver verdi kan oppstå; det viser jobbtestene for dem som kan.
 */
class UtbetalingsoversiktDbTest {
    @Test
    fun `periodetypene lagres med disse strengene`() {
        Oppslagsperiodetype.entries.associateWith { it.toDb() } shouldBe mapOf(
            Oppslagsperiodetype.UTBETALINGSPERIODE to "UTBETALINGSPERIODE",
            Oppslagsperiodetype.YTELSESPERIODE to "YTELSESPERIODE",
        )
    }

    @Test
    fun `lagrede periodetyper leses tilbake`() {
        "UTBETALINGSPERIODE".toOppslagsperiodetype() shouldBe Oppslagsperiodetype.UTBETALINGSPERIODE
        "YTELSESPERIODE".toOppslagsperiodetype() shouldBe Oppslagsperiodetype.YTELSESPERIODE
    }

    @Test
    fun `resultatene lagres med disse strengene og leses tilbake`() {
        OppslagsresultatDb.entries.associateWith { it.toDb() } shouldBe mapOf(
            OppslagsresultatDb.VELLYKKET to "VELLYKKET",
            OppslagsresultatDb.FEILET to "FEILET",
        )
        "VELLYKKET".toOppslagsresultatDb() shouldBe OppslagsresultatDb.VELLYKKET
        "FEILET".toOppslagsresultatDb() shouldBe OppslagsresultatDb.FEILET
    }

    @Test
    fun `feiltypene lagres med disse strengene`() {
        Oppslagsfeiltype.entries.associateWith { it.toDb() } shouldBe mapOf(
            Oppslagsfeiltype.TILGANG_AVVIST to "TILGANG_AVVIST",
            Oppslagsfeiltype.TJENESTEFEIL to "TJENESTEFEIL",
            Oppslagsfeiltype.SAK_AVVIST to "SAK_AVVIST",
            Oppslagsfeiltype.ULESELIG_SVAR to "ULESELIG_SVAR",
            Oppslagsfeiltype.UGYLDIG_INNHOLD to "UGYLDIG_INNHOLD",
        )
    }

    @Test
    fun `lagrede feiltyper leses tilbake`() {
        "TILGANG_AVVIST".toOppslagsfeiltype() shouldBe Oppslagsfeiltype.TILGANG_AVVIST
        "TJENESTEFEIL".toOppslagsfeiltype() shouldBe Oppslagsfeiltype.TJENESTEFEIL
        "SAK_AVVIST".toOppslagsfeiltype() shouldBe Oppslagsfeiltype.SAK_AVVIST
        "ULESELIG_SVAR".toOppslagsfeiltype() shouldBe Oppslagsfeiltype.ULESELIG_SVAR
        "UGYLDIG_INNHOLD".toOppslagsfeiltype() shouldBe Oppslagsfeiltype.UGYLDIG_INNHOLD
    }
}
