package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans

/**
 * Grunnen til avslag, stans eller opphør slik brevene i pdfgenrs kjenner den.
 * Kodene er kontrakten mot `lib/hjemmeltekster.typ` i tiltakspenger-pdfgenrs, og navnene må ikke endres uten at malen endres samtidig.
 */
enum class GrunnBrevDto {
    DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    ALDER,
    LIVSOPPHOLDYTELSE,
    KVALIFISERINGSPROGRAMMET,
    INTRODUKSJONSPROGRAMMET,
    LØNN_FRA_TILTAKSARRANGØR,
    LØNN_FRA_ANDRE,
    INSTITUSJONSOPPHOLD,
    IKKE_LOVLIG_OPPHOLD,
    FREMMET_FOR_SENT,
    RETT_TIL_DAGPENGER,
}

fun Avslagsgrunnlag.tilGrunnBrevDto(): GrunnBrevDto = when (this) {
    Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> GrunnBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
    Avslagsgrunnlag.Alder -> GrunnBrevDto.ALDER
    Avslagsgrunnlag.Livsoppholdytelser -> GrunnBrevDto.LIVSOPPHOLDYTELSE
    Avslagsgrunnlag.Kvalifiseringsprogrammet -> GrunnBrevDto.KVALIFISERINGSPROGRAMMET
    Avslagsgrunnlag.Introduksjonsprogrammet -> GrunnBrevDto.INTRODUKSJONSPROGRAMMET
    Avslagsgrunnlag.LønnFraTiltaksarrangør -> GrunnBrevDto.LØNN_FRA_TILTAKSARRANGØR
    Avslagsgrunnlag.LønnFraAndre -> GrunnBrevDto.LØNN_FRA_ANDRE
    Avslagsgrunnlag.Institusjonsopphold -> GrunnBrevDto.INSTITUSJONSOPPHOLD
    Avslagsgrunnlag.FremmetForSent -> GrunnBrevDto.FREMMET_FOR_SENT
    Avslagsgrunnlag.RettTilDagpenger -> GrunnBrevDto.RETT_TIL_DAGPENGER
}

fun HjemmelForStans.tilGrunnBrevDto(): GrunnBrevDto = when (this) {
    HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> GrunnBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
    HjemmelForStans.Alder -> GrunnBrevDto.ALDER
    HjemmelForStans.Livsoppholdytelser -> GrunnBrevDto.LIVSOPPHOLDYTELSE
    HjemmelForStans.Kvalifiseringsprogrammet -> GrunnBrevDto.KVALIFISERINGSPROGRAMMET
    HjemmelForStans.Introduksjonsprogrammet -> GrunnBrevDto.INTRODUKSJONSPROGRAMMET
    HjemmelForStans.LønnFraTiltaksarrangør -> GrunnBrevDto.LØNN_FRA_TILTAKSARRANGØR
    HjemmelForStans.LønnFraAndre -> GrunnBrevDto.LØNN_FRA_ANDRE
    HjemmelForStans.Institusjonsopphold -> GrunnBrevDto.INSTITUSJONSOPPHOLD
    HjemmelForStans.IkkeLovligOpphold -> GrunnBrevDto.IKKE_LOVLIG_OPPHOLD
}

fun HjemmelForOpphør.tilGrunnBrevDto(): GrunnBrevDto = when (this) {
    HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak -> GrunnBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
    HjemmelForOpphør.Alder -> GrunnBrevDto.ALDER
    HjemmelForOpphør.Livsoppholdytelser -> GrunnBrevDto.LIVSOPPHOLDYTELSE
    HjemmelForOpphør.Kvalifiseringsprogrammet -> GrunnBrevDto.KVALIFISERINGSPROGRAMMET
    HjemmelForOpphør.Introduksjonsprogrammet -> GrunnBrevDto.INTRODUKSJONSPROGRAMMET
    HjemmelForOpphør.LønnFraTiltaksarrangør -> GrunnBrevDto.LØNN_FRA_TILTAKSARRANGØR
    HjemmelForOpphør.LønnFraAndre -> GrunnBrevDto.LØNN_FRA_ANDRE
    HjemmelForOpphør.Institusjonsopphold -> GrunnBrevDto.INSTITUSJONSOPPHOLD
    HjemmelForOpphør.IkkeLovligOpphold -> GrunnBrevDto.IKKE_LOVLIG_OPPHOLD
    HjemmelForOpphør.FremmetForSent -> GrunnBrevDto.FREMMET_FOR_SENT
}
