package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingType
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAutomatiskSøknadsbehandlingUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetOmgjøring
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingStansTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterUnderBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterRevurderingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandlerOgBeslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

internal class BehandlingPostgresRepoTest {

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
                    periodisering = SammenhengendePeriodisering(
                        AntallBarn(1),
                        Periode(deltakelseFom, deltakelseTom),
                    ),
                    begrunnelse = Begrunnelse.create("Begrunnelse"),
                ),
            )
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.rammebehandlinger.singleOrNullOrThrow()!!.id).also {
                it shouldBe sak.rammebehandlinger.singleOrNullOrThrow()
                it.barnetillegg shouldNotBe null
            }
        }
    }

    @Test
    fun `lagre og hente en opprettet revurdering`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, revurdering) = testDataHelper.persisterOpprettetRevurdering()

            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.revurderinger.single().id) shouldBe revurdering
        }
    }

    @Test
    fun `lagre og hente en behandlet revurdering`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, behandling) = testDataHelper.persisterRevurderingStansTilBeslutning()
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.revurderinger.last().id) shouldBe behandling
        }
    }

    @Test
    fun `lagrer og henter en omgjøring`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, omgjøring) = testDataHelper.persisterOpprettetOmgjøring()

            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.revurderinger.single().id) shouldBe omgjøring
        }
    }

    @Test
    fun `hentAlleForIdent skal kun hente behandlinger for en ident og ikke de andre`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo

            val (sak1, _) = testDataHelper.persisterOpprettetSøknadsbehandling()
            val (sak2, _) = testDataHelper.persisterOpprettetSøknadsbehandling()

            behandlingRepo.hentAlleForFnr(sak1.fnr) shouldBe sak1.rammebehandlinger
            behandlingRepo.hentAlleForFnr(sak2.fnr) shouldBe sak2.rammebehandlinger
        }
    }

    @Test
    fun `en saksbehandler kan ta behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val saksbehandler = saksbehandler()
            val (_, behandling) = testDataHelper.persisterOpprettetSøknadsbehandling()
            testDataHelper.sessionFactory.withSession { sx ->
                sx.run(
                    queryOf(
                        """update behandling set saksbehandler = null, status = 'KLAR_TIL_BEHANDLING' where id = :id""",
                        mapOf("id" to behandling.id.toString()),
                    ).asUpdate,
                ) > 0
            }

            behandlingRepo.taBehandlingSaksbehandler(
                rammebehandling = behandling.copy(
                    saksbehandler = saksbehandler.navIdent,
                    status = Rammebehandlingsstatus.UNDER_BEHANDLING,
                    sistEndret = nå(testDataHelper.clock),
                ),
            ) shouldBe true
            behandlingRepo.hent(behandling.id).saksbehandler shouldBe saksbehandler.navIdent
        }
    }

    @Test
    fun `en beslutter kan underkjenne en automatisk behandling og så ta behandlingen selv`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val beslutter = saksbehandlerOgBeslutter()

            val underkjentBehandling = testDataHelper.persisterAutomatiskSøknadsbehandlingUnderBeslutning(
                beslutter = beslutter,
            ).second.underkjenn(
                utøvendeBeslutter = beslutter,
                attestering = Attestering(
                    id = AttesteringId.random(),
                    status = Attesteringsstatus.SENDT_TILBAKE,
                    begrunnelse = NonBlankString.create("fordi"),
                    beslutter = beslutter.navIdent,
                    tidspunkt = nå(testDataHelper.clock),
                ),
                clock = testDataHelper.clock,
            ).also {
                behandlingRepo.lagre(it)
            }

            val behandlingId = underkjentBehandling.id

            val oppdatertBehandling = behandlingRepo.hent(behandlingId).taBehandling(
                saksbehandler = beslutter,
                clock = testDataHelper.clock,
            )

            val harTatt = behandlingRepo.taBehandlingSaksbehandler(
                rammebehandling = oppdatertBehandling,
            )
            harTatt shouldBe true

            val behandling = behandlingRepo.hent(behandlingId)

            behandling.saksbehandler shouldBe beslutter.navIdent
            behandling.beslutter.shouldBeNull()
        }
    }

    @Test
    fun `en beslutter kan underkjenne en manuell behandling og så overta behandlingen selv`() {
        withMigratedDb { testDataHelper ->
            val correlationId = CorrelationId.generate()
            val behandlingRepo = testDataHelper.behandlingRepo
            val saksbehandler = saksbehandler()
            val beslutter = saksbehandlerOgBeslutter()

            val underkjentBehandling = testDataHelper.persisterUnderBeslutningSøknadsbehandling(
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            ).second.underkjenn(
                utøvendeBeslutter = beslutter,
                attestering = Attestering(
                    id = AttesteringId.random(),
                    status = Attesteringsstatus.SENDT_TILBAKE,
                    begrunnelse = NonBlankString.create("fordi"),
                    beslutter = beslutter.navIdent,
                    tidspunkt = nå(testDataHelper.clock),
                ),
                clock = testDataHelper.clock,
            ).also {
                behandlingRepo.lagre(it)
            }

            val behandlingId = underkjentBehandling.id

            val clockOmToMinutter = testDataHelper.clock.plus(2L, ChronoUnit.MINUTES)

            val oppdatertBehandling = behandlingRepo.hent(behandlingId).overta(
                saksbehandler = beslutter,
                correlationId = correlationId,
                clock = clockOmToMinutter,
            ).getOrFail()

            val harOvertatt = behandlingRepo.overtaSaksbehandler(
                rammebehandling = oppdatertBehandling,
                saksbehandler.navIdent,
            )
            harOvertatt shouldBe true

            val behandling = behandlingRepo.hent(behandlingId)

            behandling.saksbehandler shouldBe beslutter.navIdent
            behandling.beslutter.shouldBeNull()
        }
    }

    @Test
    fun `en beslutter kan ta behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val beslutter = beslutter()
            val (_, behandling) = testDataHelper.persisterKlarTilBeslutningSøknadsbehandling()
            behandling as Søknadsbehandling

            behandling.beslutter shouldBe null
            behandlingRepo.taBehandlingBeslutter(
                rammebehandling = behandling.copy(
                    beslutter = beslutter.navIdent,
                    status = Rammebehandlingsstatus.UNDER_BESLUTNING,
                    sistEndret = nå(testDataHelper.clock),
                ),
            ) shouldBe true
            behandlingRepo.hent(behandling.id).beslutter shouldBe beslutter.navIdent
        }
    }

    @Test
    fun `en saksbehandler kan overta behandlingen`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val nySaksbehandler = saksbehandler("nySaksbehandler")
            val (_, behandling) = testDataHelper.persisterOpprettetSøknadsbehandling()

            behandling.saksbehandler shouldNotBe null
            behandling.saksbehandler shouldNotBe nySaksbehandler.navIdent

            behandlingRepo.overtaSaksbehandler(
                rammebehandling = behandling.copy(
                    saksbehandler = nySaksbehandler.navIdent,
                    sistEndret = nå(testDataHelper.clock),
                ),
                nåværendeSaksbehandler = behandling.saksbehandler!!,
            ) shouldBe true
            behandlingRepo.hent(behandling.id).saksbehandler shouldBe nySaksbehandler.navIdent
        }
    }

    @Test
    fun `en beslutter kan overta behandlingen`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val nyBeslutter = beslutter("nyBeslutter")
            val (_, behandling) = testDataHelper.persisterUnderBeslutningSøknadsbehandling()
            behandling as Søknadsbehandling
            behandling.beslutter shouldNotBe null
            behandling.beslutter shouldNotBe nyBeslutter.navIdent
            behandlingRepo.overtaBeslutter(
                rammebehandling = behandling.copy(
                    beslutter = nyBeslutter.navIdent,
                    sistEndret = nå(testDataHelper.clock),
                ),
                nåværendeBeslutter = behandling.beslutter!!,
            ) shouldBe true
            behandlingRepo.hent(behandling.id).beslutter shouldBe nyBeslutter.navIdent
        }
    }

    @Test
    fun `lagre og hente en behandling med mange innvilgelsesperioder`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val behandlingRepo = testDataHelper.behandlingRepo

            val tiltaksdeltakelse1 = tiltaksdeltakelse(
                eksternTiltaksdeltakelseId = "asdf",
                fom = 1.januar(2025),
                tom = 31.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val tiltaksdeltakelse2 = tiltaksdeltakelse(
                eksternTiltaksdeltakelseId = "qwer",
                fom = 1.mars(2025),
                tom = 30.juni(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val saksopplysninger = saksopplysninger(
                fom = 1.januar(2025),
                tom = 30.juni(2025),
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse1,
                    tiltaksdeltakelse2,
                ),
            )

            val (sak, behandling) = testDataHelper.persisterOpprettetRevurdering(
                revurderingType = RevurderingType.INNVILGELSE,
                hentSaksopplysninger = { _, _, _, _, _ -> saksopplysninger },
            )

            val innvilgelsesperioder: NonEmptyList<InnvilgelsesperiodeKommando> = nonEmptyListOf(
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = 1.januar(2025) til 10.januar(2025),
                    antallDagerPerMeldeperiode = 6,
                    tiltaksdeltakelse = tiltaksdeltakelse1,
                ),
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = 11.januar(2025) til 20.januar(2025),
                    antallDagerPerMeldeperiode = 10,
                    tiltaksdeltakelse = tiltaksdeltakelse1,
                ),
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = 21.januar(2025) til 30.april(2025),
                    antallDagerPerMeldeperiode = 4,
                    tiltaksdeltakelse = tiltaksdeltakelse1,
                ),
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = 1.mai(2025) til 31.mai(2025),
                    antallDagerPerMeldeperiode = 4,
                    tiltaksdeltakelse = tiltaksdeltakelse2,
                ),
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = 1.juni(2025) til 30.juni(2025),
                    antallDagerPerMeldeperiode = 2,
                    tiltaksdeltakelse = tiltaksdeltakelse2,
                ),
            )

            val oppdatertBehandling = behandling.oppdaterInnvilgelse(
                kommando = oppdaterRevurderingInnvilgelseKommando(
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    saksbehandler = saksbehandler(behandling.saksbehandler!!),
                    innvilgelsesperioder = innvilgelsesperioder,
                ),
                clock = testDataHelper.clock,
                utbetaling = null,
                omgjørRammevedtak = OmgjørRammevedtak.empty,
            ).getOrFail()

            behandlingRepo.lagre(oppdatertBehandling)

            val sakFraDb = sakRepo.hentForSakId(sak.id)!!
            val behandlingFraDb = behandlingRepo.hent(behandling.id)

            sakFraDb.rammebehandlinger.last() shouldBe behandlingFraDb

            behandlingFraDb.innvilgelsesperioder!! shouldBe innvilgelsesperioder(
                innvilgelsesperiode(
                    periode = 1.januar(2025) til 10.januar(2025),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse1,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(6),
                ),

                innvilgelsesperiode(
                    periode = 11.januar(2025) til 20.januar(2025),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse1,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                ),

                innvilgelsesperiode(
                    periode = 21.januar(2025) til 30.april(2025),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse1,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(4),
                ),

                innvilgelsesperiode(
                    periode = 1.mai(2025) til 31.mai(2025),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse2,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(4),
                ),

                innvilgelsesperiode(
                    periode = 1.juni(2025) til 30.juni(2025),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse2,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(2),
                ),
            )
        }
    }
}
