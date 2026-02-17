package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

enum class SøknadstypeDTO {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
}

fun Søknadstype.toDTO(): SøknadstypeDTO {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDTO.DIGITAL
        Søknadstype.PAPIR_SKJEMA -> SøknadstypeDTO.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> SøknadstypeDTO.PAPIR_FRIHAND
        Søknadstype.MODIA -> SøknadstypeDTO.MODIA
        Søknadstype.ANNET -> SøknadstypeDTO.ANNET
    }
}

fun SøknadstypeDTO.tilDomene(): Søknadstype {
    return when (this) {
        SøknadstypeDTO.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDTO.PAPIR_SKJEMA -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDTO.PAPIR_FRIHAND -> Søknadstype.PAPIR_FRIHAND
        SøknadstypeDTO.MODIA -> Søknadstype.MODIA
        SøknadstypeDTO.ANNET -> Søknadstype.ANNET
    }
}
