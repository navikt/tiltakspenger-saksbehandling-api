package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AvslagsgrunnlagTest {

    @Test
    fun `DeltarIkkePåArbeidsmarkedstiltak er lenket til riktig hjemler`() {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak.hjemler.let {
            it.size shouldBe 2

            it.first().paragraf shouldBe Paragraf(2)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe null

            it.last().paragraf shouldBe Paragraf(13)
            it.last().forskrift shouldBe Forskrift.Arbeidsmarkedsloven
            it.last().ledd shouldBe null
        }
    }

    @Test
    fun `Alder er lenket til riktig hjemler`() {
        Avslagsgrunnlag.Alder.hjemler.let {
            it.size shouldBe 1

            it.first().paragraf shouldBe Paragraf(3)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe null
        }
    }

    @Test
    fun `Livsoppholdytelser er lenket til riktig hjemler`() {
        Avslagsgrunnlag.Livsoppholdytelser.hjemler.let {
            it.size shouldBe 2

            it.first().paragraf shouldBe Paragraf(7)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe Ledd(1)

            it.last().paragraf shouldBe Paragraf(13)
            it.last().forskrift shouldBe Forskrift.Arbeidsmarkedsloven
            it.last().ledd shouldBe Ledd(1)
        }
    }

    @Test
    fun `Kvalifiseringsprogrammet er lenket til riktig hjemler`() {
        Avslagsgrunnlag.Kvalifiseringsprogrammet.hjemler.let {
            it.size shouldBe 1

            it.first().paragraf shouldBe Paragraf(7)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe Ledd(3)
        }
    }

    @Test
    fun `Introduksjonsprogrammet er lenket til riktig hjemler`() {
        Avslagsgrunnlag.Introduksjonsprogrammet.hjemler.let {
            it.size shouldBe 1

            it.first().paragraf shouldBe Paragraf(7)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe Ledd(3)
        }
    }

    @Test
    fun `LønnFraTiltaksarrangør er lenket til riktig hjemler`() {
        Avslagsgrunnlag.LønnFraTiltaksarrangør.hjemler.let {
            it.size shouldBe 1

            it.first().paragraf shouldBe Paragraf(8)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe null
        }
    }

    @Test
    fun `LønnFraAndre er lenket til riktig hjemler`() {
        Avslagsgrunnlag.LønnFraAndre.hjemler.let {
            it.size shouldBe 2

            it.first().paragraf shouldBe Paragraf(13)
            it.first().forskrift shouldBe Forskrift.Arbeidsmarkedsloven
            it.first().ledd shouldBe null
            it.last().paragraf shouldBe Paragraf(8)
            it.last().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.last().ledd shouldBe Ledd(2)
        }
    }

    @Test
    fun `Institusjonsopphold er lenket til riktig hjemler`() {
        Avslagsgrunnlag.Institusjonsopphold.hjemler.let {
            it.size shouldBe 1

            it.first().paragraf shouldBe Paragraf(9)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe null
        }
    }

    @Test
    fun `FremmetForSent er lenket til riktig hjemler`() {
        Avslagsgrunnlag.FremmetForSent.hjemler.let {
            it.size shouldBe 2

            it.first().paragraf shouldBe Paragraf(11)
            it.first().forskrift shouldBe Forskrift.Tiltakspengeforskriften
            it.first().ledd shouldBe null

            it.last().paragraf shouldBe Paragraf(15)
            it.last().forskrift shouldBe Forskrift.Arbeidsmarkedsloven
            it.last().ledd shouldBe null
        }
    }
}
