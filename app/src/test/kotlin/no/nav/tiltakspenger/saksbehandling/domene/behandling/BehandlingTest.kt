package no.nav.tiltakspenger.saksbehandling.domene.behandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.common.januarDateTime
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Avbrutt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.ValgteTiltaksdeltakelser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BehandlingTest {

    @Test
    fun `virkingsperiode skal være innenfor eller lik tiltaksperiode`() {
        assertDoesNotThrow {
            // starter samtidig
            ObjectMother.nyBehandling(
                status = Behandlingsstatus.KLAR_TIL_BESLUTNING,
                virkningsperiode = Periode(10.januar(2024), 23.januar(2024)),
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser(
                    Periodisering(
                        PeriodeMedVerdi(
                            verdi = ObjectMother.tiltaksdeltagelse(fom = 10.januar(2024), tom = 23.januar(2024)),
                            periode = Periode(10.januar(2024), 23.januar(2024)),
                        ),
                    ),
                ),
            )

            // starter etter
            ObjectMother.nyBehandling(
                status = Behandlingsstatus.KLAR_TIL_BESLUTNING,
                virkningsperiode = Periode(11.januar(2024), 23.januar(2024)),
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser(
                    Periodisering(
                        PeriodeMedVerdi(
                            verdi = ObjectMother.tiltaksdeltagelse(fom = 10.januar(2024), tom = 23.januar(2024)),
                            periode = Periode(10.januar(2024), 23.januar(2024)),
                        ),
                    ),
                ),
            )

            // slutter før
            ObjectMother.nyBehandling(
                status = Behandlingsstatus.UNDER_BESLUTNING,
                beslutterIdent = ObjectMother.beslutter().navIdent,
                sendtTilBeslutning = 10.januarDateTime(2024),
                virkningsperiode = Periode(10.januar(2024), 22.januar(2024)),
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser(
                    Periodisering(
                        PeriodeMedVerdi(
                            verdi = ObjectMother.tiltaksdeltagelse(fom = 10.januar(2024), tom = 23.januar(2024)),
                            periode = Periode(10.januar(2024), 23.januar(2024)),
                        ),
                    ),
                ),
            )
            // starter før og slutter etter
            ObjectMother.nyBehandling(
                status = Behandlingsstatus.VEDTATT,
                beslutterIdent = ObjectMother.beslutter().navIdent,
                iverksattTidspunkt = 10.januarDateTime(2024),
                sendtTilBeslutning = 10.januarDateTime(2024),
                virkningsperiode = Periode(11.januar(2024), 22.januar(2024)),
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser(
                    Periodisering(
                        PeriodeMedVerdi(
                            verdi = ObjectMother.tiltaksdeltagelse(fom = 10.januar(2024), tom = 23.januar(2024)),
                            periode = Periode(10.januar(2024), 23.januar(2024)),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `kan avbryte en behandling`() {
        val behandling = ObjectMother.nyBehandling()
        val avbruttBehandling = behandling.avbryt(
            avbruttAv = ObjectMother.saksbehandler(),
            begrunnelse = "begrunnelse",
            tidspunkt = førsteNovember24,
        )

        avbruttBehandling.erAvsluttet shouldBe true
        avbruttBehandling.avbrutt.let {
            it shouldNotBe null
            it!!.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            it.begrunnelse shouldBe "begrunnelse"
            it.tidspunkt shouldBe førsteNovember24
        }
        avbruttBehandling.søknad!!.avbrutt shouldNotBe null
        avbruttBehandling.avbrutt shouldBe avbruttBehandling.søknad!!.avbrutt
        avbruttBehandling.status shouldBe Behandlingsstatus.AVBRUTT
    }

    @Test
    fun `kan ikke avbryte en avbrutt behandling`() {
        val avbruttBehandling = ObjectMother.nyBehandling(
            status = Behandlingsstatus.AVBRUTT,
            avbrutt = Avbrutt(
                tidspunkt = førsteNovember24,
                saksbehandler = "navident",
                begrunnelse = "skal få exception",
            ),
        )

        assertThrows<IllegalArgumentException> {
            avbruttBehandling.avbryt(
                avbruttAv = ObjectMother.saksbehandler(),
                begrunnelse = "begrunnelse",
                tidspunkt = førsteNovember24,
            )
        }
    }
}
