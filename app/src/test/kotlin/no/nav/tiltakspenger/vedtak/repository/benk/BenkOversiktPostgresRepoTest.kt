package no.nav.tiltakspenger.vedtak.repository.benk

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.db.persisterOpprettetFørstegangsbehandlingDeprecated
import no.nav.tiltakspenger.db.persisterOpprettetRevurdering
import no.nav.tiltakspenger.db.persisterSakOgSøknad
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.felles.januarDateTime
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.domene.benk.Saksoversikt
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BenkOversiktPostgresRepoTest {
    @Test
    fun `Hent alle søknader og behandlinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val repo = testDataHelper.saksoversiktRepo
            val søknad1 = testDataHelper.persisterSakOgSøknad()
            val sakId = testDataHelper.søknadRepo.hentSakIdForSoknad(søknad1.id)!!
            val (førstegangsBehandlingSak, førstegangsBehandling) = testDataHelper.persisterOpprettetFørstegangsbehandlingDeprecated()
            val (revurderingSak, revurdering) = testDataHelper.persisterOpprettetRevurdering()
            val behandlinger = repo.hentAlleBehandlinger()
            val søknader = repo.hentAlleSøknader()
            val benkOversikt = Saksoversikt(søknader + behandlinger)

            benkOversikt.also {
                it shouldBe
                    Saksoversikt(
                        listOf(
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = null,
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Søknad,
                                behandlingstype = BenkBehandlingstype.SØKNAD,
                                fnr = søknad1.fnr,
                                saksnummer = null,
                                saksbehandler = null,
                                beslutter = null,
                                sakId = sakId,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = søknad1.id,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = ObjectMother.vurderingsperiode(),
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.UNDER_BEHANDLING),
                                behandlingstype = BenkBehandlingstype.FØRSTEGANGSBEHANDLING,
                                fnr = førstegangsBehandling.fnr,
                                saksnummer = førstegangsBehandlingSak.saksnummer,
                                saksbehandler = førstegangsBehandlingSak.førstegangsbehandling!!.saksbehandler!!,
                                beslutter = null,
                                sakId = førstegangsBehandlingSak.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = førstegangsBehandlingSak.førstegangsbehandling!!.id,
                                erDeprecatedBehandling = true,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = ObjectMother.revurderingsperiode(),
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.UNDER_BEHANDLING),
                                behandlingstype = BenkBehandlingstype.REVURDERING,
                                fnr = revurdering.fnr,
                                saksnummer = revurderingSak.saksnummer,
                                saksbehandler = revurderingSak.førstegangsbehandling!!.saksbehandler!!,
                                beslutter = null,
                                sakId = revurderingSak.id,
                                underkjent = false,
                                kravtidspunkt = null,
                                id = revurderingSak.revurderinger.first().id,
                                erDeprecatedBehandling = true,
                            ),
                        ),
                    )
            }
        }
    }

    @Test
    fun `Henter ikke ferdigstilte behandlinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val repo = testDataHelper.saksoversiktRepo
            val (sak, søknad) = testDataHelper.persisterIverksattFørstegangsbehandling()
            val behandlinger = repo.hentAlleBehandlinger()
            val søknader = repo.hentAlleSøknader()
            val benkOversikt = Saksoversikt(søknader + behandlinger)

            benkOversikt.also {
                it shouldNotBe
                    Saksoversikt(
                        listOf(
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = ObjectMother.vurderingsperiode(),
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.VEDTATT),
                                behandlingstype = BenkBehandlingstype.FØRSTEGANGSBEHANDLING,
                                fnr = søknad.fnr,
                                saksnummer = sak.saksnummer,
                                saksbehandler = sak.førstegangsbehandling!!.saksbehandler!!,
                                beslutter = null,
                                sakId = sak.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = sak.førstegangsbehandling!!.id,
                            ),
                        ),
                    )
            }
        }
    }
}
