package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param vedtaksdato Datoen vi bruker i brevet. Lagres samtidig som vi genererer og journalfører brevet. Vil være null fram til dette.
 * @param periode Den opprinnelige perioden for vedtaket
 * @param gjeldendePeriode Perioden der vedtaket fortsatt er gjeldende for sakens nå-tilstand
 */
data class RammevedtakDTO(
    val id: String,
    val behandlingId: String,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDate?,
    val vedtaksType: VedtakstypeDTO,
    val periode: PeriodeDTO,
    val gjeldendePeriode: PeriodeDTO,
    val saksbehandler: String,
    val beslutter: String,
    val antallDagerPerMeldeperiode: Int,
    val barnetillegg: BarnetilleggDTO?,
)

enum class VedtakstypeDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
}

fun Rammevedtak.tilRammevedtakDTO(): RammevedtakDTO {
    val periodeDTO = periode.toDTO()

    return RammevedtakDTO(
        id = id.toString(),
        behandlingId = behandling.id.toString(),
        opprettet = opprettet,
        vedtaksdato = vedtaksdato,
        vedtaksType = when (vedtakstype) {
            Vedtakstype.INNVILGELSE -> VedtakstypeDTO.INNVILGELSE
            Vedtakstype.AVSLAG -> VedtakstypeDTO.AVSLAG
            Vedtakstype.STANS -> VedtakstypeDTO.STANS
        },
        periode = periodeDTO,
        gjeldendePeriode = periodeDTO,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        barnetillegg = barnetillegg?.toBarnetilleggDTO(),
    )
}

fun PeriodeMedVerdi<Rammevedtak>.tilPeriodisertRammevedtakDTO(): RammevedtakDTO {
    return verdi.tilRammevedtakDTO().copy(
        gjeldendePeriode = periode.toDTO(),
        barnetillegg = verdi.barnetillegg?.tilKrympetBarnetilleggDTO(periode),
    )
}

private fun Barnetillegg.tilKrympetBarnetilleggDTO(periode: Periode): BarnetilleggDTO = BarnetilleggDTO(
    perioder = periodisering.krymp(periode).perioderMedVerdi.map {
        BarnetilleggPeriodeDTO(
            antallBarn = it.verdi.value,
            periode = it.periode.toDTO(),
        )
    },
    begrunnelse = begrunnelse?.verdi?.let { saniter(it) },
)
