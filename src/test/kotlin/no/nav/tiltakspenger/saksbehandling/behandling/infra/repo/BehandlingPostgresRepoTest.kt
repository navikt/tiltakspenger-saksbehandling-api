package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterUnderBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

internal class BehandlingPostgresRepoTest {

    @Test
    fun `lagre og hente en gammel flyt behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, _) = testDataHelper.persisterOpprettetSøknadsbehandling()
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

            val (sak, _) = testDataHelper.persisterOpprettetSøknadsbehandling(
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

            val (sak, behandling) = testDataHelper.persisterRevurderingTilBeslutning()
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.revurderinger.last().id) shouldBe behandling
        }
    }

    @Test
    fun `hentAlleForIdent skal kun hente behandlinger for en ident og ikke de andre`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo

            val (sak1, _) = testDataHelper.persisterOpprettetSøknadsbehandling()
            val (sak2, _) = testDataHelper.persisterOpprettetSøknadsbehandling()

            behandlingRepo.hentAlleForFnr(sak1.fnr) shouldBe sak1.behandlinger
            behandlingRepo.hentAlleForFnr(sak2.fnr) shouldBe sak2.behandlinger
        }
    }

    @Test
    fun `en saksbehandler kan ta behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val saksbehandler = ObjectMother.saksbehandler()
            val (_, behandling) = testDataHelper.persisterOpprettetSøknadsbehandling()
            testDataHelper.sessionFactory.withSession { sx ->
                sx.run(
                    queryOf(
                        """update behandling set saksbehandler = null where id = :id""",
                        mapOf("id" to behandling.id.toString()),
                    ).asUpdate,
                ) > 0
            }

            behandlingRepo.taBehandlingSaksbehandler(behandling.id, saksbehandler, Behandlingsstatus.UNDER_BEHANDLING)
            behandlingRepo.hent(behandling.id).saksbehandler shouldBe saksbehandler.navIdent
        }
    }

    @Test
    fun `en beslutter kan ta behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val beslutter = ObjectMother.beslutter()
            val (_, behandling) = testDataHelper.persisterKlarTilBeslutningSøknadsbehandling()

            behandling.beslutter shouldBe null
            behandlingRepo.taBehandlingBeslutter(behandling.id, beslutter, Behandlingsstatus.UNDER_BESLUTNING)
            behandlingRepo.hent(behandling.id).beslutter shouldBe beslutter.navIdent
        }
    }

    @Test
    fun `en saksbehandler kan overta behandlingen`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val nySaksbehandler = ObjectMother.saksbehandler("nySaksbehandler")
            val (_, behandling) = testDataHelper.persisterOpprettetSøknadsbehandling()

            behandling.saksbehandler shouldNotBe null
            behandling.saksbehandler shouldNotBe nySaksbehandler.navIdent
            behandlingRepo.overtaSaksbehandler(behandling.id, nySaksbehandler, behandling.saksbehandler!!)
            behandlingRepo.hent(behandling.id).saksbehandler shouldBe nySaksbehandler.navIdent
        }
    }

    @Test
    fun `en beslutter kan overta behandlingen`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val nyBeslutter = ObjectMother.beslutter("nyBeslutter")
            val (_, behandling) = testDataHelper.persisterUnderBeslutningSøknadsbehandling()

            behandling.beslutter shouldNotBe null
            behandling.beslutter shouldNotBe nyBeslutter.navIdent
            behandlingRepo.overtaBeslutter(behandling.id, nyBeslutter, behandling.beslutter!!)
            behandlingRepo.hent(behandling.id).beslutter shouldBe nyBeslutter.navIdent
        }
    }
}
