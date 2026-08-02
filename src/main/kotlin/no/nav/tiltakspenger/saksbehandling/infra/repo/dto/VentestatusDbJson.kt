package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus

private data class VentestatusDbJson(
    val ventestatusHendelser: List<VentestatusHendelseDbJson>,
)

fun String.toVentestatus(): Ventestatus =
    Ventestatus(
        ventestatusHendelser = deserialize<VentestatusDbJson>(this).ventestatusHendelser.map { it.toSattPåVentBegrunnelse() },
    )

fun Ventestatus.toDbJson(): String = serialize(
    VentestatusDbJson(
        ventestatusHendelser = ventestatusHendelser.map { it.toDbJson() },
    ),
)
