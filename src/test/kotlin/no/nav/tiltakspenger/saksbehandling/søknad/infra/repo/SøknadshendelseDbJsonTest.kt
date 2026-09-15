package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser
import org.junit.jupiter.api.Test

class SøknadshendelseDbJsonTest {

    private val avbrutt = Søknadshendelse.Avbrutt(
        tidspunkt = 1.januar(2025).atTime(12, 0),
        utførtAv = "Z123456",
        begrunnelse = "duplikat".toNonBlankString(),
    )

    private val gjenåpnet = Søknadshendelse.Gjenåpnet(
        tidspunkt = 2.januar(2025).atTime(13, 30),
        utførtAv = "Z654321",
        begrunnelse = "avbrutt ved en feil".toNonBlankString(),
    )

    private val avbruttIgjen = Søknadshendelse.Avbrutt(
        tidspunkt = 3.januar(2025).atTime(9, 15),
        utførtAv = "Z123456",
        begrunnelse = "duplikat likevel".toNonBlankString(),
    )

    private val gjenåpnetUtenBegrunnelse = Søknadshendelse.Gjenåpnet(
        tidspunkt = 4.januar(2025).atTime(10, 45),
        utførtAv = "Z654321",
        begrunnelse = null,
    )

    @Test
    fun `hendelsene lagres med sitt avtalte navn og sine avtalte felter`() {
        Søknadshendelser(listOf(avbrutt, gjenåpnet, avbruttIgjen, gjenåpnetUtenBegrunnelse)).toDbJson() shouldBe
            """[{"type":"AVBRUTT","tidspunkt":"2025-01-01T12:00","utførtAv":"Z123456","begrunnelse":"duplikat"},""" +
            """{"type":"GJENÅPNET","tidspunkt":"2025-01-02T13:30","utførtAv":"Z654321","begrunnelse":"avbrutt ved en feil"},""" +
            """{"type":"AVBRUTT","tidspunkt":"2025-01-03T09:15","utførtAv":"Z123456","begrunnelse":"duplikat likevel"},""" +
            """{"type":"GJENÅPNET","tidspunkt":"2025-01-04T10:45","utførtAv":"Z654321","begrunnelse":null}]"""
    }

    @Test
    fun `hendelsene leses tilbake fra lagret json`() {
        val alle = listOf(avbrutt, gjenåpnet, avbruttIgjen, gjenåpnetUtenBegrunnelse)

        Søknadshendelser(alle).toDbJson().toSøknadshendelser().toList() shouldBe alle
    }

    @Test
    fun `en tom historikk lagres og leses som en tom liste`() {
        Søknadshendelser.empty().toDbJson() shouldBe "[]"
        "[]".toSøknadshendelser().toList() shouldBe emptyList()
    }
}
