package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Ren mapping testes uten database.
 * Verdiens format pinnes i detalj av TiltaksdeltakelseNåtilstandDbJsonTest; her pinnes konvolutten rundt den.
 */
class OppgavegrunnlagDbJsonTest {
    @Test
    fun `endret tiltaksdeltakelse lagrer kilde og verdien slik den er`() {
        val verdi = ObjectMother.tiltaksdeltakelseTac(
            eksternTiltaksdeltakelseId = "ekstern-123",
            typeKode = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
            typeNavn = "Gruppe AMO",
            eksternTiltaksgjennomføringsId = "gjennomføring-456",
            fom = LocalDate.of(2026, 1, 2),
            tom = LocalDate.of(2026, 6, 30),
            status = TiltakDeltakerstatus.Deltar,
            dagerPrUke = 2.5f,
            prosent = 50.0f,
            kilde = Tiltakskilde.Arena,
            deltidsprosentGjennomforing = 80.0,
        ).tilLibsDeltakelse()

        Oppgavegrunnlag.EndretTiltaksdeltakelse(
            kilde = Kilde.Tiltakshistorikk(LocalDateTime.of(2026, 1, 2, 13, 14, 15, 123456000)),
            verdi = verdi,
        ).toDbJson() shouldBe
            """{"type":"ENDRET_TILTAKSDELTAKELSE","kilde":{"type":"TILTAKSHISTORIKK","sisteUbehandletEndring":"2026-01-02T13:14:15.123456"},"verdi":{"eksternDeltakelseId":"ekstern-123","gjennomføringId":"gjennomføring-456","tiltakstype":"GRUPPE_AMO","tiltakstypenavn":"Gruppe AMO","tiltakskodeFraKilden":"GRUPPE_AMO","fraOgMed":"2026-01-02","tilOgMed":"2026-06-30","kildestatus":{"kilde":"Arena","kodeIKontrakten":"TAKKET_JA_TIL_TILBUD","årsak":null,"opprettet":null},"deltakelsesprosent":50.0,"dagerPerUke":2.5,"deltidsprosentPåGjennomføring":80.0}}"""
    }
}
