package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
import org.junit.jupiter.api.Test

class ValgtHjemmelForStansDbTest {

    @Test
    fun `deserialiserer ValgtHjemmelForStans`() {
        """["STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"]""".tilHjemmelForStans() shouldBe listOf(
            DeltarIkkePåArbeidsmarkedstiltak,
        )
    }

    @Test
    fun `deserialiserer ValgtHjemmelForAvslag`() {
        """[ "AVSLAG_ALDER"]""".toAvslagsgrunnlag() shouldBe listOf(Avslagsgrunnlag.Alder)
    }

    @Test
    fun `deserialiserer flere ValgtHjemmelForStans`() {
        """["STANS_ALDER", "STANS_INSTITUSJONSOPPHOLD"]""".tilHjemmelForStans() shouldBe listOf(
            ValgtHjemmelForStans.Alder,
            ValgtHjemmelForStans.Institusjonsopphold,
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
        emptyList<ValgtHjemmelForStans>().toDbJson() shouldBe "[]"
    }

    @Test
    fun `serialiserer liste med ValgtHjemmelForStans`() {
        listOf(DeltarIkkePåArbeidsmarkedstiltak).toDbJson() shouldBe """["STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"]"""
    }

    @Test
    fun `serialiserer set med ValgtHjemmelForAvslag`() {
        setOf(Avslagsgrunnlag.Alder).toDb() shouldBe """["AVSLAG_ALDER"]"""
    }

    @Test
    fun `serialiserer liste med flere elementer`() {
        setOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
            Avslagsgrunnlag.Alder,
        ).toDb() shouldBe """["AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK","AVSLAG_ALDER"]"""
    }
}
