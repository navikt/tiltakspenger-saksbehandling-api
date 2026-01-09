package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
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
import no.nav.tiltakspenger.saksbehandling.objectmothers.KlokkeMother.clock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterRevurderingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandlerOgBeslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
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
                        """update behandling set saksbehandler = null where id = :id""",
                        mapOf("id" to behandling.id.toString()),
                    ).asUpdate,
                ) > 0
            }

            behandlingRepo.taBehandlingSaksbehandler(
                behandling.id,
                saksbehandler,
                Rammebehandlingsstatus.UNDER_BEHANDLING,
                LocalDateTime.now(),
            )
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
                    tidspunkt = nå(clock),
                ),
                clock = clock,
            ).also {
                behandlingRepo.lagre(it)
            }

            val behandlingId = underkjentBehandling.id

            behandlingRepo.hent(behandlingId).taBehandling(
                saksbehandler = beslutter,
                clock = clock,
            )

            val harTatt = behandlingRepo.taBehandlingSaksbehandler(
                behandlingId,
                beslutter,
                Rammebehandlingsstatus.UNDER_BEHANDLING,
                LocalDateTime.now(),
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
                    tidspunkt = nå(clock),
                ),
                clock = clock,
            ).also {
                behandlingRepo.lagre(it)
            }

            val behandlingId = underkjentBehandling.id

            val clockOmToMinutter = clock.plus(2L, ChronoUnit.MINUTES)

            behandlingRepo.hent(behandlingId).overta(
                saksbehandler = beslutter,
                clock = clockOmToMinutter,
            ).getOrFail()

            val harOvertatt = behandlingRepo.overtaSaksbehandler(
                behandlingId,
                beslutter,
                saksbehandler.navIdent,
                LocalDateTime.now(),
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

            behandling.beslutter shouldBe null
            behandlingRepo.taBehandlingBeslutter(
                behandling.id,
                beslutter,
                Rammebehandlingsstatus.UNDER_BESLUTNING,
                LocalDateTime.now(),
            )
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
                behandling.id,
                nySaksbehandler,
                behandling.saksbehandler!!,
                LocalDateTime.now(),
            )
            behandlingRepo.hent(behandling.id).saksbehandler shouldBe nySaksbehandler.navIdent
        }
    }

    @Test
    fun `en beslutter kan overta behandlingen`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val nyBeslutter = beslutter("nyBeslutter")
            val (_, behandling) = testDataHelper.persisterUnderBeslutningSøknadsbehandling()

            behandling.beslutter shouldNotBe null
            behandling.beslutter shouldNotBe nyBeslutter.navIdent
            behandlingRepo.overtaBeslutter(behandling.id, nyBeslutter, behandling.beslutter!!, LocalDateTime.now())
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
            )
            val tiltaksdeltakelse2 = tiltaksdeltakelse(
                eksternTiltaksdeltakelseId = "qwer",
                fom = 1.mars(2025),
                tom = 30.juni(2025),
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
                clock = clock,
                utbetaling = null,
                omgjørRammevedtak = OmgjørRammevedtak.empty,
            ).getOrFail()

            behandlingRepo.lagre(oppdatertBehandling)

            val sakFraDb = sakRepo.hentForSakId(sak.id)!!
            val behandlingFraDb = behandlingRepo.hent(behandling.id)

            sakFraDb.rammebehandlinger.last() shouldBe behandlingFraDb

            behandlingFraDb.innvilgelsesperioder!! shouldBe Innvilgelsesperioder(
                listOf(
                    Innvilgelsesperiode(
                        periode = 1.januar(2025) til 10.januar(2025),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse1,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(6),
                    ).tilPeriodeMedVerdi(),

                    Innvilgelsesperiode(
                        periode = 11.januar(2025) til 20.januar(2025),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse1,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                    ).tilPeriodeMedVerdi(),

                    Innvilgelsesperiode(
                        periode = 21.januar(2025) til 30.april(2025),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse1,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(4),
                    ).tilPeriodeMedVerdi(),

                    Innvilgelsesperiode(
                        periode = 1.mai(2025) til 31.mai(2025),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse2,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(4),
                    ).tilPeriodeMedVerdi(),

                    Innvilgelsesperiode(
                        periode = 1.juni(2025) til 30.juni(2025),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse2,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(2),
                    ).tilPeriodeMedVerdi(),
                ).tilIkkeTomPeriodisering(),
            )
        }
    }
}
