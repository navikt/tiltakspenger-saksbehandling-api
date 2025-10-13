package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import kotlin.collections.emptyList

internal class StartRevurderingTest {
    @Test
    fun `kan starte revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<RevurderingResultat.Stans>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.virkningsperiode shouldBe null
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
        }
    }

    @Test
    fun `kan starte revurdering innvilgelse`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(tac)
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<RevurderingResultat.Innvilgelse>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.virkningsperiode shouldBe null
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
        }
    }

    @Test
    fun `kan starte revurdering omgjøring`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingOmgjøring(tac)
            val søknadsvedtak: Rammevedtak = sak.vedtaksliste.single() as Rammevedtak
            val søknadsvedtakResultat = søknadsvedtak.behandling.resultat as SøknadsbehandlingResultat.Innvilgelse
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<RevurderingResultat.Omgjøring>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.virkningsperiode shouldBe søknadsvedtak.periode
            revurdering.resultat.omgjøringsperiode shouldBe søknadsvedtakResultat.virkningsperiode
            revurdering.resultat.innvilgelsesperiode shouldBe søknadsvedtakResultat.innvilgelsesperiode
            revurdering.barnetillegg shouldBe søknadsvedtak.barnetillegg
            revurdering.antallDagerPerMeldeperiode shouldBe søknadsvedtak.antallDagerPerMeldeperiode
            revurdering.valgteTiltaksdeltakelser shouldBe søknadsvedtak.valgteTiltaksdeltakelser
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
            revurdering.erFerdigutfylt() shouldBe false
        }
    }
}
