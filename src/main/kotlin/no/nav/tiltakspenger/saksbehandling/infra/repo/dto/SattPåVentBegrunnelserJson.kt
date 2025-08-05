package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.SattPåVentBegrunnelse

private data class SattPåVentBegrunnelserJson(
    val sattPåVentBegrunnelser: List<SattPåVentBegrunnelseJson>,
)

internal fun List<SattPåVentBegrunnelse>.toDbJson(): String =
    serialize(
        SattPåVentBegrunnelserJson(
            sattPåVentBegrunnelser = this.map {
                it.toDbJson()
            },
        ),
    )
