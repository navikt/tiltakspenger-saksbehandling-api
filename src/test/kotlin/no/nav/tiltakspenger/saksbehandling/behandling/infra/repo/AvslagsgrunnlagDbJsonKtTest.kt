package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import org.junit.jupiter.api.Test

class AvslagsgrunnlagDbJsonKtTest {
    @Test
    fun `serialiserer avslagsgrunnlag til database string`() {
        setOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
            Avslagsgrunnlag.Alder,
            Avslagsgrunnlag.Livsoppholdytelser,
            Avslagsgrunnlag.Kvalifiseringsprogrammet,
            Avslagsgrunnlag.Introduksjonsprogrammet,
            Avslagsgrunnlag.LønnFraTiltaksarrangør,
            Avslagsgrunnlag.LønnFraAndre,
            Avslagsgrunnlag.Institusjonsopphold,
            Avslagsgrunnlag.FremmetForSent,
        ).toDb().shouldEqualJson(
            """
            [
                "AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
                "AVSLAG_ALDER",
                "AVSLAG_LIVSOPPHOLDSYTELSER",
                "AVSLAG_KVALIFISERINGSPROGRAMMET",
                "AVSLAG_INTRODUKSJONSPROGRAMMET",
                "AVSLAG_LØNN_FRA_TILTAKSARRANGØR",
                "AVSLAG_LØNN_FRA_ANDRE",
                "AVSLAG_INSTITUSJONSOPPHOLD",
                "FREMMET_FOR_SENT"
            ]
            """.trimIndent(),
        )
    }

    @Test
    fun `deserialiserer avslagsgrunnlag fra database string`() {
        """
            [
                "AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
                "AVSLAG_ALDER",
                "AVSLAG_LIVSOPPHOLDSYTELSER",
                "AVSLAG_KVALIFISERINGSPROGRAMMET",
                "AVSLAG_INTRODUKSJONSPROGRAMMET",
                "AVSLAG_LØNN_FRA_TILTAKSARRANGØR",
                "AVSLAG_LØNN_FRA_ANDRE",
                "AVSLAG_INSTITUSJONSOPPHOLD",
                "FREMMET_FOR_SENT"
            ]
        """.trimIndent().toAvslagsgrunnlag() shouldBe setOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
            Avslagsgrunnlag.Alder,
            Avslagsgrunnlag.Livsoppholdytelser,
            Avslagsgrunnlag.Kvalifiseringsprogrammet,
            Avslagsgrunnlag.Introduksjonsprogrammet,
            Avslagsgrunnlag.LønnFraTiltaksarrangør,
            Avslagsgrunnlag.LønnFraAndre,
            Avslagsgrunnlag.Institusjonsopphold,
            Avslagsgrunnlag.FremmetForSent,
        )
    }
}
