package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptySetOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import org.junit.jupiter.api.Test

class ValgtHjemmelForStansDbTest {

    @Test
    fun `deserialiserer ValgtHjemmelForStans`() {
        """["STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"]""".tilHjemmelForStans() shouldBe listOf(
            HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak,
        )
    }

    @Test
    fun `deserialiserer ValgtHjemmelForOpphør`() {
        """["OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"]""".tilHjemmelForOpphør() shouldBe listOf(
            HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak,
        )
    }

    @Test
    fun `deserialiserer ValgtHjemmelForAvslag`() {
        """[ "AVSLAG_ALDER"]""".toAvslagsgrunnlag() shouldBe listOf(Avslagsgrunnlag.Alder)
    }

    @Test
    fun `deserialiserer flere ValgtHjemmelForStans`() {
        """["STANS_ALDER", "STANS_INSTITUSJONSOPPHOLD"]""".tilHjemmelForStans() shouldBe listOf(
            HjemmelForStans.Alder,
            HjemmelForStans.Institusjonsopphold,
        )
    }

    @Test
    fun `deserialisering tomt array for stans`() {
        "[]".tilHjemmelForStans() shouldBe emptyList()
    }

    @Test
    fun `skal ikke ha tomt array for avslag`() {
        shouldThrow<NullPointerException> { "[]".toAvslagsgrunnlag() }
    }

    @Test
    fun `serialiserer tom liste`() {
        null.toHjemmelForStansDbJson() shouldBe "[]"
    }

    @Test
    fun `serialiserer liste med ValgtHjemmelForStans`() {
        nonEmptySetOf(HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak).toHjemmelForStansDbJson() shouldBe """["STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"]"""
    }

    @Test
    fun `serialiserer liste med ValgtHjemmelForOpphør`() {
        nonEmptySetOf(HjemmelForOpphør.FremmetForSent).toHjemmelForOpphørDbJson() shouldBe """["OPPHØR_FREMMET_FOR_SENT"]"""
    }

    @Test
    fun `serialiserer set med ValgtHjemmelForAvslag`() {
        setOf(Avslagsgrunnlag.Alder).toDb() shouldBe """["AVSLAG_ALDER"]"""
    }

    @Test
    fun `serialiserer liste med flere elementer`() {
        setOf(
            Avslagsgrunnlag.Alder,
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
        ).toDb() shouldBe """["AVSLAG_ALDER","AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"]"""
    }
}
