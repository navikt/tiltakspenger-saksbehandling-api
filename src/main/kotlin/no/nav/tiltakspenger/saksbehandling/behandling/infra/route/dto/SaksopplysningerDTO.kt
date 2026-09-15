package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.toDTO
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.route.YtelseDTO
import java.time.LocalDate
import java.time.LocalDateTime

data class SaksopplysningerDTO(
    val fødselsdato: SladdbarVerdi<LocalDate>,
    val tiltaksdeltagelse: List<TiltaksdeltakelseDTO>,
    val periode: PeriodeDTO?,
    val ytelser: List<YtelseDTO>,
    val tiltakspengevedtakFraArena: List<ArenaTPVedtakDTO>,
    val oppslagstidspunkt: LocalDateTime,
)

fun Saksopplysninger.toSaksopplysningerDTO(): SaksopplysningerDTO {
    return SaksopplysningerDTO(
        fødselsdato = this.fødselsdato.ikkeSladdet(),
        tiltaksdeltagelse = this.tiltaksdeltakelser.map { it.toDTO() },
        periode = this.periode?.toDTO(),
        ytelser = this.ytelser.map { it.toDTO() },
        tiltakspengevedtakFraArena = this.tiltakspengevedtakFraArena.map { it.toDto() },
        oppslagstidspunkt = this.oppslagstidspunkt,
    )
}
