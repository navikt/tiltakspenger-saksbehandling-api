package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakelsesomfang
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.GjennomføringId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TeamTiltakstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelser
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakshistorikk
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakstype
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.UkjenteDeltakelsesformer
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testStatusOpprettet
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import org.junit.jupiter.api.Test

class TiltakshistorikkTilRegisterMapperTest {

    private val ingenSøktFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty()

    private fun historikk(vararg deltakelser: Tiltaksdeltakelse) = Tiltakshistorikk(
        deltakelser = Tiltaksdeltakelser(deltakelser.toList()),
        ukjenteDeltakelsesformer = UkjenteDeltakelsesformer(emptyList()),
        hentetTidspunkt = nå(fixedClock),
    )

    private fun kometstatus(type: Kometstatus.Type) = Kometstatus.Kjent(type, årsak = null, opprettet = testStatusOpprettet)

    @Test
    fun `kun deltakelser som gir rett er med i uttrekket`() {
        val girRett = testdeltakelse(id = "TA1")
        val girIkkeRett = testdeltakelse(id = "TA2", tiltakstype = Tiltakstype.SomIkkeGirRett(tiltakskodeFraKilden = "LONNTIL"))
        val ukjentType = testdeltakelse(id = "TA3", tiltakstype = Tiltakstype.Ukjent(tiltakskodeFraKilden = "NOE_NYTT"))

        val resultat = historikk(girRett, girIkkeRett, ukjentType).tilRelevanteTiltaksdeltakelser(ingenSøktFor, fixedClock)

        resultat.deltakelser.map { it.id.verdi } shouldBe listOf("TA1")
    }

    @Test
    fun `deltakelse uten datoer er kun med når den venter på oppstart eller er søkt for`() {
        val venterPåOppstart = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.VENTER_PA_OPPSTART), fraOgMed = null, tilOgMed = null)
        val deltarUtenDatoer = testdeltakelse(id = "TA2", kildestatus = kometstatus(Kometstatus.Type.DELTAR), fraOgMed = null, tilOgMed = null)
        val kunFraDato = testdeltakelse(id = "TA3", fraOgMed = 1.mars(2026), tilOgMed = null)

        val resultat = historikk(venterPåOppstart, deltarUtenDatoer, kunFraDato).tilRelevanteTiltaksdeltakelser(ingenSøktFor, fixedClock)

        resultat.deltakelser.map { it.id.verdi } shouldBe listOf("TA1", "TA3")
    }

    @Test
    fun `deltakelse det er søkt for beholdes selv uten datoer og uavhengig av status`() {
        val søktForDeltakelse = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.SOKT_INN), fraOgMed = null, tilOgMed = null)
        val søktFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
            ObjectMother.søknadstiltak(id = "TA1"),
            1.januar(2025).atStartOfDay(),
        )

        val resultat = historikk(søktForDeltakelse).tilRelevanteTiltaksdeltakelser(søktFor, fixedClock)

        resultat.deltakelser.map { it.id.verdi } shouldBe listOf("TA1")
        resultat.girRett.single().tiltakDeltakerstatus(fixedClock) shouldBe TiltakDeltakerstatus.SøktInn
    }

    @Test
    fun `deltakelse med ukjent kildestatus utelates - den kan ikke tolkes`() {
        val ukjentStatus = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Ukjent("HELT_NY_STATUS"))
        val kjentStatus = testdeltakelse(id = "TA2")

        val resultat = historikk(ukjentStatus, kjentStatus).tilRelevanteTiltaksdeltakelser(ingenSøktFor, fixedClock)

        resultat.deltakelser.map { it.id.verdi } shouldBe listOf("TA2")
    }

    @Test
    fun `feltene mappes fra libs-domenet til intern deltakelse`() {
        val deltakelse = (testdeltakelse(id = "TA123") as Tiltaksdeltakelse.GirRett.MedPeriode)
            .copy(gjennomføringId = GjennomføringId("gjennomføring-1"))
        val internId = TiltaksdeltakerId.random()

        val resultat = deltakelse.tilTiltaksdeltakelseIntern(internId, fixedClock)

        resultat shouldBe TiltaksdeltakelseIntern(
            eksternDeltakelseId = "TA123",
            gjennomføringId = "gjennomføring-1",
            typeNavn = "Oppfølging",
            typeKode = TiltakstypeSomGirRettDTO.OPPFØLGING,
            rettPåTiltakspenger = true,
            deltakelseFraOgMed = deltakelse.fraOgMed,
            deltakelseTilOgMed = deltakelse.tilOgMed,
            deltakelseStatus = TiltakDeltakerstatus.Deltar,
            deltakelseProsent = 60f,
            antallDagerPerUke = 3f,
            kilde = Tiltakskilde.Komet,
            deltidsprosentGjennomforing = null,
            internDeltakelseId = internId,
        )
    }

    @Test
    fun `intern deltakelse bærer deltidsprosent fra gjennomføringen og mangler gjennomføring når kilden ikke har den`() {
        val deltakelse = (testdeltakelse(id = "TA1") as Tiltaksdeltakelse.GirRett.MedPeriode)
            .copy(omfang = Deltakelsesomfang(deltakelsesprosent = null, dagerPerUke = null, deltidsprosentPåGjennomføring = 80f), gjennomføringId = null)

        val resultat = deltakelse.tilTiltaksdeltakelseIntern(TiltaksdeltakerId.random(), fixedClock)

        resultat.gjennomføringId shouldBe null
        resultat.deltidsprosentGjennomforing shouldBe 80.0
    }

    @Test
    fun `lesbar nå-tilstand - kun deltakelser som gir rett og har kjent kildestatus`() {
        val girRett = testdeltakelse(id = "TA1")
        val girIkkeRett = testdeltakelse(id = "TA2", tiltakstype = Tiltakstype.SomIkkeGirRett(tiltakskodeFraKilden = "LONNTIL"))
        val ukjentStatus = testdeltakelse(id = "TA3", kildestatus = Arenastatus.Ukjent("HELT_NY_STATUS"))

        girRett.tilLesbarNåtilstand(fixedClock) shouldBe girRett
        girIkkeRett.tilLesbarNåtilstand(fixedClock).shouldBeNull()
        ukjentStatus.tilLesbarNåtilstand(fixedClock).shouldBeNull()
    }

    @Test
    fun `lesbar nå-tilstand - Komet KLADD kaster`() {
        val kladd = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.KLADD))

        shouldThrow<IllegalStateException> { kladd.tilLesbarNåtilstand(fixedClock) }
    }

    @Test
    fun `tolket status kaster for ukjent kildestatus - uttrekkene slipper aldri slike gjennom`() {
        val ukjentStatus = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Ukjent("HELT_NY_STATUS")) as Tiltaksdeltakelse.GirRett

        shouldThrow<IllegalStateException> { ukjentStatus.tiltakDeltakerstatus(fixedClock) }
    }

    @Test
    fun `Arena GJENNOMFORES mappes etter startdato - fremtidig eller manglende gir VenterPåOppstart`() {
        // fixedClock er 1. januar 2025.
        val deltar = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES), fraOgMed = 1.januar(2025))
        val venterFremtidig = testdeltakelse(id = "TA2", kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES), fraOgMed = 2.januar(2025))
        val venterUtenStart = testdeltakelse(id = "TA3", kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES), fraOgMed = null)

        val resultat = historikk(deltar, venterFremtidig, venterUtenStart).tilRelevanteTiltaksdeltakelser(ingenSøktFor, fixedClock)

        resultat.girRett.map { it.tiltakDeltakerstatus(fixedClock) } shouldBe listOf(
            TiltakDeltakerstatus.Deltar,
            TiltakDeltakerstatus.VenterPåOppstart,
            TiltakDeltakerstatus.VenterPåOppstart,
        )
    }

    @Test
    fun `Arena IKKE_MOTT mappes til Avbrutt - paritet med dagens oppførsel`() {
        val ikkeMøtt = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Kjent(Arenastatus.Type.IKKE_MOTT))

        val resultat = historikk(ikkeMøtt).tilRelevanteTiltaksdeltakelser(ingenSøktFor, fixedClock)

        resultat.girRett.single().tiltakDeltakerstatus(fixedClock) shouldBe TiltakDeltakerstatus.Avbrutt
    }

    @Test
    fun `statusmappingen fra Arena er uttømmende`() {
        // GJENNOMFORES avhenger av startdatoen, og har egen test; her har deltakelsen startet.
        val startet = 1.januar(2025)
        val forventet = mapOf(
            Arenastatus.Type.DELTAKELSE_AVBRUTT to TiltakDeltakerstatus.Avbrutt,
            Arenastatus.Type.FULLFORT to TiltakDeltakerstatus.Fullført,
            Arenastatus.Type.GJENNOMFORES to TiltakDeltakerstatus.Deltar,
            Arenastatus.Type.GJENNOMFORING_AVBRUTT to TiltakDeltakerstatus.Avbrutt,
            Arenastatus.Type.IKKE_MOTT to TiltakDeltakerstatus.Avbrutt,
            Arenastatus.Type.TAKKET_JA_TIL_TILBUD to TiltakDeltakerstatus.Deltar,
            Arenastatus.Type.TILBUD to TiltakDeltakerstatus.VenterPåOppstart,
            Arenastatus.Type.AKTUELL to TiltakDeltakerstatus.SøktInn,
            Arenastatus.Type.AVSLAG to TiltakDeltakerstatus.IkkeAktuell,
            Arenastatus.Type.GJENNOMFORING_AVLYST to TiltakDeltakerstatus.IkkeAktuell,
            Arenastatus.Type.IKKE_AKTUELL to TiltakDeltakerstatus.IkkeAktuell,
            Arenastatus.Type.INFORMASJONSMOTE to TiltakDeltakerstatus.Venteliste,
            Arenastatus.Type.TAKKET_NEI_TIL_TILBUD to TiltakDeltakerstatus.IkkeAktuell,
            Arenastatus.Type.VENTELISTE to TiltakDeltakerstatus.Venteliste,
            Arenastatus.Type.FEILREGISTRERT to TiltakDeltakerstatus.Feilregistrert,
        )

        Arenastatus.Type.entries.associateWith { Arenastatus.Kjent(it).tilTiltakDeltakerstatus(startet, fixedClock) } shouldBe forventet
    }

    @Test
    fun `statusmappingen fra Komet er uttømmende for alt unntatt kladd`() {
        val forventet = mapOf(
            Kometstatus.Type.UTKAST_TIL_PAMELDING to TiltakDeltakerstatus.PåbegyntRegistrering,
            Kometstatus.Type.PABEGYNT_REGISTRERING to TiltakDeltakerstatus.PåbegyntRegistrering,
            Kometstatus.Type.AVBRUTT_UTKAST to TiltakDeltakerstatus.IkkeAktuell,
            Kometstatus.Type.IKKE_AKTUELL to TiltakDeltakerstatus.IkkeAktuell,
            Kometstatus.Type.VENTER_PA_OPPSTART to TiltakDeltakerstatus.VenterPåOppstart,
            Kometstatus.Type.DELTAR to TiltakDeltakerstatus.Deltar,
            Kometstatus.Type.HAR_SLUTTET to TiltakDeltakerstatus.HarSluttet,
            Kometstatus.Type.FEILREGISTRERT to TiltakDeltakerstatus.Feilregistrert,
            Kometstatus.Type.SOKT_INN to TiltakDeltakerstatus.SøktInn,
            Kometstatus.Type.VURDERES to TiltakDeltakerstatus.Vurderes,
            Kometstatus.Type.VENTELISTE to TiltakDeltakerstatus.Venteliste,
            Kometstatus.Type.AVBRUTT to TiltakDeltakerstatus.Avbrutt,
            Kometstatus.Type.FULLFORT to TiltakDeltakerstatus.Fullført,
        )

        (Kometstatus.Type.entries - Kometstatus.Type.KLADD)
            .associateWith { kometstatus(it).tilTiltakDeltakerstatus(null, fixedClock) } shouldBe forventet
    }

    @Test
    fun `statusmappingen fra Team Tiltak er uttømmende`() {
        val forventet = mapOf(
            TeamTiltakstatus.Type.PAABEGYNT to TiltakDeltakerstatus.PåbegyntRegistrering,
            TeamTiltakstatus.Type.MANGLER_GODKJENNING to TiltakDeltakerstatus.SøktInn,
            TeamTiltakstatus.Type.KLAR_FOR_OPPSTART to TiltakDeltakerstatus.VenterPåOppstart,
            TeamTiltakstatus.Type.GJENNOMFORES to TiltakDeltakerstatus.Deltar,
            TeamTiltakstatus.Type.AVSLUTTET to TiltakDeltakerstatus.HarSluttet,
            TeamTiltakstatus.Type.AVBRUTT to TiltakDeltakerstatus.Avbrutt,
            TeamTiltakstatus.Type.ANNULLERT to TiltakDeltakerstatus.IkkeAktuell,
        )

        TeamTiltakstatus.Type.entries.associateWith { TeamTiltakstatus.Kjent(it).tilTiltakDeltakerstatus(null, fixedClock) } shouldBe forventet
    }

    @Test
    fun `Komet KLADD kaster - en kladd er ikke delt hos kilden og skal aldri nå oss`() {
        val kladd = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.KLADD))

        shouldThrow<IllegalStateException> {
            historikk(kladd).tilRelevanteTiltaksdeltakelser(ingenSøktFor, fixedClock)
        }
    }

    @Test
    fun `MedArrangørnavn - tittel er visningsnavn, med mindre bruker har adressebeskyttelse`() {
        val deltakelse = testdeltakelse(id = "TA1")
        val historikk = historikk(deltakelse)

        historikk.tilTiltaksdeltakelserMedArrangørnavn(harAdressebeskyttelse = false, clock = fixedClock).single().visningsnavn shouldBe "Oppfølging hos Arrangør AS"
        historikk.tilTiltaksdeltakelserMedArrangørnavn(harAdressebeskyttelse = true, clock = fixedClock).single().visningsnavn shouldBe "Oppfølging"
    }

    @Test
    fun `MedArrangørnavn - manglende tittel faller tilbake på tiltakstypenavnet`() {
        val deltakelseUtenTittel = (testdeltakelse(id = "TA1") as Tiltaksdeltakelse.GirRett.MedPeriode).copy(tittel = null)

        val resultat = historikk(deltakelseUtenTittel).tilTiltaksdeltakelserMedArrangørnavn(harAdressebeskyttelse = false, clock = fixedClock)

        resultat.single().visningsnavn shouldBe "Oppfølging"
    }

    @Test
    fun `MedArrangørnavn - samme utvalgsregler som registeruttrekket`() {
        val girIkkeRett = testdeltakelse(id = "TA1", tiltakstype = Tiltakstype.SomIkkeGirRett(tiltakskodeFraKilden = "LONNTIL"))
        val deltarUtenDatoer = testdeltakelse(id = "TA2", kildestatus = kometstatus(Kometstatus.Type.DELTAR), fraOgMed = null, tilOgMed = null)

        val resultat = historikk(girIkkeRett, deltarUtenDatoer).tilTiltaksdeltakelserMedArrangørnavn(harAdressebeskyttelse = false, clock = fixedClock)

        resultat.shouldBeEmpty()
    }
}
