package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO

data class AntallDagerForMeldeperiodeDTO(
    val dager: Int,
    val periode: PeriodeDTO,
)
