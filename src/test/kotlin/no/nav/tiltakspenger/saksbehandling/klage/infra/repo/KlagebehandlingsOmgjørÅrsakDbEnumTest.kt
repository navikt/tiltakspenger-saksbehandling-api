package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Hver årsak ville krevd sin egen klagebehandling gjennom vurder-ruta for å nås fra en prodsti, og mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en årsak ble omdøpt i begge `when`-ene samtidig, og da er lagrede rader ulesbare uten at noe slår ut.
 */
class KlagebehandlingsOmgjørÅrsakDbEnumTest {

    @Test
    fun `årsakene lagres med sitt avtalte navn`() {
        KlageOmgjøringsårsak.entries.associateWith { it.toDbEnum().name } shouldBe mapOf(
            KlageOmgjøringsårsak.FEIL_LOVANVENDELSE to "FEIL_LOVANVENDELSE",
            KlageOmgjøringsårsak.FEIL_REGELVERKSFORSTAAELSE to "FEIL_REGELVERKSFORSTAAELSE",
            KlageOmgjøringsårsak.FEIL_ELLER_ENDRET_FAKTA to "FEIL_ELLER_ENDRET_FAKTA",
            KlageOmgjøringsårsak.PROSESSUELL_FEIL to "PROSESSUELL_FEIL",
            KlageOmgjøringsårsak.ANNET to "ANNET",
        )
    }

    @Test
    fun `årsakene leses tilbake fra lagret verdi`() {
        KlageOmgjøringsårsak.entries.forEach {
            it.toDbEnum().name.toKlageOmgjøringsårsak() shouldBe it
        }
    }
}
