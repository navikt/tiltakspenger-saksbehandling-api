package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.maksAntallDager
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
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
    val resultat: RammebehandlingResultatTypeDTO,
    val periode: PeriodeDTO,
    val gjeldendePeriode: PeriodeDTO,
    val saksbehandler: String,
    val beslutter: String,
    val antallDagerPerMeldeperiode: Int,
    val barnetillegg: BarnetilleggDTO?,
)

fun Rammevedtak.tilRammevedtakDTO(): RammevedtakDTO {
    val periodeDTO = periode.toDTO()

    return RammevedtakDTO(
        id = id.toString(),
        behandlingId = behandling.id.toString(),
        opprettet = opprettet,
        vedtaksdato = vedtaksdato,
        resultat = resultat.tilRammebehandlingResultatTypeDTO(),
        periode = periodeDTO,
        gjeldendePeriode = periodeDTO,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        // TODO: sett en periodisering istedenfor bare maks
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.maksAntallDager(),
        barnetillegg = barnetillegg?.toBarnetilleggDTO(),
    )
}

fun Rammevedtaksliste.tilRammevedtakTidslinjeDTO(): List<RammevedtakDTO> {
    return tidslinje.perioderMedVerdi.map {
        it.verdi.tilRammevedtakDTO().copy(
            gjeldendePeriode = it.periode.toDTO(),
            barnetillegg = it.verdi.barnetillegg?.tilKrympetBarnetilleggDTO(it.periode),
        )
    }
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
