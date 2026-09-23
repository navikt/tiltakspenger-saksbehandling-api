package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Ren mapping testes uten database.
 * Verdien lagres slik klassen serialiseres, så formatet dens kan endre seg, og da oppdateres testen.
 * Testen bruker en verdi med alle felter satt, slik at avledede getter-verdier som ikke er data (eller ikke kan serialiseres) fanges.
 */
class OppgavegrunnlagDbJsonTest {
    @Test
    fun `endret tiltaksdeltakelse lagrer kilde og verdien slik den er`() {
        val verdi = TiltaksdeltakelseFraRegister(
            eksternDeltakelseId = "ekstern-123",
            gjennomføringId = "gjennomføring-456",
            typeNavn = "Gruppe AMO",
            typeKode = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
            rettPåTiltakspenger = true,
            deltakelseFraOgMed = LocalDate.of(2026, 1, 2),
            deltakelseTilOgMed = LocalDate.of(2026, 6, 30),
            deltakelseStatus = TiltakDeltakerstatus.Deltar,
            deltakelseProsent = 50.0f,
            antallDagerPerUke = 2.5f,
            kilde = Tiltakskilde.Komet,
            deltidsprosentGjennomforing = 80.0,
        )

        Oppgavegrunnlag.EndretTiltaksdeltakelse(
            kilde = Kilde.Tiltakshistorikk(LocalDateTime.of(2026, 1, 2, 13, 14, 15, 123456000)),
            verdi = verdi,
        ).toDbJson() shouldBe
            """{"type":"ENDRET_TILTAKSDELTAKELSE","kilde":{"type":"TILTAKSHISTORIKK","sisteUbehandletEndring":"2026-01-02T13:14:15.123456"},"verdi":{"eksternDeltakelseId":"ekstern-123","gjennomføringId":"gjennomføring-456","typeNavn":"Gruppe AMO","typeKode":"GRUPPE_AMO","rettPåTiltakspenger":true,"deltakelseFraOgMed":"2026-01-02","deltakelseTilOgMed":"2026-06-30","deltakelseStatus":"Deltar","deltakelseProsent":50.0,"antallDagerPerUke":2.5,"kilde":"Komet","deltidsprosentGjennomforing":80.0}}"""
    }
}
