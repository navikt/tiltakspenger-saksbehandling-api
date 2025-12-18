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
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.tilAntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadPåSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO
import java.time.Clock

interface SøknadsbehandlingBuilder {

    // oppretter sak (hvis sakId er null), søknad og behandling som er under automatisk behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        tiltaksdeltakelsesperiode: Periode = 1.til(10.april(2025)),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = tiltaksdeltakelsesperiode.fraOgMed,
            tom = tiltaksdeltakelsesperiode.tilOgMed,
        ),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad) = if (sakId == null) {
            opprettSakOgSøknad(
                tac = tac,
                fnr = fnr,
                deltakelsesperiode = tiltaksdeltakelsesperiode,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
        } else {
            opprettSøknadPåSakId(
                tac = tac,
                sakId = sakId,
                deltakelsesperiode = tiltaksdeltakelsesperiode,
                tiltaksdeltakelse = tiltaksdeltakelse,
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
        vedtaksperiode: Periode = 1.til(10.april(2025)),
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad) = opprettSakOgSøknad(tac, fnr, deltakelsesperiode = vedtaksperiode)
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
        tiltaksdeltakelsesperiode: Periode = 1.til(10.april(2025)),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = tiltaksdeltakelsesperiode.fraOgMed,
            tom = tiltaksdeltakelsesperiode.tilOgMed,
        ),
        manueltBehandlesGrunner: List<ManueltBehandlesGrunn> = emptyList(),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            tiltaksdeltakelsesperiode = tiltaksdeltakelse.periode!!,
            tiltaksdeltakelse = tiltaksdeltakelse,
        )

        behandling.tilManuellBehandling(
            manueltBehandlesGrunner = manueltBehandlesGrunner,
            clock = clock,
        )

        val behandlingOppdatert = behandling.tilManuellBehandling(
            manueltBehandlesGrunner = manueltBehandlesGrunner,
            clock = clock,
        ).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }

        val oppdaterSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!

        return Triple(oppdaterSak, søknad, behandlingOppdatert)
    }

    /** oppretter sak  (hvis sakId er null), søknad og behandling som er under manuell behandling*/
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        tiltaksdeltakelsesperiode: Periode = 1.til(10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = tiltaksdeltakelsesperiode.fraOgMed,
            tom = tiltaksdeltakelsesperiode.tilOgMed,
        ),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingKlarTilBehandling(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            tiltaksdeltakelsesperiode = tiltaksdeltakelsesperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            clock = clock,
        )

        val (oppdaterSak, behandlingOppdatert) = taBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandling.id,
            saksbehandler = saksbehandler,
        )

        return Triple(oppdaterSak, søknad, behandlingOppdatert as Søknadsbehandling)
    }

    /** oppretter sak (hvis sakId er null), søknad og behandling som er under manuell behandling med data*/
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        innvilgelsesperiode: Periode = 1.til(10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
        begrunnelseVilkårsvurdering: Begrunnelse? = null,
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperiode),
        antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(10),
            innvilgelsesperiode,
        ),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = innvilgelsesperiode.fraOgMed,
            tom = innvilgelsesperiode.tilOgMed,
        ),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            tiltaksdeltakelsesperiode = innvilgelsesperiode,
            saksbehandler = saksbehandler,
            tiltaksdeltakelse = tiltaksdeltakelse,
            clock = clock,
        )

        val (oppdatertSak, oppdatertBehandling) = oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandling.id,
            oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.verdi,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.verdi,
                valgteTiltaksdeltakelser = nonEmptyListOf(
                    TiltaksdeltakelsePeriodeDTO(
                        eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                        periode = innvilgelsesperiode.toDTO(),
                    ),
                ),
                innvilgelsesperiode = innvilgelsesperiode.toDTO(),
                barnetillegg = barnetillegg.toBarnetilleggDTO(),
                antallDagerPerMeldeperiodeForPerioder = antallDagerPerMeldeperiode.tilAntallDagerPerMeldeperiodeDTO(),
            ),
        )

        return Triple(oppdatertSak, søknad, oppdatertBehandling as Søknadsbehandling)
    }

    // oppretter sak, søknad og behandling som er under manuell behandling med data
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandlingMedAvslag(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        tiltaksdeltakelsesperiode: Periode = 1.til(10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
        begrunnelseVilkårsvurdering: Begrunnelse? = null,
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag> = nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
        clock: Clock = fixedClock,
    ): Triple<Sak, Søknad, Søknadsbehandling> {
        val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            fnr = fnr,
            tiltaksdeltakelsesperiode = tiltaksdeltakelsesperiode,
            saksbehandler = saksbehandler,
            clock = clock,
        )

        val (oppdatertSak, oppdatertBehandling) = oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandling.id,
            saksbehandler = saksbehandler,
            oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Avslag(
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.verdi,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.verdi,
                avslagsgrunner = avslagsgrunner.toValgtHjemmelForAvslagDTO(),
            ),
        )

        return Triple(oppdatertSak, søknad, oppdatertBehandling as Søknadsbehandling)
    }
}
