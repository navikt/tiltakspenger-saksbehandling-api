package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

enum class SøknadstypeDTO {
    DIGITAL,

    @Deprecated("Erstattet av mer spesifikke typer: PAPIR_SKJEMA, PAPIR_FRIHÅND, MODIA og ANNET")
    PAPIR,
    PAPIR_SKJEMA,
    PAPIR_FRIHÅND,
    MODIA,
    ANNET,
}

fun Søknadstype.toDTO(): SøknadstypeDTO {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDTO.DIGITAL
        Søknadstype.PAPIR -> SøknadstypeDTO.PAPIR
        Søknadstype.PAPIR_SKJEMA -> SøknadstypeDTO.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHÅND -> SøknadstypeDTO.PAPIR_FRIHÅND
        Søknadstype.MODIA -> SøknadstypeDTO.MODIA
        Søknadstype.ANNET -> SøknadstypeDTO.ANNET
    }
}

fun SøknadstypeDTO.tilDomene(): Søknadstype {
    return when (this) {
        SøknadstypeDTO.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDTO.PAPIR -> Søknadstype.PAPIR
        SøknadstypeDTO.PAPIR_SKJEMA -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDTO.PAPIR_FRIHÅND -> Søknadstype.PAPIR_FRIHÅND
        SøknadstypeDTO.MODIA -> Søknadstype.MODIA
        SøknadstypeDTO.ANNET -> Søknadstype.ANNET
    }
}
