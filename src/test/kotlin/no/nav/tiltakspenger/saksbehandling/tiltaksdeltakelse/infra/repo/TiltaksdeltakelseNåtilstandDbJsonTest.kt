package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometårsak
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Ren mapping testes uten database, og pinner json-en slik den lagres.
 * Verdien leses aldri tilbake, så en formatendring ville ellers gått upåaktet hen.
 */
class TiltaksdeltakelseNåtilstandDbJsonTest {

    private val opprettet = LocalDateTime.of(2026, 1, 2, 13, 14, 15)

    private fun deltakelse(
        kilde: Tiltakskilde = Tiltakskilde.Komet,
        status: TiltakDeltakerstatus = TiltakDeltakerstatus.Deltar,
    ): TiltaksdeltakelseIntern = ObjectMother.tiltaksdeltakelseTac(
        eksternTiltaksdeltakelseId = "ekstern-id",
        typeKode = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
        typeNavn = "Gruppe AMO",
        eksternTiltaksgjennomføringsId = "gjennomføring-id",
        fom = LocalDate.of(2026, 1, 1),
        tom = LocalDate.of(2026, 3, 31),
        status = status,
        dagerPrUke = 2.5f,
        prosent = 50f,
        kilde = kilde,
        deltidsprosentGjennomforing = 80.0,
    )

    private fun Tiltaksdeltakelse.GirRett.medKildestatus(kildestatus: Kildestatus): Tiltaksdeltakelse.GirRett = when (this) {
        is Tiltaksdeltakelse.GirRett.MedPeriode -> copy(kildestatus = kildestatus)
        is Tiltaksdeltakelse.GirRett.UtenPeriode -> copy(kildestatus = kildestatus)
    }

    private fun Tiltaksdeltakelse.GirRett.tilJson(): String = serialize(tilNåtilstandDbJson())

    @Test
    fun `Komet-deltakelse lagres med kildestatus, årsak og statustidspunkt`() {
        val nåtilstand = deltakelse().tilLibsDeltakelse()
            .medKildestatus(Kometstatus.Kjent(Kometstatus.Type.HAR_SLUTTET, Kometårsak.Kjent(Kometstatus.Årsak.FATT_JOBB), opprettet))

        nåtilstand.tilJson() shouldBe
            """{"eksternDeltakelseId":"ekstern-id","gjennomføringId":"gjennomføring-id","tiltakstype":"GRUPPE_AMO","tiltakstypenavn":"Gruppe AMO","tiltakskodeFraKilden":"GRUPPE_AMO","fraOgMed":"2026-01-01","tilOgMed":"2026-03-31","kildestatus":{"kilde":"Komet","kodeIKontrakten":"HAR_SLUTTET","årsak":"FATT_JOBB","opprettet":"2026-01-02T13:14:15"},"deltakelsesprosent":50.0,"dagerPerUke":2.5,"deltidsprosentPåGjennomføring":80.0}"""
    }

    @Test
    fun `Komet-deltakelse uten årsak lagres med null årsak`() {
        val nåtilstand = deltakelse().tilLibsDeltakelse()
            .medKildestatus(Kometstatus.Kjent(Kometstatus.Type.DELTAR, årsak = null, opprettet = opprettet))

        nåtilstand.tilJson() shouldBe
            """{"eksternDeltakelseId":"ekstern-id","gjennomføringId":"gjennomføring-id","tiltakstype":"GRUPPE_AMO","tiltakstypenavn":"Gruppe AMO","tiltakskodeFraKilden":"GRUPPE_AMO","fraOgMed":"2026-01-01","tilOgMed":"2026-03-31","kildestatus":{"kilde":"Komet","kodeIKontrakten":"DELTAR","årsak":null,"opprettet":"2026-01-02T13:14:15"},"deltakelsesprosent":50.0,"dagerPerUke":2.5,"deltidsprosentPåGjennomføring":80.0}"""
    }

    @Test
    fun `ukjente koder lagres ordrett slik de står i kontrakten`() {
        val nåtilstand = deltakelse().tilLibsDeltakelse()
            .medKildestatus(Kometstatus.Ukjent("HELT_NY_STATUS", Kometårsak.Ukjent("HELT_NY_ÅRSAK"), opprettet))

        nåtilstand.tilNåtilstandDbJson().kildestatus shouldBe TiltaksdeltakelseNåtilstandDbJson.KildestatusDbJson(
            kilde = "Komet",
            kodeIKontrakten = "HELT_NY_STATUS",
            årsak = "HELT_NY_ÅRSAK",
            opprettet = "2026-01-02T13:14:15",
        )
    }

    @Test
    fun `Arena-deltakelse lagres uten årsak og statustidspunkt`() {
        val nåtilstand = deltakelse(kilde = Tiltakskilde.Arena).tilLibsDeltakelse()
            .medKildestatus(Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES))

        nåtilstand.tilNåtilstandDbJson().kildestatus shouldBe TiltaksdeltakelseNåtilstandDbJson.KildestatusDbJson(
            kilde = "Arena",
            kodeIKontrakten = "GJENNOMFORES",
            årsak = null,
            opprettet = null,
        )
    }

    @Test
    fun `Team Tiltak-deltakelse lagres med kilden sin`() {
        val nåtilstand = deltakelse(kilde = Tiltakskilde.TeamTiltak).tilLibsDeltakelse()

        nåtilstand.tilNåtilstandDbJson().kildestatus shouldBe TiltaksdeltakelseNåtilstandDbJson.KildestatusDbJson(
            kilde = "TeamTiltak",
            kodeIKontrakten = "GJENNOMFORES",
            årsak = null,
            opprettet = null,
        )
    }

    @Test
    fun `deltakelse uten datoer, gjennomføring og omfang lagres med null-verdier`() {
        val nåtilstand = deltakelse().copy(
            gjennomføringId = null,
            deltakelseFraOgMed = null,
            deltakelseTilOgMed = null,
            deltakelseProsent = null,
            antallDagerPerUke = null,
            deltidsprosentGjennomforing = null,
        ).tilLibsDeltakelse()
            .medKildestatus(Kometstatus.Kjent(Kometstatus.Type.VENTER_PA_OPPSTART, årsak = null, opprettet = opprettet))

        nåtilstand.tilJson() shouldBe
            """{"eksternDeltakelseId":"ekstern-id","gjennomføringId":null,"tiltakstype":"GRUPPE_AMO","tiltakstypenavn":"Gruppe AMO","tiltakskodeFraKilden":"GRUPPE_AMO","fraOgMed":null,"tilOgMed":null,"kildestatus":{"kilde":"Komet","kodeIKontrakten":"VENTER_PA_OPPSTART","årsak":null,"opprettet":"2026-01-02T13:14:15"},"deltakelsesprosent":null,"dagerPerUke":null,"deltidsprosentPåGjennomføring":null}"""
    }
}
