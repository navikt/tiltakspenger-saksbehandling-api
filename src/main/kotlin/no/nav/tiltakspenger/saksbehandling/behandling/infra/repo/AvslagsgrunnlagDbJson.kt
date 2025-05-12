package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag

internal fun Set<Avslagsgrunnlag>.toDb(): String = serialize(this.map { it.toDb() })

internal fun String.toAvslagsgrunnlag(): NonEmptySet<Avslagsgrunnlag> {
    return deserializeList<ValgtHjemmelHarIkkeRettighetDb>(this).map {
        when (it) {
            ValgtHjemmelHarIkkeRettighetDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
            ValgtHjemmelHarIkkeRettighetDb.STANS_ALDER,
            ValgtHjemmelHarIkkeRettighetDb.STANS_LIVSOPPHOLDSYTELSER,
            ValgtHjemmelHarIkkeRettighetDb.STANS_KVALIFISERINGSPROGRAMMET,
            ValgtHjemmelHarIkkeRettighetDb.STANS_INTRODUKSJONSPROGRAMMET,
            ValgtHjemmelHarIkkeRettighetDb.STANS_LØNN_FRA_TILTAKSARRANGØR,
            ValgtHjemmelHarIkkeRettighetDb.STANS_LØNN_FRA_ANDRE,
            ValgtHjemmelHarIkkeRettighetDb.STANS_INSTITUSJONSOPPHOLD,
            -> throw IllegalStateException("Kan ikke bruke stans-hjemler i avslagsgrunnlag")

            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_ALDER -> Avslagsgrunnlag.Alder
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LIVSOPPHOLDSYTELSER -> Avslagsgrunnlag.Livsoppholdytelser
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_KVALIFISERINGSPROGRAMMET -> Avslagsgrunnlag.Kvalifiseringsprogrammet
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INTRODUKSJONSPROGRAMMET -> Avslagsgrunnlag.Introduksjonsprogrammet
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_TILTAKSARRANGØR -> Avslagsgrunnlag.LønnFraTiltaksarrangør
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_LØNN_FRA_ANDRE -> Avslagsgrunnlag.LønnFraAndre
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_INSTITUSJONSOPPHOLD -> Avslagsgrunnlag.Institusjonsopphold
            ValgtHjemmelHarIkkeRettighetDb.AVSLAG_FREMMET_FOR_SENT -> Avslagsgrunnlag.FremmetForSent
        }
    }.toNonEmptySetOrNull()!!
}
