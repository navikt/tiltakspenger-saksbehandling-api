package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

data class YtelseDbJson(
    val ytelsetype: String,
    val perioder: List<PeriodeDbJson>,
) {
    fun toDomain() =
        Ytelse(
            ytelsetype = Ytelsetype.valueOf(ytelsetype),
            perioder = perioder.map { it.toDomain() },
        )
}
