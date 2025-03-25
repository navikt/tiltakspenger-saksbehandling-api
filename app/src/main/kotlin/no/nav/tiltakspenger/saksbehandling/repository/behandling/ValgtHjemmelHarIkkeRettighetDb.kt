package no.nav.tiltakspenger.saksbehandling.repository.behandling

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForAvslag
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelHarIkkeRettighet

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
        is ValgtHjemmelForAvslag.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
        is ValgtHjemmelForAvslag.Alder -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_ALDER
        is ValgtHjemmelForAvslag.Livsoppholdytelser -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LIVSOPPHOLDSYTELSER
        is ValgtHjemmelForAvslag.Kvalifiseringsprogrammet -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_KVALIFISERINGSPROGRAMMET
        is ValgtHjemmelForAvslag.Introduksjonsprogrammet -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INTRODUKSJONSPROGRAMMET
        is ValgtHjemmelForAvslag.LønnFraTiltaksarrangør -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR
        is ValgtHjemmelForAvslag.LønnFraAndre -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_ANDRE
        is ValgtHjemmelForAvslag.Institusjonsopphold -> ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INSTITUSJONSOPPHOLD
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
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> ValgtHjemmelForAvslag.DeltarIkkePåArbeidsmarkedstiltak
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_ALDER -> ValgtHjemmelForAvslag.Alder
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LIVSOPPHOLDSYTELSER -> ValgtHjemmelForAvslag.Livsoppholdytelser
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_KVALIFISERINGSPROGRAMMET -> ValgtHjemmelForAvslag.Kvalifiseringsprogrammet
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INTRODUKSJONSPROGRAMMET -> ValgtHjemmelForAvslag.Introduksjonsprogrammet
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR -> ValgtHjemmelForAvslag.LønnFraTiltaksarrangør
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_ANDRE -> ValgtHjemmelForAvslag.LønnFraAndre
        ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INSTITUSJONSOPPHOLD -> ValgtHjemmelForAvslag.Institusjonsopphold
    }
}
