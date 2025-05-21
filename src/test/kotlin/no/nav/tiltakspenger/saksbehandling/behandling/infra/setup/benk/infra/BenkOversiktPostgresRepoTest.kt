package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.benk.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurderingDeprecated
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
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
            val (søknadsbehandlingSak, søknadsbehandling) = testDataHelper.persisterOpprettetSøknadsbehandling()
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
                                behandlingstype = BenkBehandlingstype.SØKNADSBEHANDLING,
                                fnr = søknadsbehandling.fnr,
                                saksnummer = søknadsbehandlingSak.saksnummer,
                                saksbehandler = søknadsbehandling.saksbehandler!!,
                                beslutter = null,
                                sakId = søknadsbehandlingSak.id,
                                underkjent = false,
                                kravtidspunkt = LocalDateTime.from(1.januarDateTime(2022)),
                                id = søknadsbehandling.id,
                                opprettet = søknadsbehandling.opprettet,
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
            val (sak, søknad) = testDataHelper.persisterAvbruttSøknadsbehandling()
            val (sakMedAvbruttBehandling, vedtak, behandling) = testDataHelper.persisterIverksattSøknadsbehandling(
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
                                behandlingstype = BenkBehandlingstype.SØKNADSBEHANDLING,
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
                                behandlingstype = BenkBehandlingstype.SØKNADSBEHANDLING,
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
