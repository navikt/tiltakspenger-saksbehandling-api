package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltagelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.toDTO
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.route.YtelseDTO
import java.time.LocalDateTime

data class SaksopplysningerDTO(
    val fødselsdato: String,
    val tiltaksdeltagelse: List<TiltaksdeltagelseDTO>,
    val periode: PeriodeDTO?,
    val ytelser: List<YtelseDTO>,
    val tiltakspengevedtakFraArena: List<ArenaTPVedtakDTO>,
    val oppslagstidspunkt: LocalDateTime,
)

fun Saksopplysninger.toSaksopplysningerDTO(): SaksopplysningerDTO {
    return SaksopplysningerDTO(
        fødselsdato = this.fødselsdato.toString(),
        tiltaksdeltagelse = this.tiltaksdeltakelser.map { it.toDTO() },
        periode = this.periode?.toDTO(),
        ytelser = this.ytelser.map { it.toDTO() },
        tiltakspengevedtakFraArena = this.tiltakspengevedtakFraArena.map { it.toDTO() },
        oppslagstidspunkt = this.oppslagstidspunkt,
    )
}
