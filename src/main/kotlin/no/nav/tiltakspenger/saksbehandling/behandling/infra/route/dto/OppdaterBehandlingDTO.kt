package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando.Innvilgelse.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Innvilgelse::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Avslag::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.IkkeValgtResultat::class, name = "IKKE_VALGT"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Innvilgelse::class, name = "REVURDERING_INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Stans::class, name = "STANS"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Omgjøring::class, name = "OMGJØRING"),
)
sealed interface OppdaterBehandlingDTO {
    val resultat: RammebehandlingResultatTypeDTO?
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?

    fun tilDomene(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): OppdaterBehandlingKommando

    // Midlertidig løsning for å mappe eksisterende frontend-requester til ny modell for innvilgelsesperioder
    fun tilInnvilgelsesperioderKommando(
        innvilgelsesperiode: PeriodeDTO,
        antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>,
        tiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
    ): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
        val innvilgelsesperiode = innvilgelsesperiode.toDomain()

        val antallDagerPeriodisert = antallDagerPerMeldeperiode.map {
            PeriodeMedVerdi(
                periode = it.periode.toDomain(),
                verdi = it.antallDagerPerMeldeperiode,
            )
        }.tilSammenhengendePeriodisering()

        val tiltaksIdPeriodisert = tiltaksdeltakelser.map {
            PeriodeMedVerdi(periode = it.periode.toDomain(), verdi = it.eksternDeltagelseId)
        }.tilSammenhengendePeriodisering()

        require(antallDagerPeriodisert.totalPeriode == innvilgelsesperiode) {
            "Periodisering av antall dager må ha totalperiode lik innvilgelsesperioden"
        }

        require(tiltaksIdPeriodisert.totalPeriode == innvilgelsesperiode) {
            "Periodisering av tiltaksdeltakelse må ha totalperiode lik innvilgelsesperioden"
        }

        val unikePerioder = nonEmptyListOf(innvilgelsesperiode)
            .plus(antallDagerPeriodisert.perioder)
            .plus(tiltaksIdPeriodisert.perioder)
            .tilPerioderUtenHullEllerOverlapp()

        return unikePerioder.map {
            PeriodeMedVerdi(
                periode = it,
                verdi = InnvilgelsesperiodeKommando(
                    periode = it,
                    antallDagerPerMeldeperiode = antallDagerPeriodisert.hentVerdiForDag(it.fraOgMed)!!,
                    tiltaksdeltakelseId = tiltaksIdPeriodisert.hentVerdiForDag(it.fraOgMed)!!,
                ),
            )
        }.tilIkkeTomPeriodisering()
    }
}

private fun NonEmptyList<Periode>.tilPerioderUtenHullEllerOverlapp(): NonEmptyList<Periode> {
    val unikePerioder: NonEmptyList<Periode> = this.distinct()

    if (unikePerioder.size == 1) {
        return unikePerioder
    }

    val sisteTilOgMed: LocalDate = unikePerioder.maxOf { it.tilOgMed }

    val fraOgMedDatoer = unikePerioder.toList().flatMap { periode ->
        if (periode.tilOgMed == sisteTilOgMed) {
            listOf(periode.fraOgMed)
        } else {
            listOf(periode.fraOgMed, periode.tilOgMed.plusDays(1))
        }
    }.distinct().sorted()

    return fraOgMedDatoer.mapIndexed { index, fraOgMed ->
        val nesteFraOgMed = fraOgMedDatoer.getOrNull(index + 1)

        val tilOgMed = nesteFraOgMed?.minusDays(1) ?: sisteTilOgMed

        Periode(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
        )
    }.toNonEmptyListOrNull()!!
}
