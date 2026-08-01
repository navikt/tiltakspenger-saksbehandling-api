package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptySetOrThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt** — samme begrunnelse som [HjemmelForOpphørDbTest].
 * Hjemlene lagres som en jsonb-liste med én gren per hjemmel i hver retning, og en stans har i praksis én eller to.
 *
 * Testen pinner den faktiske json-en, ikke bare rundturen, slik at en omdøpt variant ikke kan slippe gjennom ved at begge `when`-ene endres samtidig.
 */
class HjemmelForStansDbTest {

    @Test
    fun `alle stanshjemlene lagres med sitt avtalte navn`() {
        HjemmelForStans.entries.toNonEmptySetOrThrow().toHjemmelForStansDbJson() shouldBe
            """["STANS_ALDER","STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK","STANS_IKKE_LOVLIG_OPPHOLD","STANS_INSTITUSJONSOPPHOLD","STANS_INTRODUKSJONSPROGRAMMET","STANS_KVALIFISERINGSPROGRAMMET","STANS_LIVSOPPHOLDSYTELSER","STANS_LØNN_FRA_ANDRE","STANS_LØNN_FRA_TILTAKSARRANGØR"]"""
    }

    @Test
    fun `alle stanshjemlene leses tilbake fra lagret json`() {
        val alle = HjemmelForStans.entries.toNonEmptySetOrThrow()

        alle.toHjemmelForStansDbJson().tilHjemmelForStans() shouldContainExactlyInAnyOrder alle
    }

    /** Ingen hjemler lagres som en tom liste, ikke som `null` — lesestien forventer alltid gyldig json. */
    @Test
    fun `uten hjemler lagres en tom liste`() {
        null.toHjemmelForStansDbJson() shouldBe "[]"
    }
}
