package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import org.junit.jupiter.api.Test

/**
 * Ren mapping testes uten database, og pinner json-en slik den lagres.
 * Status og kilde lagres som navn, og et navnebytte i begge retninger samtidig ville ikke slått ut i en ren rundtur.
 */
class TiltaksdeltakelseSomFratarRettDbJsonTest {

    @Test
    fun `lagres med alle felter`() {
        val deltakelse = ObjectMother.tiltaksdeltakelseSomFratarRett(
            eksternDeltakelseId = "TA123",
            status = TiltakDeltakerstatus.HarSluttet,
            kilde = Tiltakskilde.TeamTiltak,
        )

        serialize(deltakelse.toDbJson()) shouldBe
            """{"eksternDeltakelseId":"TA123","typeNavn":"Midlertidig lønnstilskudd","tiltakskodeFraKilden":"MIDLONTIL","deltakelseFraOgMed":"2023-01-01","deltakelseTilOgMed":"2023-03-31","deltakelseStatus":"HarSluttet","deltakelseProsent":100.0,"antallDagerPerUke":5.0,"kilde":"TeamTiltak"}"""
    }

    @Test
    fun `lagres uten datoer og omfang`() {
        val deltakelse = ObjectMother.tiltaksdeltakelseSomFratarRett(
            eksternDeltakelseId = "TA123",
            fom = null,
            tom = null,
            deltakelseProsent = null,
            antallDagerPerUke = null,
        )

        serialize(deltakelse.toDbJson()) shouldBe
            """{"eksternDeltakelseId":"TA123","typeNavn":"Midlertidig lønnstilskudd","tiltakskodeFraKilden":"MIDLONTIL","deltakelseFraOgMed":null,"deltakelseTilOgMed":null,"deltakelseStatus":"Deltar","deltakelseProsent":null,"antallDagerPerUke":null,"kilde":"Arena"}"""
    }

    @Test
    fun `leses tilbake fra lagret verdi`() {
        val lagret =
            """{"eksternDeltakelseId":"TA123","typeNavn":"Midlertidig lønnstilskudd","tiltakskodeFraKilden":"MIDLONTIL","deltakelseFraOgMed":"2023-01-01","deltakelseTilOgMed":"2023-03-31","deltakelseStatus":"HarSluttet","deltakelseProsent":100.0,"antallDagerPerUke":5.0,"kilde":"TeamTiltak"}"""

        deserialize<TiltaksdeltakelseSomFratarRettDbJson>(lagret).toDomain() shouldBe ObjectMother.tiltaksdeltakelseSomFratarRett(
            eksternDeltakelseId = "TA123",
            status = TiltakDeltakerstatus.HarSluttet,
            kilde = Tiltakskilde.TeamTiltak,
        )
    }
}
