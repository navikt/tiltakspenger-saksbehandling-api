package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptySetOrThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Hjemlene lagres som en jsonb-liste med én gren per hjemmel i hver retning.
 * En sak har i praksis én eller to hjemler, så det ville tatt ti iverksettelser gjennom prodstien å nå alle grenene — for en mapping som ikke rører postgres i det hele tatt.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en variant ble omdøpt i begge `when`-ene samtidig — og da er dataen som allerede ligger i databasen ulesbar uten at noe slår ut.
 *
 * Det testen ikke sier noe om, er om hver enkelt hjemmel kan velges gjennom prodstien.
 * En hjemmel ingen prodsti produserer er død kode, og skal fjernes framfor å dekkes.
 */
class HjemmelForOpphørDbTest {

    @Test
    fun `alle opphørshjemlene lagres med sitt avtalte navn`() {
        HjemmelForOpphør.entries.toNonEmptySetOrThrow().toHjemmelForOpphørDbJson() shouldBe
            """["OPPHØR_ALDER","OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK","OPPHØR_FREMMET_FOR_SENT","OPPHØR_IKKE_LOVLIG_OPPHOLD","OPPHØR_INSTITUSJONSOPPHOLD","OPPHØR_INTRODUKSJONSPROGRAMMET","OPPHØR_KVALIFISERINGSPROGRAMMET","OPPHØR_LIVSOPPHOLDSYTELSER","OPPHØR_LØNN_FRA_ANDRE","OPPHØR_LØNN_FRA_TILTAKSARRANGØR"]"""
    }

    @Test
    fun `alle opphørshjemlene leses tilbake fra lagret json`() {
        val alle = HjemmelForOpphør.entries.toNonEmptySetOrThrow()

        alle.toHjemmelForOpphørDbJson().tilHjemmelForOpphør() shouldBe alle
    }
}
