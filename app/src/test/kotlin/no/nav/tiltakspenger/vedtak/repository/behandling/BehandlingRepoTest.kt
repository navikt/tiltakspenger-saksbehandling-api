package no.nav.tiltakspenger.vedtak.repository.behandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.db.persisterBehandletRevurdering
import no.nav.tiltakspenger.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
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
            behandlingRepo.hent(sak.førstegangsbehandling!!.id) shouldBe sak.førstegangsbehandling
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
                    value = Periodisering(
                        PeriodeMedVerdi(1, Periode(deltakelseFom, deltakelseTom)),
                    ),
                    begrunnelse = BegrunnelseVilkårsvurdering("Begrunnelse"),
                ),
            )
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.førstegangsbehandling!!.id).also {
                it shouldBe sak.førstegangsbehandling
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

            behandlingRepo.hentAlleForFnr(sak1.fnr) shouldBe listOf(sak1.førstegangsbehandling)
            behandlingRepo.hentAlleForFnr(sak2.fnr) shouldBe listOf(sak2.førstegangsbehandling)
        }
    }
}
