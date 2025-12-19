package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

enum class SøknadstypeDTO {
    DIGITAL,

    @Deprecated("Erstattet av mer spesifikke typer: PAPIR_SKJEMA, PAPIR_FRIHAND, MODIA og ANNET")
    PAPIR,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    OVERFORT_FRA_ARENA,
    ANNET,
}

fun Søknadstype.toDTO(): SøknadstypeDTO {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDTO.DIGITAL
        Søknadstype.PAPIR -> SøknadstypeDTO.PAPIR
        Søknadstype.PAPIR_SKJEMA -> SøknadstypeDTO.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> SøknadstypeDTO.PAPIR_FRIHAND
        Søknadstype.MODIA -> SøknadstypeDTO.MODIA
        Søknadstype.OVERFORT_FRA_ARENA -> SøknadstypeDTO.OVERFORT_FRA_ARENA
        Søknadstype.ANNET -> SøknadstypeDTO.ANNET
    }
}

fun SøknadstypeDTO.tilDomene(): Søknadstype {
    return when (this) {
        SøknadstypeDTO.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDTO.PAPIR -> Søknadstype.PAPIR
        SøknadstypeDTO.PAPIR_SKJEMA -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDTO.PAPIR_FRIHAND -> Søknadstype.PAPIR_FRIHAND
        SøknadstypeDTO.MODIA -> Søknadstype.MODIA
        SøknadstypeDTO.OVERFORT_FRA_ARENA -> Søknadstype.OVERFORT_FRA_ARENA
        SøknadstypeDTO.ANNET -> Søknadstype.ANNET
    }
}
