package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør

// TODO abn: enten fjerne tekstene om barnetillegg, eller sette flagget basert på vedtaket som stanses eller opphøres
// Tekstene brukes ikke til noe nå
fun HjemmelForStansEllerOpphør.tekstVedtaksbrev(barnetillegg: Boolean = false): String {
    return when (this) {
        HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak ->
            if (barnetillegg) {
                "du ikke lenger deltar på arbeidsmarkedstiltak. Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til tiltakspenger og barnetillegg. Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften §§ 2 og 3."
            } else {
                "du ikke lenger deltar på arbeidsmarkedstiltak. Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til å få tiltakspenger. Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 2."
            }

        HjemmelForStansEllerOpphør.Alder ->
            "du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få tiltakspenger. Det kommer frem av tiltakspengeforskriften § 3."

        HjemmelForStansEllerOpphør.Livsoppholdytelser ->
            if (barnetillegg) {
                "du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få tiltakspenger og barnetillegg. Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og forskrift om tiltakspenger §§ 3 og 7."
            } else {
                "du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold, har ikke samtidig rett til å få tiltakspenger. Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og tiltakspengeforskriften § 7."
            }

        HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet ->
            if (barnetillegg) {
                "du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger og barnetillegg. Dette kommer frem av tiltakspengeforskriften §§ 3 og 7 tredje ledd."
            } else {
                "du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger. Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
            }

        HjemmelForStansEllerOpphør.Introduksjonsprogrammet ->
            if (barnetillegg) {
                "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger og barnetillegg. Dette kommer frem av tiltakspengeforskriften §§ 3 og 7 tredje ledd."
            } else {
                "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger. Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
            }

        HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør ->
            if (barnetillegg) {
                "du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket. Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til tiltakspenger og barnetillegg. Dette kommer frem av tiltakspengeforskriften §§ 3 og 8. "
            } else {
                "du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket. Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til tiltakspenger. Dette kommer frem av tiltakspengeforskriften § 8."
            }

        HjemmelForStansEllerOpphør.LønnFraAndre ->
            if (barnetillegg) {
                """
                    du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til tiltakspenger og barnetillegg. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften §$ 3 og 8 andre ledd.
                """
            } else {
                """
                    du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til tiltakspenger. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 8 andre ledd.
                """
            }

        HjemmelForStansEllerOpphør.Institusjonsopphold ->
            if (barnetillegg) {
                """
                    du oppholder deg på en institusjon med gratis opphold, mat og drikke. 
                    Deltakere som har opphold i institusjon, med gratis opphold, mat og drikke. under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til tiltakspenger og barnetillegg.
                    Det er gjort unntak for opphold i barneverns-institusjoner. Dette kommer frem av tiltakspengeforskriften §§ 3 og 9. 
                """
            } else {
                """
                    du oppholder deg på en institusjon med gratis opphold, mat og drikke. 
                    Deltakere som har opphold i institusjon, med gratis opphold, mat og drikke. under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til tiltakspenger.
                    Det er gjort unntak for opphold i barneverns-institusjoner. Dette kommer frem av tiltakspengeforskriften § 9. 
                """
            }
    }.trimIndent()
}
