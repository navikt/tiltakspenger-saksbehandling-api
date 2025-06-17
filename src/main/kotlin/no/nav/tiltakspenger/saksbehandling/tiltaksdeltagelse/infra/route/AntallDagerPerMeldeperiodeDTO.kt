package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO

data class AntallDagerPerMeldeperiodeDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
)
