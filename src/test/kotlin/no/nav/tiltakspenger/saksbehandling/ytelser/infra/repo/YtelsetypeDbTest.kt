package no.nav.tiltakspenger.saksbehandling.ytelser.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import org.junit.jupiter.api.Test

/** Kodene ligger i lagrede saksopplysninger og utbetalingsoversikter, så de pinnes som faste strenger. */
class YtelsetypeDbTest {
    @Test
    fun `kodene er faste`() {
        Ytelsetype.entries.associateWith { it.toDb() } shouldBe mapOf(
            Ytelsetype.AAP to "AAP",
            Ytelsetype.AFP to "AFP",
            Ytelsetype.ALDERSPENSJON to "ALDERSPENSJON",
            Ytelsetype.DAGPENGER to "DAGPENGER",
            Ytelsetype.ENSLIG_FORSORGER to "ENSLIG_FORSORGER",
            Ytelsetype.ETTERLATTEPENSJON to "ETTERLATTEPENSJON",
            Ytelsetype.FORELDREPENGER to "FORELDREPENGER",
            Ytelsetype.FORELDREPENREFARBGIV to "FORELDREPENREFARBGIV",
            Ytelsetype.KOMPENSASJON to "KOMPENSASJON",
            Ytelsetype.KRIGSPENSJON to "KRIGSPENSJON",
            Ytelsetype.LONNSKOMPENSASJON to "LONNSKOMPENSASJON",
            Ytelsetype.OMSORGSPENGER to "OMSORGSPENGER",
            Ytelsetype.OMSTILLINGSSTONAD to "OMSTILLINGSSTONAD",
            Ytelsetype.OPPLAERINGSPENGER to "OPPLAERINGSPENGER",
            Ytelsetype.PEN_REINDRIFTSSAMER to "PEN_REINDRIFTSSAMER",
            Ytelsetype.PLEIEPENGER to "PLEIEPENGER",
            Ytelsetype.PLEIEPENGERREFARBGIV to "PLEIEPENGERREFARBGIV",
            Ytelsetype.REFUSJON to "REFUSJON",
            Ytelsetype.REFUSJON_PST to "REFUSJON_PST",
            Ytelsetype.REFUSJON_SPK to "REFUSJON_SPK",
            Ytelsetype.REFUSJONSKRAV to "REFUSJONSKRAV",
            Ytelsetype.SPK to "SPK",
            Ytelsetype.SUPPLERENDE_STONAD to "SUPPLERENDE_STONAD",
            Ytelsetype.SVANGERSKAPSPENGER to "SVANGERSKAPSPENGER",
            Ytelsetype.SYKEPENGER to "SYKEPENGER",
            Ytelsetype.SYKEPENGERREFARBGIV to "SYKEPENGERREFARBGIV",
            Ytelsetype.TJENESTEPENSJON to "TJENESTEPENSJON",
            Ytelsetype.TILTAKSPENGER to "TILTAKSPENGER",
            Ytelsetype.UFOREPENSJON to "UFOREPENSJON",
            Ytelsetype.UFORETRYGD to "UFORETRYGD",
            Ytelsetype.UNGDOMSPROGRAMYTELSE to "UNGDOMSPROGRAMYTELSE",
        )
    }

    @Test
    fun `kodene leses tilbake`() {
        Ytelsetype.entries.forEach { it.toDb().tilYtelsetypeFraDb() shouldBe it }
    }
}
