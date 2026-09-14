package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Historikken lagres som en jsonb-liste, og hver variant har sin egen gren i begge retninger.
 * Prodstien produserer én hendelse per kall, så det ville tatt flere avbrytelser og gjenopprettinger å nå alle grenene - for en mapping som ikke rører postgres.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om `type` ble omdøpt i begge retninger samtidig - og da er dataen som allerede ligger i databasen ulesbar uten at noe slår ut.
 * Feltnavnene må også holdes i sync med backfillen i `V249__soknad_hendelser.sql`.
 */
class SøknadshendelseDbJsonTest {

    private val avbrutt = Søknadshendelse.Avbrutt(
        tidspunkt = 1.januar(2025).atTime(12, 0),
        utførtAv = "Z123456",
        begrunnelse = "duplikat".toNonBlankString(),
    )

    private val gjenopprettet = Søknadshendelse.Gjenopprettet(
        tidspunkt = 2.januar(2025).atTime(13, 30),
        utførtAv = "Z654321",
        begrunnelse = "avbrutt ved en feil".toNonBlankString(),
    )

    private val gjenopprettetUtenBegrunnelse = gjenopprettet.copy(begrunnelse = null)

    @Test
    fun `hendelsene lagres med sitt avtalte navn og sine avtalte felter`() {
        no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser(listOf(avbrutt, gjenopprettet, gjenopprettetUtenBegrunnelse)).toDbJson() shouldBe
            """[{"type":"AVBRUTT","tidspunkt":"2025-01-01T12:00","utførtAv":"Z123456","begrunnelse":"duplikat"},""" +
            """{"type":"GJENOPPRETTET","tidspunkt":"2025-01-02T13:30","utførtAv":"Z654321","begrunnelse":"avbrutt ved en feil"},""" +
            """{"type":"GJENOPPRETTET","tidspunkt":"2025-01-02T13:30","utførtAv":"Z654321","begrunnelse":null}]"""
    }

    @Test
    fun `hendelsene leses tilbake fra lagret json`() {
        val alle = listOf(avbrutt, gjenopprettet, gjenopprettetUtenBegrunnelse)

        no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser(alle).toDbJson().toSøknadshendelser().toList() shouldBe alle
    }

    @Test
    fun `en tom historikk lagres og leses som en tom liste`() {
        no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser.empty().toDbJson() shouldBe "[]"
        "[]".toSøknadshendelser().toList() shouldBe emptyList()
    }
}
