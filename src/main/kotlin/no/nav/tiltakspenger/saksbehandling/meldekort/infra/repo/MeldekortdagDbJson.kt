package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeUtfylt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.FRAVÆR_VELFERD_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.IKKE_UTFYLT
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortdagDbJson.StatusDb.SPERRET
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toTiltakstypeSomGirRett
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @property reduksjon null dersom den ikke er utfylt
 */
private data class MeldekortdagDbJson(
    val tiltakstype: String?,
    val dato: String,
    val status: StatusDb,
    val reduksjon: ReduksjonAvYtelsePåGrunnAvFraværDb?,
    val beregningsdag: BeregningsdagDbJson?,
) {
    enum class StatusDb {
        SPERRET,
        IKKE_UTFYLT,
        DELTATT_UTEN_LØNN_I_TILTAKET,
        DELTATT_MED_LØNN_I_TILTAKET,
        IKKE_DELTATT,
        FRAVÆR_SYK,
        FRAVÆR_SYKT_BARN,
        FRAVÆR_VELFERD_GODKJENT_AV_NAV,
        FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
    }

    enum class ReduksjonAvYtelsePåGrunnAvFraværDb {
        IngenReduksjon,
        DelvisReduksjon,
        YtelsenFallerBort,
        ;

        fun toDomain(): ReduksjonAvYtelsePåGrunnAvFravær =
            when (this) {
                IngenReduksjon -> ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
                DelvisReduksjon -> ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
                YtelsenFallerBort -> ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
            }
    }

    fun toMeldekortdag(meldekortId: MeldekortId): MeldeperiodeBeregningDag {
        val parsedDato = LocalDate.parse(dato)
        val parsedTiltakstype = tiltakstype?.toTiltakstypeSomGirRett()
        val parsedBeregningsdag = beregningsdag?.toBeregningsdag()
        return when (status) {
            SPERRET -> Sperret(meldekortId, parsedDato)
            IKKE_UTFYLT -> IkkeUtfylt(meldekortId, parsedDato, parsedTiltakstype!!)
            DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            IKKE_DELTATT -> IkkeDeltatt.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            FRAVÆR_SYK -> SykBruker.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            FRAVÆR_SYKT_BARN -> SyktBarn.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            FRAVÆR_VELFERD_GODKJENT_AV_NAV -> VelferdGodkjentAvNav.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> VelferdIkkeGodkjentAvNav.fromDb(
                meldekortId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )
        }
    }
}

private data class MeldeperiodeBeregnetDbJson(
    val kjedeId: String,
    val meldekortId: String,
    val dager: List<MeldekortdagDbJson>,
)

internal fun MeldekortBeregning.tilMeldekortdagerDbJson(): String =
    when (this) {
        is MeldekortBeregning.IkkeUtfyltMeldeperiode -> this.tilMeldekortdagerDbJson()
        is MeldekortBeregning.UtfyltMeldeperiode -> this.tilMeldekortdagerDbJson()
    }

internal fun MeldekortBeregning.tilBeregningerDbJson(): String? =
    when (this) {
        is MeldekortBeregning.IkkeUtfyltMeldeperiode -> null
        is MeldekortBeregning.UtfyltMeldeperiode -> this.beregninger.toDbJson()
    }

private fun MeldekortBeregning.IkkeUtfyltMeldeperiode.tilMeldekortdagerDbJson(): String =
    dager.toList().map { meldekortdag ->
        MeldekortdagDbJson(
            tiltakstype = meldekortdag.tiltakstype?.toDb(),
            dato = meldekortdag.dato.toString(),
            status = when (meldekortdag) {
                is Sperret -> SPERRET
                is IkkeUtfylt -> IKKE_UTFYLT
                is DeltattMedLønnITiltaket -> DELTATT_MED_LØNN_I_TILTAKET
                is DeltattUtenLønnITiltaket -> DELTATT_UTEN_LØNN_I_TILTAKET
                is SykBruker -> FRAVÆR_SYK
                is SyktBarn -> FRAVÆR_SYKT_BARN
                is VelferdGodkjentAvNav -> FRAVÆR_VELFERD_GODKJENT_AV_NAV
                is VelferdIkkeGodkjentAvNav -> FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                is IkkeDeltatt -> IKKE_DELTATT
            },
            reduksjon = null,
            beregningsdag = null,
        )
    }.let { serialize(it) }

private fun MeldekortBeregning.UtfyltMeldeperiode.tilMeldekortdagerDbJson(): String =
    dager.toList().toDbJson().let { serialize(it) }

private fun List<MeldekortBeregning.MeldeperiodeBeregnet>.toDbJson(): String = this.map { meldeperiode ->
    MeldeperiodeBeregnetDbJson(
        kjedeId = meldeperiode.kjedeId.toString(),
        meldekortId = meldeperiode.meldekortId.toString(),
        dager = meldeperiode.dager.toDbJson(),
    )
}.let { serialize(it) }

private fun List<MeldeperiodeBeregningDag.Utfylt>.toDbJson() = this.map { meldekortdag ->
    MeldekortdagDbJson(
        tiltakstype = meldekortdag.tiltakstype?.toDb(),
        dato = meldekortdag.dato.toString(),
        reduksjon = meldekortdag.reduksjon.toDb(),
        beregningsdag = meldekortdag.beregningsdag?.toDbJson(),
        status =
        when (meldekortdag) {
            is DeltattMedLønnITiltaket -> DELTATT_MED_LØNN_I_TILTAKET
            is DeltattUtenLønnITiltaket -> DELTATT_UTEN_LØNN_I_TILTAKET
            is SykBruker -> FRAVÆR_SYK
            is SyktBarn -> FRAVÆR_SYKT_BARN
            is VelferdGodkjentAvNav -> FRAVÆR_VELFERD_GODKJENT_AV_NAV
            is VelferdIkkeGodkjentAvNav -> FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
            is IkkeDeltatt -> IKKE_DELTATT
            is Sperret -> SPERRET
        },
    )
}

private fun ReduksjonAvYtelsePåGrunnAvFravær.toDb(): ReduksjonAvYtelsePåGrunnAvFraværDb =
    when (this) {
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon -> ReduksjonAvYtelsePåGrunnAvFraværDb.IngenReduksjon
        ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon -> ReduksjonAvYtelsePåGrunnAvFraværDb.DelvisReduksjon
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> ReduksjonAvYtelsePåGrunnAvFraværDb.YtelsenFallerBort
    }

internal fun String.tilIkkeUtfylteMeldekortDager(
    meldekortId: MeldekortId,
): NonEmptyList<MeldeperiodeBeregningDag> =
    deserializeList<MeldekortdagDbJson>(this)
        .map { it.toMeldekortdag(meldekortId) }
        .toNonEmptyListOrNull()!!

private fun MeldeperiodeBeregnetDbJson.tilMeldeperiodeBeregnet(opprettet: LocalDateTime): MeldekortBeregning.MeldeperiodeBeregnet {
    val meldekortId = MeldekortId.fromString(this.meldekortId)

    return MeldekortBeregning.MeldeperiodeBeregnet(
        kjedeId = MeldeperiodeKjedeId(this.kjedeId),
        meldekortId = meldekortId,
        dager = this.dager.map { dag ->
            dag.toMeldekortdag(meldekortId) as MeldeperiodeBeregningDag.Utfylt
        }.toNonEmptyListOrNull()!!,
        opprettet = opprettet,
    )
}

internal fun String.tilBeregninger(opprettet: LocalDateTime): NonEmptyList<MeldekortBeregning.MeldeperiodeBeregnet> =
    deserializeList<MeldeperiodeBeregnetDbJson>(this).map {
        it.tilMeldeperiodeBeregnet(opprettet)
    }.toNonEmptyListOrNull()!!

internal fun String.tilBeregning(opprettet: LocalDateTime): MeldekortBeregning.MeldeperiodeBeregnet =
    deserialize<MeldeperiodeBeregnetDbJson>(this).tilMeldeperiodeBeregnet(opprettet)
