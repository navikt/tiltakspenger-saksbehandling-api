package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param vedtaksdato Datoen vi bruker i brevet. Lagres samtidig som vi genererer og journalfører brevet. Vil være null fram til dette.
 */
data class RammevedtakDTO(
    val id: String,
    val behandlingId: String,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDate?,
    val vedtaksType: VedtakstypeDTO,
    val periode: PeriodeDTO,
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
        periode = periode.toDTO(),
        saksbehandler = saksbehandlerNavIdent,
        beslutter = beslutterNavIdent,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        barnetillegg = barnetillegg?.toBarnetilleggDTO(),
    )
}
