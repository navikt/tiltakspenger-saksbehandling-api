package no.nav.tiltakspenger.saksbehandling.behandling.util

import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadPåSakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO
import java.time.Clock

interface SøknadsbehandlingBuilder {

    // oppretter sak (hvis sakId er null), søknad og behandling som er under automatisk behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad) = if (sakId == null) {
            opprettSakOgSøknad(
                tac = tac,
                fnr = fnr,
                deltakelsesperiode = virkningsperiode,
                tiltaksdeltagelse = tiltaksdeltagelse,
            )
        } else {
            opprettSøknadPåSakId(
                tac = tac,
                sakId = sakId,
                deltakelsesperiode = virkningsperiode,
                tiltaksdeltagelse = tiltaksdeltagelse,
            )
        }
        søknad.shouldBeInstanceOf<InnvilgbarSøknad>()
        val behandling = tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
            soknad = søknad,
            correlationId = CorrelationId.generate(),
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
        søknad.shouldBeInstanceOf<InnvilgbarSøknad>()
        val behandling = tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
            søknad,
            CorrelationId.generate(),
        )
        tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())
        return Triple(sak, søknad, behandling)
    }

    /** oppretter sak (hvis sakId er null), søknad og behandling som er klar til manuell behandling */
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingKlarTilBehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            tiltaksdeltagelse = tiltaksdeltagelse,
        )
        val behandlingOppdatert = behandling.copy(
            status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
            saksbehandler = null,
        ).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Triple(sak, søknad, behandlingOppdatert)
    }

    /** oppretter sak  (hvis sakId er null), søknad og behandling som er under manuell behandling*/
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingKlarTilBehandling(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            tiltaksdeltagelse = tiltaksdeltagelse,
        )

        val behandlingOppdatert = behandling.taBehandling(saksbehandler = saksbehandler).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        } as Søknadsbehandling

        return Triple(sak, søknad, behandlingOppdatert)
    }

    /** oppretter sak (hvis sakId er null), søknad og behandling som er under manuell behandling med data*/
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering? = null,
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(10),
            virkningsperiode,
        ),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
            tiltaksdeltagelse = tiltaksdeltagelse,
        )
        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandling.id,
            oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.verdi,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.verdi,
                valgteTiltaksdeltakelser = nonEmptyListOf(
                    TiltaksdeltakelsePeriodeDTO(
                        eksternDeltagelseId = tiltaksdeltagelse.eksternDeltagelseId,
                        periode = virkningsperiode.toDTO(),
                    ),
                ),
                innvilgelsesperiode = virkningsperiode.toDTO(),
                barnetillegg = barnetillegg.toBarnetilleggDTO(),
                antallDagerPerMeldeperiodeForPerioder = antallDagerPerMeldeperiode.toDTO(),
            ),
        )
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
                automatiskSaksbehandlet = false,
                avslagsgrunner = avslagsgrunner,
            ),
            clock = clock,
            utbetaling = null,
        ).getOrFail().also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Triple(sak, søknad, behandling)
    }
}
