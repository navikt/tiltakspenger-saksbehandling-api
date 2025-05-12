package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

fun String.toBehandlingsutfallDto(): BehandlingsutfallDTO = BehandlingsutfallDTO.valueOf(this)
