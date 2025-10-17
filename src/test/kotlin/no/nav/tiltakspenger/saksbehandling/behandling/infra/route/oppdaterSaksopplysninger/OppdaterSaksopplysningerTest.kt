package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
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
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

internal class OppdaterSaksopplysningerTest {
    @Test
    fun `søknadsbehandling - saksopplysninger blir oppdatert`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            behandling.saksopplysninger?.fødselsdato shouldBe 1.januar(2001)
            val personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(
                fnr = sak.fnr,
                fødselsdato = 2.januar(2001),
            )
            tac.leggTilPerson(
                fnr = sak.fnr,
                person = personopplysningerForBrukerFraPdl,
                tiltaksdeltagelse = Tiltaksdeltagelse(
                    eksternDeltagelseId = "TA12345",
                    gjennomføringId = null,
                    typeNavn = "Testnavn",
                    typeKode = TiltakstypeSomGirRett.JOBBKLUBB,
                    rettPåTiltakspenger = true,
                    deltagelseFraOgMed = behandling.saksopplysningsperiode!!.fraOgMed,
                    deltagelseTilOgMed = behandling.saksopplysningsperiode!!.tilOgMed,
                    deltakelseStatus = TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = 100.0f,
                    antallDagerPerUke = 5.0f,
                    kilde = Tiltakskilde.Arena,
                ),
            )
            val (oppdatertSak, oppdatertBehandling, responseJson) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                behandling.id,
            )
            oppdatertBehandling.saksopplysninger?.fødselsdato shouldBe 2.januar(2001)
        }
    }

    @Test
    fun `revurdering til omgjøring - kan oppdatere saksopplysninger`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingOmgjøring(tac)
            val forrigeTiltaksdeltagelse = revurdering.saksopplysninger.tiltaksdeltagelser.first()
            val avbruttTiltaksdeltagelse = forrigeTiltaksdeltagelse.copy(
                deltagelseFraOgMed = forrigeTiltaksdeltagelse.deltagelseFraOgMed!!,
                deltagelseTilOgMed = forrigeTiltaksdeltagelse.deltagelseTilOgMed!!.minusDays(1),
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )
            tac.oppdaterTiltaksdeltagelse(fnr = sak.fnr, tiltaksdeltagelse = avbruttTiltaksdeltagelse)
            val (_, oppdatertRevurdering: Rammebehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            // Forventer at saksopplysningene er oppdatert, mens resultatet er ubesudlet.
            (oppdatertRevurdering as Revurdering).saksopplysninger.tiltaksdeltagelser.single() shouldBe avbruttTiltaksdeltagelse
            oppdatertRevurdering.resultat shouldBe revurdering.resultat
            oppdatertRevurdering.erFerdigutfylt() shouldBe false
            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }
}
