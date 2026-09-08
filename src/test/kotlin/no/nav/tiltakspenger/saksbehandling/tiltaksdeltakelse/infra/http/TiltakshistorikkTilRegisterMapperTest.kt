package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
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

        val resultat = historikk(girRett, girIkkeRett, ukjentType).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)

        resultat.value.map { it.eksternDeltakelseId } shouldBe listOf("TA1")
    }

    @Test
    fun `deltakelse uten datoer er kun med når den venter på oppstart eller er søkt for`() {
        val venterPåOppstart = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.VENTER_PA_OPPSTART), fraOgMed = null, tilOgMed = null)
        val deltarUtenDatoer = testdeltakelse(id = "TA2", kildestatus = kometstatus(Kometstatus.Type.DELTAR), fraOgMed = null, tilOgMed = null)
        val kunFraDato = testdeltakelse(id = "TA3", fraOgMed = 1.mars(2026), tilOgMed = null)

        val resultat = historikk(venterPåOppstart, deltarUtenDatoer, kunFraDato).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)

        resultat.value.map { it.eksternDeltakelseId } shouldBe listOf("TA1", "TA3")
    }

    @Test
    fun `deltakelse det er søkt for beholdes selv uten datoer og uavhengig av status`() {
        val søktForDeltakelse = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.SOKT_INN), fraOgMed = null, tilOgMed = null)
        val søktFor = TiltaksdeltakelserDetErSøktTiltakspengerFor(
            ObjectMother.søknadstiltak(id = "TA1"),
            1.januar(2025).atStartOfDay(),
        )

        val resultat = historikk(søktForDeltakelse).tilTiltaksdeltakelserFraRegister(søktFor, fixedClock)

        resultat.value.map { it.eksternDeltakelseId } shouldBe listOf("TA1")
        resultat.value.single().deltakelseStatus shouldBe TiltakDeltakerstatus.SøktInn
    }

    @Test
    fun `deltakelse med ukjent kildestatus utelates - den kan ikke tolkes`() {
        val ukjentStatus = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Ukjent("HELT_NY_STATUS"))
        val kjentStatus = testdeltakelse(id = "TA2")

        val resultat = historikk(ukjentStatus, kjentStatus).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)

        resultat.value.map { it.eksternDeltakelseId } shouldBe listOf("TA2")
    }

    @Test
    fun `feltene mappes fra libs-domenet`() {
        val deltakelse = testdeltakelse(id = "TA123")

        val resultat = historikk(deltakelse).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock).value.single()

        resultat.eksternDeltakelseId shouldBe "TA123"
        resultat.typeNavn shouldBe "Oppfølging"
        resultat.typeKode.name shouldBe "OPPFØLGING"
        resultat.rettPåTiltakspenger shouldBe true
        resultat.deltakelseFraOgMed shouldBe deltakelse.fraOgMed
        resultat.deltakelseTilOgMed shouldBe deltakelse.tilOgMed
        resultat.deltakelseStatus shouldBe TiltakDeltakerstatus.Deltar
        resultat.deltakelseProsent shouldBe 60f
        resultat.antallDagerPerUke shouldBe 3f
        resultat.kilde shouldBe Tiltakskilde.Komet
        resultat.deltidsprosentGjennomforing shouldBe null
    }

    @Test
    fun `Arena GJENNOMFORES mappes etter startdato - fremtidig eller manglende gir VenterPåOppstart`() {
        // fixedClock er 1. januar 2025.
        val deltar = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES), fraOgMed = 1.januar(2025))
        val venterFremtidig = testdeltakelse(id = "TA2", kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES), fraOgMed = 2.januar(2025))
        val venterUtenStart = testdeltakelse(id = "TA3", kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES), fraOgMed = null)

        val resultat = historikk(deltar, venterFremtidig, venterUtenStart).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)

        resultat.value.map { it.deltakelseStatus } shouldBe listOf(
            TiltakDeltakerstatus.Deltar,
            TiltakDeltakerstatus.VenterPåOppstart,
            TiltakDeltakerstatus.VenterPåOppstart,
        )
    }

    @Test
    fun `Arena IKKE_MOTT mappes til Avbrutt - paritet med dagens oppførsel`() {
        val ikkeMøtt = testdeltakelse(id = "TA1", kildestatus = Arenastatus.Kjent(Arenastatus.Type.IKKE_MOTT))

        val resultat = historikk(ikkeMøtt).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)

        resultat.value.single().deltakelseStatus shouldBe TiltakDeltakerstatus.Avbrutt
    }

    @Test
    fun `statusmappingen dekker alle tre kilder`() {
        val deltakelser = listOf(
            testdeltakelse(id = "TA1", kildestatus = Arenastatus.Kjent(Arenastatus.Type.AKTUELL)) to TiltakDeltakerstatus.SøktInn,
            testdeltakelse(id = "TA2", kildestatus = Arenastatus.Kjent(Arenastatus.Type.TILBUD)) to TiltakDeltakerstatus.VenterPåOppstart,
            testdeltakelse(id = "TA3", kildestatus = Arenastatus.Kjent(Arenastatus.Type.FEILREGISTRERT)) to TiltakDeltakerstatus.Feilregistrert,
            testdeltakelse(id = "TA4", kildestatus = kometstatus(Kometstatus.Type.UTKAST_TIL_PAMELDING)) to TiltakDeltakerstatus.PåbegyntRegistrering,
            testdeltakelse(id = "TA5", kildestatus = kometstatus(Kometstatus.Type.HAR_SLUTTET)) to TiltakDeltakerstatus.HarSluttet,
            testdeltakelse(id = "TA6", kildestatus = kometstatus(Kometstatus.Type.VURDERES)) to TiltakDeltakerstatus.Vurderes,
            testdeltakelse(id = "TA7", kildestatus = TeamTiltakstatus.Kjent(TeamTiltakstatus.Type.PAABEGYNT)) to TiltakDeltakerstatus.PåbegyntRegistrering,
            testdeltakelse(id = "TA8", kildestatus = TeamTiltakstatus.Kjent(TeamTiltakstatus.Type.GJENNOMFORES)) to TiltakDeltakerstatus.Deltar,
            testdeltakelse(id = "TA9", kildestatus = TeamTiltakstatus.Kjent(TeamTiltakstatus.Type.ANNULLERT)) to TiltakDeltakerstatus.IkkeAktuell,
        )

        val resultat = historikk(*deltakelser.map { it.first }.toTypedArray()).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)

        resultat.value.map { it.deltakelseStatus } shouldBe deltakelser.map { it.second }
    }

    @Test
    fun `Komet KLADD kaster - en kladd er ikke delt hos kilden og skal aldri nå oss`() {
        val kladd = testdeltakelse(id = "TA1", kildestatus = kometstatus(Kometstatus.Type.KLADD))

        shouldThrow<IllegalStateException> {
            historikk(kladd).tilTiltaksdeltakelserFraRegister(ingenSøktFor, fixedClock)
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
