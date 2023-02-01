package no.nav.tiltakspenger.vedtak

import no.nav.tiltakspenger.felles.UføreVedtakId
import java.time.LocalDate
import java.time.LocalDateTime

data class UføreVedtak(
    val id: UføreVedtakId,
    val harUforegrad: Boolean,
    val datoUfor: LocalDate?,
    val virkDato: LocalDate?,
    val innhentet: LocalDateTime,
) : Tidsstempler {

    override fun tidsstempelKilde() = innhentet

    override fun tidsstempelHosOss() = innhentet
}
