package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak

enum class KlagebehandlingsOmgjørÅrsakDbEnum {
    FEIL_LOVANVENDELSE,
    FEIL_REGELVERKSFORSTAAELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    PROSESSUELL_FEIL,
    ANNET,
    ;

    fun toDomain(): KlageOmgjøringsårsak {
        return when (this) {
            FEIL_LOVANVENDELSE -> KlageOmgjøringsårsak.FEIL_LOVANVENDELSE
            FEIL_REGELVERKSFORSTAAELSE -> KlageOmgjøringsårsak.FEIL_REGELVERKSFORSTAAELSE
            FEIL_ELLER_ENDRET_FAKTA -> KlageOmgjøringsårsak.FEIL_ELLER_ENDRET_FAKTA
            PROSESSUELL_FEIL -> KlageOmgjøringsårsak.PROSESSUELL_FEIL
            ANNET -> KlageOmgjøringsårsak.ANNET
        }
    }
}

fun String.toKlageOmgjøringsårsak(): KlageOmgjøringsårsak {
    return KlagebehandlingsOmgjørÅrsakDbEnum.valueOf(this).toDomain()
}

fun KlageOmgjøringsårsak.toDbEnum(): KlagebehandlingsOmgjørÅrsakDbEnum {
    return when (this) {
        KlageOmgjøringsårsak.FEIL_LOVANVENDELSE -> KlagebehandlingsOmgjørÅrsakDbEnum.FEIL_LOVANVENDELSE
        KlageOmgjøringsårsak.FEIL_REGELVERKSFORSTAAELSE -> KlagebehandlingsOmgjørÅrsakDbEnum.FEIL_REGELVERKSFORSTAAELSE
        KlageOmgjøringsårsak.FEIL_ELLER_ENDRET_FAKTA -> KlagebehandlingsOmgjørÅrsakDbEnum.FEIL_ELLER_ENDRET_FAKTA
        KlageOmgjøringsårsak.PROSESSUELL_FEIL -> KlagebehandlingsOmgjørÅrsakDbEnum.PROSESSUELL_FEIL
        KlageOmgjøringsårsak.ANNET -> KlagebehandlingsOmgjørÅrsakDbEnum.ANNET
    }
}
