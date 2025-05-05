package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet

enum class ValgtHjemmelHarIkkeRettighetDb {
    STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    STANS_ALDER,
    STANS_LIVSOPPHOLDSYTELSER,
    STANS_KVALIFISERINGSPROGRAMMET,
    STANS_INTRODUKSJONSPROGRAMMET,
    STANS_LØNN_FRA_TILTAKSARRANGØR,
    STANS_LØNN_FRA_ANDRE,
    STANS_INSTITUSJONSOPPHOLD,
    AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    AVSLAG_ALDER,
    AVSLAG_LIVSOPPHOLDSYTELSER,
    AVSLAG_KVALIFISERINGSPROGRAMMET,
    AVSLAG_INTRODUKSJONSPROGRAMMET,
    AVSLAG_LØNN_FRA_TILTAKSARRANGØR,
    AVSLAG_LØNN_FRA_ANDRE,
    AVSLAG_INSTITUSJONSOPPHOLD,
    FREMMET_FOR_SENT,
}

fun List<ValgtHjemmelHarIkkeRettighet>.toDbJson(): String {
    return serialize(this.map { it.toDb() })
}

fun String.toValgtHjemmelHarIkkeRettighet(): List<ValgtHjemmelHarIkkeRettighet> {
    return deserializeList<ValgtHjemmelHarIkkeRettighetDb>(this).map { it.toDomain() }
}

internal fun ValgtHjemmelHarIkkeRettighet.toDb(): ValgtHjemmelHarIkkeRettighetDb {
    return when (this) {
        is ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelHarIkkeRettighetDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
        is ValgtHjemmelForStans.Alder -> ValgtHjemmelHarIkkeRettighetDb.STANS_ALDER
        is ValgtHjemmelForStans.Livsoppholdytelser -> ValgtHjemmelHarIkkeRettighetDb.STANS_LIVSOPPHOLDSYTELSER
        is ValgtHjemmelForStans.Kvalifiseringsprogrammet -> ValgtHjemmelHarIkkeRettighetDb.STANS_KVALIFISERINGSPROGRAMMET
        is ValgtHjemmelForStans.Introduksjonsprogrammet -> ValgtHjemmelHarIkkeRettighetDb.STANS_INTRODUKSJONSPROGRAMMET
        is ValgtHjemmelForStans.LønnFraTiltaksarrangør -> ValgtHjemmelHarIkkeRettighetDb.STANS_LØNN_FRA_TILTAKSARRANGØR
        is ValgtHjemmelForStans.LønnFraAndre -> ValgtHjemmelHarIkkeRettighetDb.STANS_LØNN_FRA_ANDRE
        is ValgtHjemmelForStans.Institusjonsopphold -> ValgtHjemmelHarIkkeRettighetDb.STANS_INSTITUSJONSOPPHOLD
        is Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
        is Avslagsgrunnlag.Alder -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_ALDER
        is Avslagsgrunnlag.Livsoppholdytelser -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LIVSOPPHOLDSYTELSER
        is Avslagsgrunnlag.Kvalifiseringsprogrammet -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_KVALIFISERINGSPROGRAMMET
        is Avslagsgrunnlag.Introduksjonsprogrammet -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INTRODUKSJONSPROGRAMMET
        is Avslagsgrunnlag.LønnFraTiltaksarrangør -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR
        is Avslagsgrunnlag.LønnFraAndre -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_ANDRE
        is Avslagsgrunnlag.Institusjonsopphold -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INSTITUSJONSOPPHOLD
        is Avslagsgrunnlag.FremmetForSent -> ValgtHjemmelHarIkkeRettighetDb.FREMMET_FOR_SENT
    }
}

internal fun ValgtHjemmelHarIkkeRettighetDb.toDomain(): ValgtHjemmelHarIkkeRettighet {
    return when (this) {
        ValgtHjemmelHarIkkeRettighetDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
        ValgtHjemmelHarIkkeRettighetDb.STANS_ALDER -> ValgtHjemmelForStans.Alder
        ValgtHjemmelHarIkkeRettighetDb.STANS_LIVSOPPHOLDSYTELSER -> ValgtHjemmelForStans.Livsoppholdytelser
        ValgtHjemmelHarIkkeRettighetDb.STANS_KVALIFISERINGSPROGRAMMET -> ValgtHjemmelForStans.Kvalifiseringsprogrammet
        ValgtHjemmelHarIkkeRettighetDb.STANS_INTRODUKSJONSPROGRAMMET -> ValgtHjemmelForStans.Introduksjonsprogrammet
        ValgtHjemmelHarIkkeRettighetDb.STANS_LØNN_FRA_TILTAKSARRANGØR -> ValgtHjemmelForStans.LønnFraTiltaksarrangør
        ValgtHjemmelHarIkkeRettighetDb.STANS_LØNN_FRA_ANDRE -> ValgtHjemmelForStans.LønnFraAndre
        ValgtHjemmelHarIkkeRettighetDb.STANS_INSTITUSJONSOPPHOLD -> ValgtHjemmelForStans.Institusjonsopphold
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_ALDER -> Avslagsgrunnlag.Alder
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LIVSOPPHOLDSYTELSER -> Avslagsgrunnlag.Livsoppholdytelser
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_KVALIFISERINGSPROGRAMMET -> Avslagsgrunnlag.Kvalifiseringsprogrammet
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INTRODUKSJONSPROGRAMMET -> Avslagsgrunnlag.Introduksjonsprogrammet
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR -> Avslagsgrunnlag.LønnFraTiltaksarrangør
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_ANDRE -> Avslagsgrunnlag.LønnFraAndre
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INSTITUSJONSOPPHOLD -> Avslagsgrunnlag.Institusjonsopphold
        ValgtHjemmelHarIkkeRettighetDb.FREMMET_FOR_SENT -> Avslagsgrunnlag.FremmetForSent
    }
}
