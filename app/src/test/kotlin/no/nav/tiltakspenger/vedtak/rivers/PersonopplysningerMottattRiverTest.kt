package no.nav.tiltakspenger.vedtak.rivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tiltakspenger.vedtak.Aktivitetslogg
import no.nav.tiltakspenger.vedtak.Søker
import no.nav.tiltakspenger.vedtak.SøkerMediator
import no.nav.tiltakspenger.vedtak.SøkerTilstandType
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vedtak.Tiltak
import no.nav.tiltakspenger.vedtak.Tiltaksaktivitet
import no.nav.tiltakspenger.vedtak.meldinger.PersonopplysningerMottattHendelse
import no.nav.tiltakspenger.vedtak.meldinger.SøknadMottattHendelse
import no.nav.tiltakspenger.vedtak.repository.SøkerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

internal class PersonopplysningerMottattRiverTest {
    private val søkerRepository = mockk<SøkerRepository>(relaxed = true)
    private val testRapid = TestRapid()
    private val mediatorSpy = spyk(SøkerMediator(søkerRepository = søkerRepository, rapidsConnection = testRapid))

    init {
        PersonopplysningerMottattRiver(
            rapidsConnection = testRapid,
            søkerMediator = mediatorSpy
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `Når PersonopplysningerRiver får en løsning på person, skal den sende en behovsmelding etter skjerming`() {
        val today = LocalDate.now()
        val ident = "04927799109"
        val personopplysningerMottattHendelse =
            File("src/test/resources/personopplysningerMottattHendelse.json").readText()
        val søknadMottattHendelse = SøknadMottattHendelse(
            aktivitetslogg = Aktivitetslogg(forelder = null),
            ident = ident,
            søknad = Søknad(
                søknadId = "42",
                journalpostId = "43",
                dokumentInfoId = "44",
                fornavn = null,
                etternavn = null,
                ident = ident,
                deltarKvp = false,
                deltarIntroduksjonsprogrammet = null,
                introduksjonsprogrammetDetaljer = null,
                oppholdInstitusjon = null,
                typeInstitusjon = null,
                opprettet = null,
                barnetillegg = emptyList(),
                tidsstempelHosOss = LocalDateTime.now(),
                tiltak = Tiltak.ArenaTiltak(
                    arenaId = "123",
                    arrangoernavn = "Tiltaksarrangør AS",
                    harSluttdatoFraArena = false,
                    tiltakskode = Tiltaksaktivitet.Tiltak.ARBTREN,
                    erIEndreStatus = false,
                    opprinneligSluttdato = today,
                    opprinneligStartdato = today,
                    sluttdato = today,
                    startdato = today
                ),
                trygdOgPensjon = emptyList(),
                fritekst = null
            )
        )
        val søker = Søker(ident)
        every { søkerRepository.hent(ident) } returns søker

        søker.håndter(søknadMottattHendelse)
        testRapid.sendTestMessage(personopplysningerMottattHendelse)

        assertEquals(SøkerTilstandType.AvventerSkjermingdata, søker.tilstand.type)
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals("behov", field(0, "@event_name").asText())
            assertEquals("skjerming", field(0, "@behov")[0].asText())
            assertEquals(SøkerTilstandType.AvventerPersonopplysninger.name, field(0, "tilstandtype").asText())
            assertEquals(ident, field(0, "ident").asText())
            verify {
                mediatorSpy.håndter(
                    withArg<PersonopplysningerMottattHendelse> {
                        assertEquals(ident, it.ident())
                        assertEquals(ident, it.personopplysninger().ident)
                        assertEquals(LocalDate.of(1983, Month.JULY, 4), it.personopplysninger().fødselsdato)
                        assertEquals("Knuslete", it.personopplysninger().fornavn)
                        assertEquals("Melon", it.personopplysninger().mellomnavn)
                        assertEquals("Ekspedisjon", it.personopplysninger().etternavn)
                        assertEquals("Oslo", it.personopplysninger().kommune)
                        assertEquals("460105", it.personopplysninger().bydel)
                        assertEquals("Norge", it.personopplysninger().land)
                        assertFalse(it.personopplysninger().fortrolig)
                        assertFalse(it.personopplysninger().strengtFortrolig)
                        assertNull(it.personopplysninger().skjermet)
                    }
                )
            }
        }
    }
}
