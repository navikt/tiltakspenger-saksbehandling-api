package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.junit.jupiter.api.Test

/**
 * Ren mapping testes uten database, og pinner json-en slik den lagres.
 */
class TiltaksdeltakelseDbTest {

    private val internDeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KAZ7Z8JDHVH2KQ4YJ7XK0Q4R")

    private val lagretUtenTittel =
        """{"eksternDeltagelseId":"TA123","gjennomføringId":"G1","typeNavn":"Avklaring","typeKode":"AVKLARING","deltagelseFraOgMed":"2023-01-01","deltagelseTilOgMed":"2023-03-31","deltakelseStatus":"Deltar","deltakelseProsent":100.0,"antallDagerPerUke":5.0,"kilde":"Komet","rettPåTiltakspenger":true,"deltidsprosentGjennomforing":null,"internDeltakelseId":"$internDeltakelseId"}"""

    private fun deltakelse(tittel: String?) = ObjectMother.tiltaksdeltakelse(
        eksternTiltaksdeltakelseId = "TA123",
        eksternTiltaksgjennomføringsId = "G1",
        tiltakstype = TiltakstypeSomGirRett.AVKLARING,
        typeNavn = "Avklaring",
        tittel = tittel,
        internDeltakelseId = internDeltakelseId,
    )

    @Test
    fun `tittelen lagres ved siden av typenavnet`() {
        serialize(deltakelse(tittel = "Avklaring hos Arrangør AS").toDbJson()) shouldBe
            """{"eksternDeltagelseId":"TA123","gjennomføringId":"G1","typeNavn":"Avklaring","tittel":"Avklaring hos Arrangør AS","typeKode":"AVKLARING","deltagelseFraOgMed":"2023-01-01","deltagelseTilOgMed":"2023-03-31","deltakelseStatus":"Deltar","deltakelseProsent":100.0,"antallDagerPerUke":5.0,"kilde":"Komet","rettPåTiltakspenger":true,"deltidsprosentGjennomforing":null,"internDeltakelseId":"$internDeltakelseId"}"""
    }

    @Test
    fun `lagret tittel leses tilbake`() {
        val lagret = lagretUtenTittel.replace(
            """"typeNavn":"Avklaring",""",
            """"typeNavn":"Avklaring","tittel":"Avklaring hos Arrangør AS",""",
        )

        deserialize<TiltaksdeltakelseDb>(lagret).toDomain() shouldBe deltakelse(tittel = "Avklaring hos Arrangør AS")
    }

    @Test
    fun `lagret verdi uten tittel gir ingen tittel`() {
        deserialize<TiltaksdeltakelseDb>(lagretUtenTittel).toDomain() shouldBe deltakelse(tittel = null)
    }
}
