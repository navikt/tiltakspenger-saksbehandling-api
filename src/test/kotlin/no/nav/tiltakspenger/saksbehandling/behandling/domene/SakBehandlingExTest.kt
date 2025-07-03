package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SakBehandlingExTest {
    @Test
    fun `send søknadsbehandling til beslutning`() {
        val clock = ObjectMother.clock
        val saksbehandler = ObjectMother.saksbehandler()
        val saksopplysninger = ObjectMother.saksopplysninger()
        val søknadsbehandling = ObjectMother.nyOpprettetSøknadsbehandling(
            saksbehandler = saksbehandler,
            hentSaksopplysninger = { saksopplysninger },
        )
        val sak = ObjectMother.nySak(behandlinger = Behandlinger(søknadsbehandling))
        val virkningsperiode = ObjectMother.virkningsperiode()
        val kommando = SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse(
            sakId = sak.id,
            behandlingId = søknadsbehandling.id,
            saksbehandler = saksbehandler,
            correlationId = CorrelationId("test-correlation-id"),
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            innvilgelsesperiode = virkningsperiode,
            barnetillegg = null,
            tiltaksdeltakelser = listOf(
                Pair(
                    virkningsperiode,
                    saksopplysninger.tiltaksdeltagelse[0].eksternDeltagelseId,
                ),
            ),
            antallDagerPerMeldeperiode = SammenhengendePeriodisering(AntallDagerForMeldeperiode(10), virkningsperiode),
        )

        søknadsbehandling.status shouldNotBe Behandlingsstatus.KLAR_TIL_BESLUTNING

        val result = sak.sendSøknadsbehandlingTilBeslutning(kommando, clock)

        result.isRight() shouldBe true
        val (oppdatertSak, oppdatertBehandling) = result.getOrNull()!!
        oppdatertBehandling.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
        oppdatertSak.behandlinger.any { it.id == oppdatertBehandling.id } shouldBe true
    }

    @Test
    fun `kan innvilge selv det en behandling med periode som tilstøter eller overlapper`() {
        val clock = ObjectMother.clock
        val saksbehandler = ObjectMother.saksbehandler()
        val virkningsperiode = ObjectMother.virkningsperiode()
        val saksopplysninger = ObjectMother.saksopplysninger(virkningsperiode.fraOgMed, virkningsperiode.tilOgMed)
        val sakId = SakId.random()
        val fnr = Fnr.random()

        val søknadsbehandling = ObjectMother.nyOpprettetSøknadsbehandling(
            fnr = fnr,
            sakId = sakId,
            hentSaksopplysninger = { saksopplysninger },
        )

        val overlappendeAvslåttSøknad =
            ObjectMother.nySøknadsbehandlingKlarTilBeslutning(
                sakId = sakId,
                fnr = fnr,
                saksopplysninger = saksopplysninger,
                virkningsperiode = virkningsperiode,
                resultat = SøknadsbehandlingType.AVSLAG,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
            )

        val sak = ObjectMother.nySak(
            behandlinger = Behandlinger(
                listOf(
                    søknadsbehandling,
                    overlappendeAvslåttSøknad,
                ),
            ),
        )
        val kommando = SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse(
            sakId = sak.id,
            behandlingId = søknadsbehandling.id,
            saksbehandler = saksbehandler,
            correlationId = CorrelationId("test-correlation-id"),
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            innvilgelsesperiode = Periode(
                fraOgMed = virkningsperiode.fraOgMed,
                tilOgMed = virkningsperiode.tilOgMed,
            ),
            barnetillegg = null,
            tiltaksdeltakelser = listOf(
                Pair(
                    virkningsperiode,
                    saksopplysninger.tiltaksdeltagelse[0].eksternDeltagelseId,
                ),
            ),
            antallDagerPerMeldeperiode = SammenhengendePeriodisering(AntallDagerForMeldeperiode(10), virkningsperiode),
        )

        val resultat = sak.sendSøknadsbehandlingTilBeslutning(kommando, clock)

        resultat.isRight() shouldBe true
    }
}
