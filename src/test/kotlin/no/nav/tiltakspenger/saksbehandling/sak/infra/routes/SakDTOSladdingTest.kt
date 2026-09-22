package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.sladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.sladdet
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.sladdet
import org.junit.jupiter.api.Test

class SakDTOSladdingTest {

    @Test
    fun `alle personopplysningsfelter i SakDTO sladdes mens øvrige felter forblir uendret`() {
        val sakDTO = sakDTO()

        sakDTO.sladdet() shouldBe sakDTO.copy(
            fnr = SladdetVerdi,
            søknader = sakDTO.søknader.map { it.sladdet() },
            rammebehandlinger = sakDTO.rammebehandlinger.map { it.sladdet() },
            klagebehandlinger = sakDTO.klagebehandlinger.map { it.sladdet() },
            alleRammevedtak = sakDTO.alleRammevedtak.map { it.sladdet() },
            meldekortbehandlinger = sakDTO.meldekortbehandlinger.mapValues { (_, behandling) -> behandling.sladdet() },
        )
    }

    @Test
    fun `sladdingen når ned i søknadens barnetillegg og behandlingens saksopplysninger`() {
        sakDTO().sladdet().let {
            it.fnr shouldBe SladdetVerdi
            it.søknader.single().barnetillegg.map { barn -> barn.fornavn } shouldBe
                listOf(SladdetVerdi, SladdetVerdi)
            it.rammebehandlinger.single().saksopplysninger.fødselsdato shouldBe SladdetVerdi
        }
    }

    @Test
    fun `saksnummer og id-er beholdes`() {
        val sakDTO = sakDTO()

        sakDTO.sladdet().let {
            it.saksnummer shouldBe sakDTO.saksnummer
            it.sakId shouldBe sakDTO.sakId
            it.åpneBehandlinger shouldBe sakDTO.åpneBehandlinger
            it.rammebehandlinger.single().id shouldBe sakDTO.rammebehandlinger.single().id
        }
    }

    @Test
    fun `saksbehandler får full sak med urørte personopplysninger`() {
        val sakDTO = sakDTO()

        sakDTO.sladdetFor(ObjectMother.saksbehandler()) shouldBe sakDTO
    }

    @Test
    fun `bruker med både utvikler og saksbehandlerrolle får ikke sladdet saken`() {
        val sakDTO = sakDTO()

        sakDTO.sladdetFor(
            ObjectMother.saksbehandler(
                roller = Saksbehandlerroller(
                    listOf(Saksbehandlerrolle.UTVIKLER, Saksbehandlerrolle.SAKSBEHANDLER),
                ),
            ),
        ) shouldBe sakDTO
    }

    @Test
    fun `bruker uten fagrolle får sladdet sak`() {
        val sakDTO = sakDTO()

        sakDTO.sladdetFor(ObjectMother.utvikler()) shouldBe sakDTO.sladdet()
    }

    private fun sakDTO(): SakDTO {
        val sakId = SakId.random()
        val saksnummer = ObjectMother.nesteSaksnummer()
        val fnr = Fnr.random()
        val vedtaksperiode = Periode(1.januar(2023), 31.januar(2023))

        val (sak) = ObjectMother.sakMedOpprettetBehandling(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            vedtaksperiode = vedtaksperiode,
            søknad = ObjectMother.nyInnvilgbarSøknad(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                barnetillegg = listOf(
                    ObjectMother.barnetilleggMedIdent(fnr = Fnr.random()),
                    ObjectMother.barnetilleggUtenIdent(),
                ),
                søknadstiltak = ObjectMother.søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                ),
            ),
        )

        return sak.toSakDTO(ObjectMother.saksbehandler(), fixedClock)
    }
}
