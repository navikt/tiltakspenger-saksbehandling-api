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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeBeregningDagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toTiltakstypeSomGirRett
import java.time.LocalDate

/**
 * @property reduksjon null dersom den ikke er utfylt
 */
private data class MeldeperiodeBeregningDagDbJson(
    val tiltakstype: String?,
    val dato: String,
    val status: MeldekortstatusDb,
    val reduksjon: ReduksjonAvYtelsePåGrunnAvFraværDb?,
    val beregningsdag: BeregningsdagDbJson?,
) {

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
            MeldekortstatusDb.DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.FRAVÆR_SYK -> SykBruker.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.FRAVÆR_SYKT_BARN -> SyktBarn.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                reduksjon!!.toDomain(),
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.FRAVÆR_GODKJENT_AV_NAV -> FraværGodkjentAvNav.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.FRAVÆR_ANNET -> FraværAnnet.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.IKKE_BESVART -> MeldeperiodeBeregningDag.IkkeBesvart.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.IKKE_TILTAKSDAG -> IkkeDeltatt.fromDb(
                parsedDato,
                parsedTiltakstype!!,
                parsedBeregningsdag!!,
            )

            MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> IkkeRettTilTiltakspenger(parsedDato)
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
            is DeltattUtenLønnITiltaket -> MeldekortstatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
            is DeltattMedLønnITiltaket -> MeldekortstatusDb.DELTATT_MED_LØNN_I_TILTAKET
            is SykBruker -> MeldekortstatusDb.FRAVÆR_SYK
            is SyktBarn -> MeldekortstatusDb.FRAVÆR_SYKT_BARN
            is FraværGodkjentAvNav -> MeldekortstatusDb.FRAVÆR_GODKJENT_AV_NAV
            is FraværAnnet -> MeldekortstatusDb.FRAVÆR_ANNET
            is MeldeperiodeBeregningDag.IkkeBesvart -> MeldekortstatusDb.IKKE_BESVART
            is IkkeDeltatt -> MeldekortstatusDb.IKKE_TILTAKSDAG
            is IkkeRettTilTiltakspenger -> MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER
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
