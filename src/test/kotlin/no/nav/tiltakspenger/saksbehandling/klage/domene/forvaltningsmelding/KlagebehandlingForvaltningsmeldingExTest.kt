package no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KlagebehandlingForvaltningsmeldingExTest {

    private val respons = PdfOgJson(PdfA("pdf".toByteArray()), "{}").right()
    private val saksbehandler = ObjectMother.saksbehandler()
    private val sendteTekster = Brevtekster(listOf(TittelOgTekst(tittel = "Svartid", tekst = "Ca. 3 måneder.")))
    private val nyeTekster = Brevtekster(listOf(TittelOgTekst(tittel = "Svartid", tekst = "Ca. 6 måneder.")))

    private fun kommando(
        behandling: Klagebehandling,
        brevtekster: Brevtekster = sendteTekster,
    ) = ForvaltningsmeldingKommando(
        sakId = behandling.sakId,
        klagebehandlingId = behandling.id,
        saksbehandler = saksbehandler,
        correlationId = CorrelationId.generate(),
        brevtekster = brevtekster,
    )

    @Test
    fun `forhåndsvisning før sending bruker tekstene i kommandoen og gir utkast`() {
        runTest {
            val behandling = ObjectMother.opprettKlagebehandling(saksbehandler = saksbehandler)

            var fangetTekst: Brevtekster? = null
            var fangetForhåndsvisning: Boolean? = null
            behandling.forhåndsvisForvaltningsmelding(
                kommando = kommando(behandling, nyeTekster),
                generer = { _, _, navIdent, tekst, forhåndsvisning, innsendingsdato, _ ->
                    navIdent shouldBe saksbehandler.navIdent
                    innsendingsdato shouldBe behandling.formkrav.innsendingsdato
                    fangetTekst = tekst
                    fangetForhåndsvisning = forhåndsvisning
                    respons
                },
                clock = fixedClock,
            ) shouldBe respons

            fangetTekst shouldBe nyeTekster
            fangetForhåndsvisning shouldBe true
        }
    }

    @Test
    fun `forhåndsvisning etter sending viser det som ble sendt og ignorerer tekstene i kommandoen`() {
        runTest {
            val opprettet = ObjectMother.opprettKlagebehandling(saksbehandler = saksbehandler)
            val sendt = opprettet.sendForvaltningsmelding(kommando(opprettet), fixedClock).getOrNull()!!
            val journalført = sendt.copy(
                forvaltningsmelding = sendt.forvaltningsmelding!!.oppdaterJournalpost(
                    brevdato = LocalDate.of(2025, 3, 1),
                    journalpostId = JournalpostId("123"),
                    tidspunkt = nå(fixedClock),
                ),
            )

            listOf(sendt to LocalDate.now(fixedClock), journalført to LocalDate.of(2025, 3, 1)).forEach { (behandling, forventetBrevdato) ->
                var fangetTekst: Brevtekster? = null
                var fangetForhåndsvisning: Boolean? = null
                var fangetBrevdato: LocalDate? = null
                behandling.forhåndsvisForvaltningsmelding(
                    kommando = kommando(behandling, nyeTekster),
                    generer = { _, _, _, tekst, forhåndsvisning, _, brevdato ->
                        fangetTekst = tekst
                        fangetForhåndsvisning = forhåndsvisning
                        fangetBrevdato = brevdato
                        respons
                    },
                    clock = fixedClock,
                ) shouldBe respons

                fangetTekst shouldBe sendteTekster
                fangetForhåndsvisning shouldBe false
                fangetBrevdato shouldBe forventetBrevdato
            }
        }
    }

    @Test
    fun `endelig brev genereres fra den sendte forvaltningsmeldingen`() {
        runTest {
            val opprettet = ObjectMother.opprettKlagebehandling(saksbehandler = saksbehandler)
            val sendt = opprettet.sendForvaltningsmelding(kommando(opprettet), fixedClock).getOrNull()!!

            sendt.genererForvaltningsmelding(
                generer = { _, _, navIdent, tekst, forhåndsvisning, _, brevdato ->
                    navIdent shouldBe saksbehandler.navIdent
                    tekst shouldBe sendteTekster
                    forhåndsvisning shouldBe false
                    brevdato shouldBe LocalDate.of(2025, 3, 1)
                    respons
                },
                brevdato = LocalDate.of(2025, 3, 1),
            ) shouldBe respons

            shouldThrow<IllegalArgumentException> {
                opprettet.genererForvaltningsmelding(generer = { _, _, _, _, _, _, _ -> respons }, brevdato = LocalDate.now(fixedClock))
            }
        }
    }

    @Test
    fun `journalføring og distribusjon kan bare skje én gang og i riktig rekkefølge`() {
        val ny = KlageForvaltningsmelding.ny(sendteTekster, saksbehandler.navIdent, nå(fixedClock))

        shouldThrow<IllegalArgumentException> { ny.oppdaterDistribusjon(DistribusjonId("d"), nå(fixedClock)) }

        val journalført = ny.oppdaterJournalpost(LocalDate.now(fixedClock), JournalpostId("j"), nå(fixedClock))
        shouldThrow<IllegalArgumentException> {
            journalført.oppdaterJournalpost(LocalDate.now(fixedClock), JournalpostId("j"), nå(fixedClock))
        }

        val distribuert = journalført.oppdaterDistribusjon(DistribusjonId("d"), nå(fixedClock))
        distribuert.erDistribuert shouldBe true
        shouldThrow<IllegalArgumentException> { distribuert.oppdaterDistribusjon(DistribusjonId("d"), nå(fixedClock)) }
    }
}
