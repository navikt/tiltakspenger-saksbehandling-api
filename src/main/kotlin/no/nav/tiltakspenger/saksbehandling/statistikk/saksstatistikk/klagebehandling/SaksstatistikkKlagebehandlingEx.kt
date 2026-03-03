package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling

import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkFormat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkResultatBegrunnelse

fun KlageInnsendingskilde.toStatistikkFormat(): StatistikkFormat {
    return when (this) {
        KlageInnsendingskilde.DIGITAL -> StatistikkFormat.DIGITAL
        KlageInnsendingskilde.PAPIR_SKJEMA -> StatistikkFormat.PAPIR_SKJEMA
        KlageInnsendingskilde.PAPIR_FRIHAND -> StatistikkFormat.PAPIR_FRIHAND
        KlageInnsendingskilde.MODIA -> StatistikkFormat.MODIA
        KlageInnsendingskilde.ANNET -> StatistikkFormat.ANNET
    }
}

fun KlageOmgjøringsårsak.tilResultatBegrunnelse(): StatistikkResultatBegrunnelse {
    return when (this) {
        KlageOmgjøringsårsak.FEIL_LOVANVENDELSE -> StatistikkResultatBegrunnelse.FEIL_LOVANVENDELSE
        KlageOmgjøringsårsak.FEIL_REGELVERKSFORSTAAELSE -> StatistikkResultatBegrunnelse.FEIL_REGELVERKSFORSTAAELSE
        KlageOmgjøringsårsak.FEIL_ELLER_ENDRET_FAKTA -> StatistikkResultatBegrunnelse.FEIL_ELLER_ENDRET_FAKTA
        KlageOmgjøringsårsak.PROSESSUELL_FEIL -> StatistikkResultatBegrunnelse.PROSESSUELL_FEIL
        KlageOmgjøringsårsak.ANNET -> StatistikkResultatBegrunnelse.ANNET
    }
}
