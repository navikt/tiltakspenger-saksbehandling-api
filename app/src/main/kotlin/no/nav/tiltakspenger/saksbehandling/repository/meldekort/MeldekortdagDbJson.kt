package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeOmberegnet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.FRAVÆR_VELFERD_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.IKKE_UTFYLT
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortdagDbJson.StatusDb.SPERRET
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.toDb
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.toTiltakstypeSomGirRett
import java.time.LocalDate

/**
 * @property reduksjon null dersom den ikke er utfylt
 */
private data class MeldekortdagDbJson(
    val tiltakstype: String?,
    val dato: String,
    val status: StatusDb,
    val reduksjon: ReduksjonAvYtelsePåGrunnAvFraværDb?,
    val beregningsdag: BeregningsdagDbJson?,
    val kjedeId: String?,
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

    fun toMeldekortdag(meldekortId: MeldekortId, kjedeId: MeldeperiodeKjedeId): MeldeperiodeBeregningDag {
        val parsedDato = LocalDate.parse(dato)
        val parsedTiltakstype = tiltakstype?.toTiltakstypeSomGirRett()
        val parsedBeregningsdag = beregningsdag?.toBeregningsdag()
        return when (status) {
            SPERRET -> Sperret(meldekortId, kjedeId, parsedDato)
            IKKE_UTFYLT -> IkkeUtfylt(meldekortId, kjedeId, parsedDato, parsedTiltakstype!!)
            DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            IKKE_DELTATT -> IkkeDeltatt.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            FRAVÆR_SYK -> SykBruker.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            FRAVÆR_SYKT_BARN -> SyktBarn.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            FRAVÆR_VELFERD_GODKJENT_AV_NAV -> VelferdGodkjentAvNav.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> VelferdIkkeGodkjentAvNav.fromDb(
                meldekortId,
                kjedeId,
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )
        }
    }
}

private data class MeldeperiodeOmberegnetDbJson(
    val kjedeId: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val dager: List<MeldekortdagDbJson>,
)

internal fun MeldeperiodeBeregning.tilMeldekortdagerDbJson(): String =
    when (this) {
        is MeldeperiodeBeregning.IkkeUtfyltMeldeperiode -> this.tilMeldekortdagerDbJson()
        is MeldeperiodeBeregning.UtfyltMeldeperiode -> this.tilMeldekortdagerDbJson()
    }

internal fun MeldeperiodeBeregning.tilMeldeperioderOmberegnetDbJson(): String? =
    when (this) {
        is MeldeperiodeBeregning.IkkeUtfyltMeldeperiode -> null
        is MeldeperiodeBeregning.UtfyltMeldeperiode ->
            if (this.meldeperioderOmberegnet.isNotEmpty()) this.meldeperioderOmberegnet.toDbJson() else null
    }

private fun MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.tilMeldekortdagerDbJson(): String =
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
            kjedeId = meldekortdag.kjedeId.toString(),
        )
    }.let { serialize(it) }

private fun MeldeperiodeBeregning.UtfyltMeldeperiode.tilMeldekortdagerDbJson(): String =
    dager.toList().toDbJson().let { serialize(it) }

private fun List<MeldeperiodeOmberegnet>.toDbJson(): String = this.map { meldeperiode ->
    MeldeperiodeOmberegnetDbJson(
        kjedeId = meldeperiode.kjedeId.toString(),
        fraOgMed = meldeperiode.periode.fraOgMed,
        tilOgMed = meldeperiode.periode.tilOgMed,
        dager = meldeperiode.dager.toDbJson(),
    )
}.let { serialize(it) }

private fun List<MeldeperiodeBeregningDag.Utfylt>.toDbJson() = this.map { meldekortdag ->
    MeldekortdagDbJson(
        tiltakstype = meldekortdag.tiltakstype?.toDb(),
        dato = meldekortdag.dato.toString(),
        reduksjon = meldekortdag.reduksjon.toDb(),
        beregningsdag = meldekortdag.beregningsdag?.toDbJson(),
        kjedeId = meldekortdag.kjedeId.toString(),
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

internal fun String.tilUtfylteMeldekortDager(
    meldekortId: MeldekortId,
    kjedeId: MeldeperiodeKjedeId,
): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> =
    deserializeList<MeldekortdagDbJson>(this).map {
        it.toMeldekortdag(
            meldekortId,
            kjedeId,
        ) as MeldeperiodeBeregningDag.Utfylt
    }
        .toNonEmptyListOrNull()!!

internal fun String.tilIkkeUtfylteMeldekortDager(
    meldekortId: MeldekortId,
    kjedeId: MeldeperiodeKjedeId,
): NonEmptyList<MeldeperiodeBeregningDag> =
    deserializeList<MeldekortdagDbJson>(this).map { it.toMeldekortdag(meldekortId, kjedeId) }
        .toNonEmptyListOrNull()!!

internal fun String.tilMeldeperioderOmberegnet(meldekortId: MeldekortId): List<MeldeperiodeOmberegnet> =
    deserializeList<MeldeperiodeOmberegnetDbJson>(this).map {
        val kjedeId = MeldeperiodeKjedeId(it.kjedeId)
        MeldeperiodeOmberegnet(
            kjedeId = kjedeId,
            periode = Periode(it.fraOgMed, it.tilOgMed),
            dager = it.dager.map { dag ->
                dag.toMeldekortdag(meldekortId, kjedeId) as MeldeperiodeBeregningDag.Utfylt
            }.toNonEmptyListOrNull()!!,
        )
    }
