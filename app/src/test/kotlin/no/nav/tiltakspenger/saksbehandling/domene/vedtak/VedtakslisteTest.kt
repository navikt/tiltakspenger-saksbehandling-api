package no.nav.tiltakspenger.saksbehandling.domene.vedtak

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.objectmothers.RevurderingMother.revurderingStansIverksatt
import no.nav.tiltakspenger.saksbehandling.domene.stønadsdager.Stønadsdager
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import org.junit.jupiter.api.Test

internal class VedtakslisteTest {

    @Test
    fun stønadsdager() {
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val hele = Periode(1.januar(2024), 2.januar(2024))
                val jan1 = Periode(1.januar(2024), 1.januar(2024))
                val jan2 = Periode(2.januar(2024), 2.januar(2024))
                val sak = tac.revurderingStansIverksatt(
                    førstegangsbehandlingPeriode = hele,
                    stansFraOgMed = 2.januar(2024),
                )
                sak.vedtaksperiode shouldBe hele
                sak.stønadsdager()[0] shouldBe sak.krympStønadsdager(jan1).single()
                sak.stønadsdager()[1] shouldBe sak.krympStønadsdager(jan2).single()
                sak.stønadsdager() shouldBe sak.krympStønadsdager(hele)
                val førsteStønadsdagPeriode = Stønadsdager(
                    vurderingsperiode = jan1,
                    registerSaksopplysning = sak.førstegangsbehandling.stønadsdager.registerSaksopplysning.copy(
                        periode = jan1,
                    ),
                )
                val andreStønadsdagPeriode = Stønadsdager(
                    vurderingsperiode = jan2,
                    registerSaksopplysning = sak.revurderinger.first().stønadsdager.registerSaksopplysning.copy(
                        periode = jan2,
                    ),
                )
                sak.stønadsdager() shouldBe Periodisering(
                    PeriodeMedVerdi(førsteStønadsdagPeriode, jan1),
                    PeriodeMedVerdi(andreStønadsdagPeriode, jan2),
                )
                sak.krympStønadsdager(jan1) shouldBe Periodisering(
                    PeriodeMedVerdi(førsteStønadsdagPeriode, jan1),
                )
                sak.krympStønadsdager(jan2) shouldBe Periodisering(
                    PeriodeMedVerdi(andreStønadsdagPeriode, jan2),
                )
            }
        }
    }

    @Test
    fun utfallsperioder() {
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val hele = Periode(1.januar(2024), 2.januar(2024))
                val jan1 = Periode(1.januar(2024), 1.januar(2024))
                val jan2 = Periode(2.januar(2024), 2.januar(2024))
                val sak = tac.revurderingStansIverksatt(
                    førstegangsbehandlingPeriode = hele,
                    stansFraOgMed = 2.januar(2024),
                )
                sak.vedtaksperiode shouldBe hele
                sak.utfallsperioder()[0] shouldBe sak.krympUtfallsperioder(jan1).single()
                sak.utfallsperioder()[1] shouldBe sak.krympUtfallsperioder(jan2).single()
                sak.utfallsperioder() shouldBe sak.krympUtfallsperioder(hele)

                sak.utfallsperioder() shouldBe Periodisering(
                    PeriodeMedVerdi(AvklartUtfallForPeriode.OPPFYLT, jan1),
                    PeriodeMedVerdi(AvklartUtfallForPeriode.IKKE_OPPFYLT, jan2),
                )
                sak.krympUtfallsperioder(jan1) shouldBe Periodisering(
                    PeriodeMedVerdi(AvklartUtfallForPeriode.OPPFYLT, jan1),
                )
                sak.krympUtfallsperioder(jan2) shouldBe Periodisering(
                    PeriodeMedVerdi(AvklartUtfallForPeriode.IKKE_OPPFYLT, jan2),
                )
            }
        }
    }

    @Test
    fun vilkårssett() {
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                val hele = Periode(1.januar(2024), 2.januar(2024))
                val jan1 = Periode(1.januar(2024), 1.januar(2024))
                val jan2 = Periode(2.januar(2024), 2.januar(2024))
                val sak = tac.revurderingStansIverksatt(
                    førstegangsbehandlingPeriode = hele,
                    stansFraOgMed = 2.januar(2024),
                )
                sak.vedtaksperiode shouldBe hele

                sak.vilkårssett()[0] shouldBe sak.krympVilkårssett(jan1).single()
                sak.vilkårssett()[1] shouldBe sak.krympVilkårssett(jan2).single()
                sak.vilkårssett() shouldBe sak.krympVilkårssett(hele)

                sak.vilkårssett()[0].let {
                    it.periode shouldBe jan1
                    it.verdi.kvpVilkår.vurderingsperiode shouldBe jan1
                    it.verdi.kvpVilkår.søknadSaksopplysning.totalePeriode shouldBe jan1
                    it.verdi.kvpVilkår.avklartSaksopplysning.totalePeriode shouldBe jan1
                    it.verdi.alderVilkår.vurderingsperiode shouldBe jan1
                    it.verdi.introVilkår.vurderingsperiode shouldBe jan1
                    it.verdi.introVilkår.søknadSaksopplysning.totalePeriode shouldBe jan1
                    it.verdi.introVilkår.avklartSaksopplysning.totalePeriode shouldBe jan1
                    it.verdi.institusjonsoppholdVilkår.vurderingsperiode shouldBe jan1
                    it.verdi.institusjonsoppholdVilkår.søknadSaksopplysning.totalePeriode shouldBe jan1
                    it.verdi.institusjonsoppholdVilkår.avklartSaksopplysning.totalePeriode shouldBe jan1
                    it.verdi.livsoppholdVilkår.vurderingsperiode shouldBe jan1
                    it.verdi.livsoppholdVilkår.avklartSaksopplysning!!.periode shouldBe jan1
                    it.verdi.livsoppholdVilkår.søknadssaksopplysning.periode shouldBe jan1
                    it.verdi.livsoppholdVilkår.saksbehandlerSaksopplysning!!.periode shouldBe jan1
                    it.verdi.tiltakDeltagelseVilkår.vurderingsperiode shouldBe jan1
                    it.verdi.tiltakDeltagelseVilkår.registerSaksopplysning.deltagelsePeriode shouldBe jan1
                    it.verdi.tiltakDeltagelseVilkår.avklartSaksopplysning.deltagelsePeriode shouldBe jan1
                    it.verdi.kravfristVilkår.vurderingsperiode shouldBe jan1
                }
                sak.vilkårssett()[1].let {
                    it.periode shouldBe jan2
                    it.verdi.kvpVilkår.vurderingsperiode shouldBe jan2
                    it.verdi.kvpVilkår.søknadSaksopplysning.totalePeriode shouldBe jan2
                    it.verdi.kvpVilkår.avklartSaksopplysning.totalePeriode shouldBe jan2
                    it.verdi.alderVilkår.vurderingsperiode shouldBe jan2
                    it.verdi.introVilkår.vurderingsperiode shouldBe jan2
                    it.verdi.introVilkår.søknadSaksopplysning.totalePeriode shouldBe jan2
                    it.verdi.introVilkår.avklartSaksopplysning.totalePeriode shouldBe jan2
                    it.verdi.institusjonsoppholdVilkår.vurderingsperiode shouldBe jan2
                    it.verdi.institusjonsoppholdVilkår.søknadSaksopplysning.totalePeriode shouldBe jan2
                    it.verdi.institusjonsoppholdVilkår.avklartSaksopplysning.totalePeriode shouldBe jan2
                    it.verdi.livsoppholdVilkår.vurderingsperiode shouldBe jan2
                    it.verdi.livsoppholdVilkår.avklartSaksopplysning!!.periode shouldBe jan2
                    it.verdi.livsoppholdVilkår.søknadssaksopplysning.periode shouldBe jan2
                    it.verdi.livsoppholdVilkår.saksbehandlerSaksopplysning!!.periode shouldBe jan2
                    it.verdi.tiltakDeltagelseVilkår.vurderingsperiode shouldBe jan2
                    it.verdi.tiltakDeltagelseVilkår.registerSaksopplysning.deltagelsePeriode shouldBe jan2
                    it.verdi.tiltakDeltagelseVilkår.avklartSaksopplysning.deltagelsePeriode shouldBe jan2
                    it.verdi.kravfristVilkår.vurderingsperiode shouldBe jan2
                }
            }
        }
    }
}
