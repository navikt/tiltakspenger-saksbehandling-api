package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Sperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.FRAVÆR_VELFERD_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.StatusDb.SPERRET
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toTiltakstypeSomGirRett
import java.time.LocalDate

/**
 * @property reduksjon null dersom den ikke er utfylt
 */
private data class MeldeperiodeBeregningDagDbJson(
    val tiltakstype: String?,
    val dato: String,
    val status: StatusDb,
    val reduksjon: ReduksjonAvYtelsePåGrunnAvFraværDb?,
    val beregningsdag: BeregningsdagDbJson?,
) {
    enum class StatusDb {
        SPERRET,
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

    fun tilMeldeperiodeBeregningDag(): MeldeperiodeBeregningDag {
        val parsedDato = LocalDate.parse(dato)
        val parsedTiltakstype = tiltakstype?.toTiltakstypeSomGirRett()
        val parsedBeregningsdag = beregningsdag?.toBeregningsdag()
        return when (status) {
            SPERRET -> Sperret(parsedDato)
            DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            IKKE_DELTATT -> IkkeDeltatt.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            FRAVÆR_SYK -> SykBruker.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            FRAVÆR_SYKT_BARN -> SyktBarn.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            FRAVÆR_VELFERD_GODKJENT_AV_NAV -> VelferdGodkjentAvNav.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> VelferdIkkeGodkjentAvNav.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )
        }
    }
}

private data class MeldeperiodeBeregningDbJson(
    val kjedeId: String,
    val meldekortId: String,
    val dager: List<MeldeperiodeBeregningDagDbJson>,
)

fun MeldekortBeregning.tilBeregningerDbJson(): String {
    return this.map {
        MeldeperiodeBeregningDbJson(
            kjedeId = it.kjedeId.toString(),
            meldekortId = it.dagerMeldekortId.toString(),
            dager = it.dager.toDbJson(),
        )
    }.let { serialize(it) }
}

private fun List<MeldeperiodeBeregningDag>.toDbJson() = this.map { meldekortdag ->
    MeldeperiodeBeregningDagDbJson(
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

private fun MeldeperiodeBeregningDbJson.tilMeldeperiodeBeregning(meldekortId: MeldekortId): MeldeperiodeBeregning {
    return MeldeperiodeBeregning(
        kjedeId = MeldeperiodeKjedeId(this.kjedeId),
        beregningMeldekortId = meldekortId,
        dagerMeldekortId = MeldekortId.fromString(this.meldekortId),
        dager = this.dager.map { it.tilMeldeperiodeBeregningDag() }.toNonEmptyListOrNull()!!,
    )
}

fun String.tilBeregninger(meldekortId: MeldekortId): NonEmptyList<MeldeperiodeBeregning> =
    deserializeList<MeldeperiodeBeregningDbJson>(this).map {
        it.tilMeldeperiodeBeregning(meldekortId)
    }.toNonEmptyListOrNull()!!
