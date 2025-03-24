package no.nav.tiltakspenger.saksbehandling.repository.behandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.db.persisterBehandletRevurdering
import no.nav.tiltakspenger.saksbehandling.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import org.junit.jupiter.api.Test
import java.util.Random

internal class BehandlingRepoTest {
    companion object {
        val random = Random()
    }

    @Test
    fun `lagre og hente en gammel flyt behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, _) = testDataHelper.persisterOpprettetFørstegangsbehandling()
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.behandlinger.singleOrNullOrThrow()!!.id) shouldBe sak.behandlinger.singleOrNullOrThrow()
        }
    }

    @Test
    fun `lagre og hente en behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo
            val deltakelseFom = 1.januar(2023)
            val deltakelseTom = 31.mars(2023)

            val (sak, _) = testDataHelper.persisterOpprettetFørstegangsbehandling(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
                barnetillegg = Barnetillegg(
                    periodisering = Periodisering(
                        PeriodeMedVerdi(AntallBarn(1), Periode(deltakelseFom, deltakelseTom)),
                    ),
                    begrunnelse = BegrunnelseVilkårsvurdering("Begrunnelse"),
                ),
            )
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.behandlinger.singleOrNullOrThrow()!!.id).also {
                it shouldBe sak.behandlinger.singleOrNullOrThrow()
                it.barnetillegg shouldNotBe null
            }
        }
    }

    @Test
    fun `lagre og hente en behandlet revurdering`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, behandling) = testDataHelper.persisterBehandletRevurdering()
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.revurderinger.last().id) shouldBe behandling
        }
    }

    @Test
    fun `hentAlleForIdent skal kun hente behandlinger for en ident og ikke de andre`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo

            val (sak1, _) = testDataHelper.persisterOpprettetFørstegangsbehandling()
            val (sak2, _) = testDataHelper.persisterOpprettetFørstegangsbehandling()

            behandlingRepo.hentAlleForFnr(sak1.fnr) shouldBe sak1.behandlinger
            behandlingRepo.hentAlleForFnr(sak2.fnr) shouldBe sak2.behandlinger
        }
    }
}
