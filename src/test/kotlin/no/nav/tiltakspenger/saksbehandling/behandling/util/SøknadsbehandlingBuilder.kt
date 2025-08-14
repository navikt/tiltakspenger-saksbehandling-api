package no.nav.tiltakspenger.saksbehandling.behandling.util

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import java.time.Clock

interface SøknadsbehandlingBuilder {

    // oppretter sak, søknad og behandling som er under automatisk behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad) = opprettSakOgSøknad(tac, fnr, deltakelsesperiode = virkningsperiode)
        val behandling = tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
            søknad,
            CorrelationId.generate(),
        )
        return Triple(sak, søknad, behandling)
    }

    // oppretter sak, søknad og automatisk behandling som er klar til beslutning
    suspend fun ApplicationTestBuilder.opprettAutomatiskBehandlingKlarTilBeslutning(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad) = opprettSakOgSøknad(tac, fnr, deltakelsesperiode = virkningsperiode)
        val behandling = tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
            søknad,
            CorrelationId.generate(),
        )
        tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())
        return Triple(sak, søknad, behandling)
    }

    // oppretter sak, søknad og behandling som er klar til manuell behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingKlarTilBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
            tac = tac,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
        )
        val behandlingOppdatert = behandling.copy(
            status = Behandlingsstatus.KLAR_TIL_BEHANDLING,
            saksbehandler = null,
        ).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Triple(sak, søknad, behandlingOppdatert)
    }

    // oppretter sak, søknad og behandling som er under manuell behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingKlarTilBehandling(
            tac = tac,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
        )

        val behandlingOppdatert = behandling.taBehandling(saksbehandler = saksbehandler).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        } as Søknadsbehandling

        return Triple(sak, søknad, behandlingOppdatert)
    }

    // oppretter sak, søknad og behandling som er under manuell behandling med data
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering? = null,
        barnetillegg: Barnetillegg? = null,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(10),
            virkningsperiode,
        ),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
        )
        behandling.oppdater(
            kommando = OppdaterSøknadsbehandlingKommando.Innvilgelse(
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                tiltaksdeltakelser = listOf(
                    Pair(
                        virkningsperiode,
                        behandling.saksopplysninger.tiltaksdeltagelser[0].eksternDeltagelseId,
                    ),
                ),
                automatiskSaksbehandlet = false,
                innvilgelsesperiode = virkningsperiode,
                barnetillegg = barnetillegg,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            ),
            clock = clock,
        ).getOrFail().also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Triple(sak, søknad, behandling)
    }

    // oppretter sak, søknad og behandling som er under manuell behandling med data
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandlingMedAvslag(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering? = null,
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag> = nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
        )
        behandling.oppdater(
            kommando = OppdaterSøknadsbehandlingKommando.Avslag(
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                tiltaksdeltakelser = listOf(
                    Pair(
                        virkningsperiode,
                        behandling.saksopplysninger.tiltaksdeltagelser[0].eksternDeltagelseId,
                    ),
                ),
                automatiskSaksbehandlet = false,
                avslagsgrunner = avslagsgrunner,
            ),
            clock = clock,
        ).getOrFail().also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Triple(sak, søknad, behandling)
    }
}
