package no.nav.tiltakspenger.saksbehandling.repository.benk

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.common.januarDateTime
import no.nav.tiltakspenger.db.persisterAvbruttFørstegangsbehandling
import no.nav.tiltakspenger.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.db.persisterOpprettetRevurderingDeprecated
import no.nav.tiltakspenger.db.persisterSakOgSøknad
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.Saksoversikt
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BenkOversiktPostgresRepoTest {
    @Test
    fun `Hent alle søknader og behandlinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val repo = testDataHelper.saksoversiktRepo
            val søknad1 = testDataHelper.persisterSakOgSøknad()
            val sakId = testDataHelper.søknadRepo.hentSakIdForSoknad(søknad1.id)!!
            val (førstegangsBehandlingSak, førstegangsBehandling) = testDataHelper.persisterOpprettetFørstegangsbehandling()
            val (revurderingSak, revurdering) = testDataHelper.persisterOpprettetRevurderingDeprecated()
            val behandlinger = repo.hentÅpneBehandlinger()
            val søknader = repo.hentÅpneSøknader()
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
                                saksnummer = søknad1.saksnummer,
                                saksbehandler = null,
                                beslutter = null,
                                sakId = sakId,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = søknad1.id,
                                opprettet = søknad1.opprettet,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = null,
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
                                opprettet = førstegangsBehandlingSak.førstegangsbehandling!!.opprettet,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = null,
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
                                opprettet = revurderingSak.revurderinger.first().opprettet,
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
            val (sak, søknad) = testDataHelper.persisterAvbruttFørstegangsbehandling()
            val (sakMedAvbruttBehandling, behandling) = testDataHelper.persisterIverksattFørstegangsbehandling(
                sakId = sak.id,
                fnr = søknad.fnr,
                sak = sak,
            )

            val behandlinger = repo.hentÅpneBehandlinger()
            val søknader = repo.hentÅpneSøknader()
            val benkOversikt = Saksoversikt(søknader + behandlinger)

            benkOversikt.also {
                it shouldNotBe
                    Saksoversikt(
                        listOf(
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = ObjectMother.virkningsperiode(),
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.AVBRUTT),
                                behandlingstype = BenkBehandlingstype.FØRSTEGANGSBEHANDLING,
                                fnr = søknad.fnr,
                                saksnummer = sakMedAvbruttBehandling.saksnummer,
                                saksbehandler = sakMedAvbruttBehandling.førstegangsbehandling!!.saksbehandler!!,
                                beslutter = null,
                                sakId = sakMedAvbruttBehandling.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = sakMedAvbruttBehandling.førstegangsbehandling!!.id,
                                opprettet = sakMedAvbruttBehandling.førstegangsbehandling!!.opprettet,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = ObjectMother.virkningsperiode(),
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.VEDTATT),
                                behandlingstype = BenkBehandlingstype.FØRSTEGANGSBEHANDLING,
                                fnr = søknad.fnr,
                                saksnummer = sakMedAvbruttBehandling.saksnummer,
                                saksbehandler = behandling.saksbehandlerNavIdent,
                                beslutter = null,
                                sakId = sakMedAvbruttBehandling.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = behandling.id,
                                opprettet = behandling.opprettet,
                            ),

                        ),
                    )
            }
        }
    }
}
