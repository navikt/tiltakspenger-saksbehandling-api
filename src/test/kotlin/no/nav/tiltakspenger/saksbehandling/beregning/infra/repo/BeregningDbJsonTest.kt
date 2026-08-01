package no.nav.tiltakspenger.saksbehandling.beregning.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningId
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværSterkeVelferdsgrunnerEllerJobbintervju
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.beregning.ReduksjonAvYtelsePåGrunnAvFravær
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * De ti dagstatusene ville krevd ti utfylte meldekort gjennom prodstiene, og flere av dem kan bare oppstå i kombinasjoner vi ikke styrer fra en test.
 * Mappingen rører ikke postgres — den er ren json.
 *
 * Testen pinner **de faktiske verdiene som havner i jsonb-kolonnen**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en status ble omdøpt i begge `when`-ene samtidig, og da er lagrede beregninger ulesbare uten at noe slår ut.
 *
 * Merk at testen *ikke* sier at alle statusene kan nås.
 * En status ingen prodsti produserer er død kode og skal slettes, ikke dekkes.
 */
class BeregningDbJsonTest {

    private val tiltakstype = TiltakstypeSomGirRettDTO.GRUPPE_AMO
    private val meldekortId = MeldekortId.random()
    private val kjedeId = MeldeperiodeKjedeId("2025-01-06/2025-01-19")

    /**
     * Én dag av hver status, i samme rekkefølge som [MeldekortDagStatusDbForventet].
     * En meldeperiode må være nøyaktig 14 dager, så de fire siste er fyll.
     */
    private fun beregningMedAlleDagstatuser(
        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær = ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon,
    ): Beregning {
        val dager = nonEmptyListOf(
            DeltattUtenLønnITiltaket.create(6.januar(2025), tiltakstype, AntallBarn.ZERO),
            DeltattMedLønnITiltaket.create(7.januar(2025), tiltakstype, AntallBarn.ZERO),
            SykBruker.create(8.januar(2025), reduksjon, tiltakstype, AntallBarn.ZERO),
            SyktBarn.create(9.januar(2025), reduksjon, tiltakstype, AntallBarn.ZERO),
            FraværGodkjentAvNav.create(10.januar(2025), tiltakstype, AntallBarn.ZERO),
            FraværSterkeVelferdsgrunnerEllerJobbintervju.create(11.januar(2025), tiltakstype, AntallBarn.ZERO),
            FraværAnnet.create(12.januar(2025), tiltakstype, AntallBarn.ZERO),
            MeldeperiodeBeregningDag.IkkeBesvart.create(13.januar(2025), tiltakstype, AntallBarn.ZERO),
            IkkeDeltatt.create(14.januar(2025), tiltakstype, AntallBarn.ZERO),
            IkkeRettTilTiltakspenger(15.januar(2025)),
            IkkeDeltatt.create(16.januar(2025), tiltakstype, AntallBarn.ZERO),
            IkkeDeltatt.create(17.januar(2025), tiltakstype, AntallBarn.ZERO),
            IkkeDeltatt.create(18.januar(2025), tiltakstype, AntallBarn.ZERO),
            IkkeDeltatt.create(19.januar(2025), tiltakstype, AntallBarn.ZERO),
        )
        return Beregning(
            beregninger = nonEmptyListOf(
                MeldeperiodeBeregning(
                    id = BeregningId.random(),
                    meldekortId = meldekortId,
                    kjedeId = kjedeId,
                    dager = dager,
                    beregningKilde = BeregningKilde.BeregningKildeMeldekort(meldekortId),
                ),
            ),
            beregningstidspunkt = LocalDateTime.parse("2025-01-20T10:00:00"),
        )
    }

    @Test
    fun `dagstatusene lagres med sitt avtalte navn`() {
        beregningMedAlleDagstatuser().tilBeregningDbJson()
            .beregninger.single().dager.map { it.status.name } shouldBe MeldekortDagStatusDbForventet
    }

    @Test
    fun `dagstatusene leses tilbake som riktig domenetype`() {
        val lagret = beregningMedAlleDagstatuser().tilBeregningerDbJsonString()

        val dager = lagret.tilBeregningFraMeldekortbehandling(meldekortId).beregninger.single().dager

        dager[0].shouldBeInstanceOf<DeltattUtenLønnITiltaket>()
        dager[1].shouldBeInstanceOf<DeltattMedLønnITiltaket>()
        dager[2].shouldBeInstanceOf<SykBruker>()
        dager[3].shouldBeInstanceOf<SyktBarn>()
        dager[4].shouldBeInstanceOf<FraværGodkjentAvNav>()
        dager[5].shouldBeInstanceOf<FraværSterkeVelferdsgrunnerEllerJobbintervju>()
        dager[6].shouldBeInstanceOf<FraværAnnet>()
        dager[7].shouldBeInstanceOf<MeldeperiodeBeregningDag.IkkeBesvart>()
        dager[8].shouldBeInstanceOf<IkkeDeltatt>()
        dager[9].shouldBeInstanceOf<IkkeRettTilTiltakspenger>()
    }

    /**
     * Domenets `Reduksjon` lagres som `DelvisReduksjon`.
     * Navnene er ulike med vilje, og nettopp derfor må verdien pinnes — en rundtur alene ville ikke fanget en omdøping.
     */
    @Test
    fun `reduksjon lagres med sitt avtalte navn`() {
        ReduksjonAvYtelsePåGrunnAvFravær.entries.associateWith { reduksjon ->
            beregningMedAlleDagstatuser(reduksjon).tilBeregningDbJson()
                .beregninger.single().dager
                .first { it.status.name == "FRAVÆR_SYK" }
                .reduksjon!!.name
        } shouldBe mapOf(
            ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon to "IngenReduksjon",
            ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon to "DelvisReduksjon",
            ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort to "YtelsenFallerBort",
        )
    }

    @Test
    fun `reduksjon leses tilbake fra lagret verdi`() {
        MeldeperiodeBeregningDagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb.entries.associateWith { it.toDomain() } shouldBe mapOf(
            MeldeperiodeBeregningDagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb.IngenReduksjon to ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon,
            MeldeperiodeBeregningDagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb.DelvisReduksjon to ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon,
            MeldeperiodeBeregningDagDbJson.ReduksjonAvYtelsePåGrunnAvFraværDb.YtelsenFallerBort to ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort,
        )
    }

    private companion object {
        val MeldekortDagStatusDbForventet = listOf(
            "DELTATT_UTEN_LØNN_I_TILTAKET",
            "DELTATT_MED_LØNN_I_TILTAKET",
            "FRAVÆR_SYK",
            "FRAVÆR_SYKT_BARN",
            "FRAVÆR_GODKJENT_AV_NAV",
            "FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU",
            "FRAVÆR_ANNET",
            "IKKE_BESVART",
            "IKKE_TILTAKSDAG",
            "IKKE_RETT_TIL_TILTAKSPENGER",
            "IKKE_TILTAKSDAG",
            "IKKE_TILTAKSDAG",
            "IKKE_TILTAKSDAG",
            "IKKE_TILTAKSDAG",
        )
    }
}
