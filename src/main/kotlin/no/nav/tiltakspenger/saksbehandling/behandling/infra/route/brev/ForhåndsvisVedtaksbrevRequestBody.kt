package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import arrow.core.toNonEmptySetOrThrow
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev.Companion.toFritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toAvslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toDomain
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevForRevurderingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevForRevurderingOmgjøringKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevForRevurderingStansKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevKommando
import java.time.LocalDate

/**
 * Skal kun brukes av route-laget.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(
        value = ForhåndsvisVedtaksbrevRequestBody.SøknadsbehandlingInnvilgelse::class,
        name = "INNVILGELSE",
    ),
    JsonSubTypes.Type(value = ForhåndsvisVedtaksbrevRequestBody.SøknadsbehandlingAvslag::class, name = "AVSLAG"),

    JsonSubTypes.Type(
        value = ForhåndsvisVedtaksbrevRequestBody.RevurderingInnvilgelse::class,
        name = "REVURDERING_INNVILGELSE",
    ),
    JsonSubTypes.Type(value = ForhåndsvisVedtaksbrevRequestBody.RevurderingStans::class, name = "STANS"),

    JsonSubTypes.Type(value = ForhåndsvisVedtaksbrevRequestBody.RevurderingOmgjøring::class, name = "OMGJØRING"),
)
sealed interface ForhåndsvisVedtaksbrevRequestBody {
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): ForhåndsvisVedtaksbrevKommando

    data class SøknadsbehandlingInnvilgelse(
        val fritekst: String?,
        val barnetillegg: List<BarnetilleggPeriodeDTO>?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
    ) : ForhåndsvisVedtaksbrevRequestBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            correlationId: CorrelationId,
            saksbehandler: Saksbehandler,
        ): ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando {
            return ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando(
                sakId = sakId,
                behandlingId = behandlingId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = fritekst?.toFritekstTilVedtaksbrev(),
                innvilgelsesperioder = innvilgelsesperioder.tilKommando(),
                barnetillegg = if (barnetillegg.isNullOrEmpty()) null else (barnetillegg.tilPeriodisering() as IkkeTomPeriodisering),
            )
        }
    }

    data class SøknadsbehandlingAvslag(
        val fritekst: String?,
        val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>,
    ) : ForhåndsvisVedtaksbrevRequestBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            correlationId: CorrelationId,
            saksbehandler: Saksbehandler,
        ): ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando {
            return ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando(
                sakId = sakId,
                behandlingId = behandlingId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = fritekst?.toFritekstTilVedtaksbrev(),
                avslagsgrunner = avslagsgrunner.toAvslagsgrunnlag(),
            )
        }
    }

    data class RevurderingInnvilgelse(
        val fritekst: String?,
        val barnetillegg: List<BarnetilleggPeriodeDTO>?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
    ) : ForhåndsvisVedtaksbrevRequestBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            correlationId: CorrelationId,
            saksbehandler: Saksbehandler,
        ): ForhåndsvisVedtaksbrevForRevurderingInnvilgelseKommando {
            return ForhåndsvisVedtaksbrevForRevurderingInnvilgelseKommando(
                sakId = sakId,
                behandlingId = behandlingId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = fritekst?.toFritekstTilVedtaksbrev(),
                innvilgelsesperioder = innvilgelsesperioder.tilKommando(),
                barnetillegg = if (barnetillegg.isNullOrEmpty()) null else (barnetillegg.tilPeriodisering() as IkkeTomPeriodisering),
            )
        }
    }

    data class RevurderingStans(
        val fritekst: String?,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
        val harValgtStansTilSisteDagSomGirRett: Boolean?,
        val stansFraOgMed: LocalDate?,
        val stansTilOgMed: LocalDate?,
        val valgteHjemler: List<ValgtHjemmelForStansDTO>,
    ) : ForhåndsvisVedtaksbrevRequestBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            correlationId: CorrelationId,
            saksbehandler: Saksbehandler,
        ): ForhåndsvisVedtaksbrevForRevurderingStansKommando {
            return ForhåndsvisVedtaksbrevForRevurderingStansKommando(
                sakId = sakId,
                behandlingId = behandlingId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = fritekst?.toFritekstTilVedtaksbrev(),
                valgteHjemler = valgteHjemler.toDomain().toNonEmptySetOrThrow(),
                stansFraOgMed = stansFraOgMed,
            )
        }
    }

    data class RevurderingOmgjøring(
        val fritekst: String?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
        val barnetillegg: List<BarnetilleggPeriodeDTO>?,
    ) : ForhåndsvisVedtaksbrevRequestBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            correlationId: CorrelationId,
            saksbehandler: Saksbehandler,
        ): ForhåndsvisVedtaksbrevForRevurderingOmgjøringKommando {
            return ForhåndsvisVedtaksbrevForRevurderingOmgjøringKommando(
                sakId = sakId,
                behandlingId = behandlingId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = fritekst?.toFritekstTilVedtaksbrev(),
                innvilgelsesperioder = innvilgelsesperioder.tilKommando(),
                barnetillegg = if (barnetillegg.isNullOrEmpty()) null else (barnetillegg.tilPeriodisering() as IkkeTomPeriodisering),
            )
        }
    }
}
