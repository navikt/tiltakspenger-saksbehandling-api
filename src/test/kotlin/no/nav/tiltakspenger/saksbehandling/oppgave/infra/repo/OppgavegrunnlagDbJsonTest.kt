package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.Test
import tools.jackson.databind.exc.InvalidTypeIdException
import java.time.LocalDate

/**
 * Ren mapping testes uten database for å pinne lagringsformatet uavhengig av prodstiene.
 * Testene verifiserer eksplisitt JSON, lesing av lagret JSON og rundtur, ikke at variantene nås gjennom prodstiene.
 */
class OppgavegrunnlagDbJsonTest {
    @Test
    fun `endret tiltaksdeltakelse med alle valgfrie opplysninger`() {
        verifiser(
            Oppgavegrunnlag.EndretTiltaksdeltakelse(
                hendelseId = TiltaksdeltakerHendelseId.fromString("tiltaksdeltakerhendelse_01HSTRQBRM443VGB4WA822TE01"),
                tiltaksdeltakerId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01HSTRQBRM443VGB4WA822TE02"),
                eksternDeltakerId = "ekstern-123",
                deltakelseFraOgMed = LocalDate.of(2026, 1, 2),
                deltakelseTilOgMed = LocalDate.of(2026, 6, 30),
                dagerPerUke = 2.5f,
                deltakelsesprosent = 50.0f,
                deltakerstatus = TiltakDeltakerstatus.Deltar,
            ),
            """{"type":"ENDRET_TILTAKSDELTAKELSE","hendelseId":"tiltaksdeltakerhendelse_01HSTRQBRM443VGB4WA822TE01","tiltaksdeltakerId":"tiltaksdeltaker_01HSTRQBRM443VGB4WA822TE02","eksternDeltakerId":"ekstern-123","deltakelseFraOgMed":"2026-01-02","deltakelseTilOgMed":"2026-06-30","dagerPerUke":2.5,"deltakelsesprosent":50.0,"deltakerstatus":"Deltar"}""",
        )
    }

    @Test
    fun `endret tiltaksdeltakelse uten valgfrie opplysninger`() {
        verifiser(
            Oppgavegrunnlag.EndretTiltaksdeltakelse(
                hendelseId = TiltaksdeltakerHendelseId.fromString("tiltaksdeltakerhendelse_01HSTRQBRM443VGB4WA822TE01"),
                tiltaksdeltakerId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01HSTRQBRM443VGB4WA822TE02"),
                eksternDeltakerId = "ekstern-123",
                deltakelseFraOgMed = null,
                deltakelseTilOgMed = null,
                dagerPerUke = null,
                deltakelsesprosent = null,
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
            ),
            """{"type":"ENDRET_TILTAKSDELTAKELSE","hendelseId":"tiltaksdeltakerhendelse_01HSTRQBRM443VGB4WA822TE01","tiltaksdeltakerId":"tiltaksdeltaker_01HSTRQBRM443VGB4WA822TE02","eksternDeltakerId":"ekstern-123","deltakelseFraOgMed":null,"deltakelseTilOgMed":null,"dagerPerUke":null,"deltakelsesprosent":null,"deltakerstatus":"Avbrutt"}""",
        )
    }

    @Test
    fun `tidligere personhendelse søknad og meldekort støttes ikke`() {
        listOf(
            """{"type":"PERSONHENDELSE","personhendelseId":"b6b25b11-234b-432f-91b7-f6869e4f96c3","hendelseId":"pdl-hendelse-1","opplysning":{"type":"DOEDSFALL","doedsdato":"2026-03-17"}}""",
            """{"type":"PERSONHENDELSE","personhendelseId":"b6b25b11-234b-432f-91b7-f6869e4f96c3","hendelseId":"pdl-hendelse-2","opplysning":{"type":"ADRESSEBESKYTTELSE","gradering":"STRENGT_FORTROLIG_UTLAND"}}""",
            """{"type":"SOKNAD","søknadId":"soknad_01HSTRQBRM443VGB4WA822TE01","journalpostId":"453812134","personbeskyttelse":{"fortrolig":true,"strengtFortrolig":false,"strengtFortroligUtland":false,"skjermet":false}}""",
            """{"type":"MELDEKORT","meldekortId":"meldekort_01HSTRQBRM443VGB4WA822TE02","journalpostId":"453812135","personbeskyttelse":{"fortrolig":false,"strengtFortrolig":false,"strengtFortroligUtland":false,"skjermet":true}}""",
        ).forEach { json ->
            shouldThrow<InvalidTypeIdException> { json.toOppgavegrunnlag() }
        }
    }

    private fun verifiser(grunnlag: Oppgavegrunnlag, json: String) {
        grunnlag.toDbJson() shouldBe json
        json.toOppgavegrunnlag() shouldBe grunnlag
        grunnlag.toDbJson().toOppgavegrunnlag() shouldBe grunnlag
    }
}
