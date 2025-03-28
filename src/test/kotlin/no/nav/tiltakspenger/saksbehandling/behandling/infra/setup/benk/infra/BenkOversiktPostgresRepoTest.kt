package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.benk.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurderingDeprecated
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
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
                                saksbehandler = førstegangsBehandling.saksbehandler!!,
                                beslutter = null,
                                sakId = førstegangsBehandlingSak.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = førstegangsBehandling.id,
                                opprettet = førstegangsBehandling.opprettet,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = null,
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.UNDER_BEHANDLING),
                                behandlingstype = BenkBehandlingstype.REVURDERING,
                                fnr = revurdering.fnr,
                                saksnummer = revurderingSak.saksnummer,
                                saksbehandler = revurdering.saksbehandler!!,
                                beslutter = null,
                                sakId = revurdering.sakId,
                                underkjent = false,
                                kravtidspunkt = null,
                                id = revurdering.id,
                                opprettet = revurdering.opprettet,
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
            val (sakMedAvbruttBehandling, vedtak, behandling) = testDataHelper.persisterIverksattFørstegangsbehandling(
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
                                saksbehandler = behandling.saksbehandler!!,
                                beslutter = null,
                                sakId = sakMedAvbruttBehandling.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = behandling.id,
                                opprettet = behandling.opprettet,
                            ),
                            BehandlingEllerSøknadForSaksoversikt(
                                periode = ObjectMother.virkningsperiode(),
                                status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(Behandlingsstatus.VEDTATT),
                                behandlingstype = BenkBehandlingstype.FØRSTEGANGSBEHANDLING,
                                fnr = søknad.fnr,
                                saksnummer = sakMedAvbruttBehandling.saksnummer,
                                saksbehandler = vedtak.saksbehandlerNavIdent,
                                beslutter = null,
                                sakId = sakMedAvbruttBehandling.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = vedtak.id,
                                opprettet = vedtak.opprettet,
                            ),

                        ),
                    )
            }
        }
    }
}
