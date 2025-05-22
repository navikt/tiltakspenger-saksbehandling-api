package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag

private enum class ValgtHjemmelForAvslagDb {
    AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    AVSLAG_ALDER,
    AVSLAG_LIVSOPPHOLDSYTELSER,
    AVSLAG_KVALIFISERINGSPROGRAMMET,
    AVSLAG_INTRODUKSJONSPROGRAMMET,
    AVSLAG_LØNN_FRA_TILTAKSARRANGØR,
    AVSLAG_LØNN_FRA_ANDRE,
    AVSLAG_INSTITUSJONSOPPHOLD,
    AVSLAG_FREMMET_FOR_SENT,
}

fun Set<Avslagsgrunnlag>.toDb(): String = serialize(this.map { it.toDb() })

fun String.toAvslagsgrunnlag(): NonEmptySet<Avslagsgrunnlag> {
    return deserializeList<ValgtHjemmelForAvslagDb>(this).map {
        when (it) {
            ValgtHjemmelForAvslagDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak
            ValgtHjemmelForAvslagDb.AVSLAG_ALDER -> Avslagsgrunnlag.Alder
            ValgtHjemmelForAvslagDb.AVSLAG_LIVSOPPHOLDSYTELSER -> Avslagsgrunnlag.Livsoppholdytelser
            ValgtHjemmelForAvslagDb.AVSLAG_KVALIFISERINGSPROGRAMMET -> Avslagsgrunnlag.Kvalifiseringsprogrammet
            ValgtHjemmelForAvslagDb.AVSLAG_INTRODUKSJONSPROGRAMMET -> Avslagsgrunnlag.Introduksjonsprogrammet
            ValgtHjemmelForAvslagDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR -> Avslagsgrunnlag.LønnFraTiltaksarrangør
            ValgtHjemmelForAvslagDb.AVSLAG_LØNN_FRA_ANDRE -> Avslagsgrunnlag.LønnFraAndre
            ValgtHjemmelForAvslagDb.AVSLAG_INSTITUSJONSOPPHOLD -> Avslagsgrunnlag.Institusjonsopphold
            ValgtHjemmelForAvslagDb.AVSLAG_FREMMET_FOR_SENT -> Avslagsgrunnlag.FremmetForSent
        }
    }.toNonEmptySetOrNull()!!
}

private fun Avslagsgrunnlag.toDb(): String {
    return when (this) {
        is Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelForAvslagDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
        is Avslagsgrunnlag.Alder -> ValgtHjemmelForAvslagDb.AVSLAG_ALDER
        is Avslagsgrunnlag.Livsoppholdytelser -> ValgtHjemmelForAvslagDb.AVSLAG_LIVSOPPHOLDSYTELSER
        is Avslagsgrunnlag.Kvalifiseringsprogrammet -> ValgtHjemmelForAvslagDb.AVSLAG_KVALIFISERINGSPROGRAMMET
        is Avslagsgrunnlag.Introduksjonsprogrammet -> ValgtHjemmelForAvslagDb.AVSLAG_INTRODUKSJONSPROGRAMMET
        is Avslagsgrunnlag.LønnFraTiltaksarrangør -> ValgtHjemmelForAvslagDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR
        is Avslagsgrunnlag.LønnFraAndre -> ValgtHjemmelForAvslagDb.AVSLAG_LØNN_FRA_ANDRE
        is Avslagsgrunnlag.Institusjonsopphold -> ValgtHjemmelForAvslagDb.AVSLAG_INSTITUSJONSOPPHOLD
        is Avslagsgrunnlag.FremmetForSent -> ValgtHjemmelForAvslagDb.AVSLAG_FREMMET_FOR_SENT
    }.toString()
}
