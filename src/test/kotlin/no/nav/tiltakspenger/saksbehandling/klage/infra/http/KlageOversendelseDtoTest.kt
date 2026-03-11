package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel
import org.junit.jupiter.api.Test

class KlageOversendelseDtoTest {
    @Test
    fun `mapper klagehjemmel til DTO riktig`() {
        Klagehjemmel::class.sealedSubclasses.mapNotNull { it.objectInstance }.forEach {
            when (it) {
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13 -> it.tilKlageOversendelseDto() shouldBe "ARBML_2"
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4 -> it.tilKlageOversendelseDto() shouldBe "ARBML_13_4"
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_LØNN -> it.tilKlageOversendelseDto() shouldBe "ARBML_13_LOENN"
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15 -> it.tilKlageOversendelseDto() shouldBe "ARBML_15"
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17 -> it.tilKlageOversendelseDto() shouldBe "ARBML_17"
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2 -> it.tilKlageOversendelseDto() shouldBe "ARBML_2"
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22 -> it.tilKlageOversendelseDto() shouldBe "ARBML_22"
                Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15 -> it.tilKlageOversendelseDto() shouldBe "FTRL_22_15"
                Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A -> it.tilKlageOversendelseDto() shouldBe "FTRL_22_17A"
                Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_10 -> it.tilKlageOversendelseDto() shouldBe "FORELDELSESLOVEN_10"
                Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3 -> it.tilKlageOversendelseDto() shouldBe "FORELDELSESLOVEN_2_OG_3"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11 -> it.tilKlageOversendelseDto() shouldBe "FVL_11"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17 -> it.tilKlageOversendelseDto() shouldBe "FVL_17"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19 -> it.tilKlageOversendelseDto() shouldBe "FVL_18_19"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28 -> it.tilKlageOversendelseDto() shouldBe "FVL_28"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30 -> it.tilKlageOversendelseDto() shouldBe "FVL_30"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31 -> it.tilKlageOversendelseDto() shouldBe "FVL_31"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32 -> it.tilKlageOversendelseDto() shouldBe "FVL_32"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35 -> it.tilKlageOversendelseDto() shouldBe "FVL_35"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41 -> it.tilKlageOversendelseDto() shouldBe "FVL_41"
                Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42 -> it.tilKlageOversendelseDto() shouldBe "FVL_42"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_10"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_11"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_2"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_3"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_5"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_6"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_7"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_8"
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9 -> it.tilKlageOversendelseDto() shouldBe "FS_TIP_9"
            }
        }
    }
}
