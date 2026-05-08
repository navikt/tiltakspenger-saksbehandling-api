package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.GjenopptaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.gjenoppta
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortbehandlingSettPåVentTest {
    @Test
    fun `setter meldekortbehandling under behandling på vent`() {
        val saksbehandler = ObjectMother.saksbehandler()
        val frist = LocalDate.now().plusDays(1)
        val meldekortbehandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        )

        val oppdatert = meldekortbehandling.settPåVent(
            kommando = settPåVentKommando(
                sakId = meldekortbehandling.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
                frist = frist,
            ),
            clock = fixedClock,
        )

        oppdatert.shouldBeInstanceOf<MeldekortUnderBehandling>()
        oppdatert.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
        oppdatert.saksbehandler shouldBe null
        oppdatert.ventestatus.erSattPåVent shouldBe true
        oppdatert.ventestatus.sattPåVentBegrunnelse shouldBe "Venter på dokumentasjon"
        oppdatert.ventestatus.sattPåVentFrist shouldBe frist
    }

    @Test
    fun `setter meldekortbehandling under beslutning på vent`() {
        val beslutter = ObjectMother.beslutter()
        val meldekortbehandling = ObjectMother.meldekortBehandletManuelt(
            status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
            beslutter = beslutter.navIdent,
            iverksattTidspunkt = null,
        )

        val oppdatert = meldekortbehandling.settPåVent(
            kommando = settPåVentKommando(
                sakId = meldekortbehandling.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandler = beslutter,
            ),
            clock = fixedClock,
        )

        oppdatert.shouldBeInstanceOf<MeldekortbehandlingManuell>()
        oppdatert.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
        oppdatert.saksbehandler shouldBe meldekortbehandling.saksbehandler
        oppdatert.beslutter shouldBe null
        oppdatert.ventestatus.erSattPåVent shouldBe true
        oppdatert.ventestatus.sattPåVentBegrunnelse shouldBe "Venter på dokumentasjon"
    }

    @Test
    fun `kan ikke sette avbrutt meldekortbehandling på vent`() {
        val saksbehandler = ObjectMother.saksbehandler()
        val meldekortbehandling = ObjectMother.meldekortbehandlingAvbrutt(
            saksbehandler = saksbehandler.navIdent,
        )

        val exception = shouldThrow<IllegalStateException> {
            meldekortbehandling.settPåVent(
                kommando = settPåVentKommando(
                    sakId = meldekortbehandling.sakId,
                    meldekortId = meldekortbehandling.id,
                    saksbehandler = saksbehandler,
                ),
                clock = fixedClock,
            )
        }

        exception.message shouldBe "Kan ikke sette meldekortbehandling på vent som har status AVBRUTT"
    }

    @Test
    fun `kan ikke sette automatisk behandlet meldekortbehandling på vent`() {
        val saksbehandler = ObjectMother.saksbehandler()
        val meldekortbehandling = ObjectMother.meldekortBehandletAutomatisk()

        val exception = shouldThrow<IllegalStateException> {
            meldekortbehandling.settPåVent(
                kommando = settPåVentKommando(
                    sakId = meldekortbehandling.sakId,
                    meldekortId = meldekortbehandling.id,
                    saksbehandler = saksbehandler,
                ),
                clock = fixedClock,
            )
        }

        exception.message shouldBe "Kan ikke sette meldekortbehandling på vent som har status AUTOMATISK_BEHANDLET"
    }

    @Test
    fun `gjenopptar meldekortbehandling klar til behandling`() {
        val clock = TikkendeKlokke(fixedClock)
        val saksbehandler = ObjectMother.saksbehandler()
        val meldekortbehandlingUnderBehandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        )
        val meldekortbehandling = meldekortbehandlingUnderBehandling.settPåVent(
            kommando = settPåVentKommando(
                sakId = meldekortbehandlingUnderBehandling.sakId,
                meldekortId = meldekortbehandlingUnderBehandling.id,
                saksbehandler = saksbehandler,
            ),
            clock = clock,
        )

        val oppdatert = meldekortbehandling.gjenoppta(
            kommando = GjenopptaMeldekortbehandlingKommando(
                sakId = meldekortbehandling.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
            ),
            clock = clock,
        )

        oppdatert.shouldBeInstanceOf<MeldekortUnderBehandling>()
        oppdatert.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
        oppdatert.saksbehandler shouldBe saksbehandler.navIdent
        oppdatert.ventestatus.erSattPåVent shouldBe false
        oppdatert.ventestatus.ventestatusHendelser.size shouldBe 2
    }

    @Test
    fun `gjenopptar meldekortbehandling under behandling`() {
        val clock = TikkendeKlokke(fixedClock)
        val saksbehandler = ObjectMother.saksbehandler()
        val meldekortbehandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        ).copy(
            ventestatus = ventestatusSattPåVent(
                endretAv = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING.toString(),
                clock = clock,
            ),
        )

        val oppdatert = meldekortbehandling.gjenoppta(
            kommando = GjenopptaMeldekortbehandlingKommando(
                sakId = meldekortbehandling.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
            ),
            clock = clock,
        )

        oppdatert.shouldBeInstanceOf<MeldekortUnderBehandling>()
        oppdatert.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
        oppdatert.saksbehandler shouldBe saksbehandler.navIdent
        oppdatert.ventestatus.erSattPåVent shouldBe false
        oppdatert.ventestatus.ventestatusHendelser.size shouldBe 2
    }

    @Test
    fun `kan ikke gjenoppta meldekortbehandling under behandling med annen saksbehandler`() {
        val clock = TikkendeKlokke(fixedClock)
        val saksbehandler = ObjectMother.saksbehandler()
        val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999")
        val meldekortbehandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        ).copy(
            ventestatus = ventestatusSattPåVent(
                endretAv = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING.toString(),
                clock = clock,
            ),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            meldekortbehandling.gjenoppta(
                kommando = GjenopptaMeldekortbehandlingKommando(
                    sakId = meldekortbehandling.sakId,
                    meldekortId = meldekortbehandling.id,
                    saksbehandler = annenSaksbehandler,
                    correlationId = CorrelationId.generate(),
                ),
                clock = clock,
            )
        }

        exception.message shouldBe "Du må være saksbehandler på meldekortbehandlingen for å kunne gjenoppta den."
    }

    @Test
    fun `gjenopptar meldekortbehandling klar til beslutning`() {
        val clock = TikkendeKlokke(fixedClock)
        val beslutter = ObjectMother.beslutter()
        val meldekortbehandlingUnderBeslutning = ObjectMother.meldekortBehandletManuelt(
            status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
            beslutter = beslutter.navIdent,
            iverksattTidspunkt = null,
        )
        val meldekortbehandling = meldekortbehandlingUnderBeslutning.settPåVent(
            kommando = settPåVentKommando(
                sakId = meldekortbehandlingUnderBeslutning.sakId,
                meldekortId = meldekortbehandlingUnderBeslutning.id,
                saksbehandler = beslutter,
            ),
            clock = clock,
        )

        val oppdatert = meldekortbehandling.gjenoppta(
            kommando = gjenopptaMeldekortbehandlingKommando(
                sakId = meldekortbehandling.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandler = beslutter,
            ),
            clock = clock,
        )

        oppdatert.shouldBeInstanceOf<MeldekortbehandlingManuell>()
        oppdatert.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
        oppdatert.beslutter shouldBe beslutter.navIdent
        oppdatert.ventestatus.erSattPåVent shouldBe false
        oppdatert.ventestatus.ventestatusHendelser.size shouldBe 2
    }

    @Test
    fun `kan ikke gjenoppta avbrutt meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClock)
        val saksbehandler = ObjectMother.saksbehandler()
        val meldekortbehandling = ObjectMother.meldekortbehandlingAvbrutt(
            saksbehandler = saksbehandler.navIdent,
            avbruttTidspunkt = nå(clock),
        )
        val meldekortbehandlingPåVent = meldekortbehandling.copy(
            ventestatus = ventestatusSattPåVent(
                endretAv = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.AVBRUTT.toString(),
                clock = clock,
            ),
        )

        val exception = shouldThrow<IllegalStateException> {
            meldekortbehandlingPåVent.gjenoppta(
                kommando = gjenopptaMeldekortbehandlingKommando(
                    sakId = meldekortbehandlingPåVent.sakId,
                    meldekortId = meldekortbehandlingPåVent.id,
                    saksbehandler = saksbehandler,
                ),
                clock = clock,
            )
        }

        exception.message shouldBe "Kan ikke gjenoppta meldekortbehandling som har status AVBRUTT"
    }

    @Test
    fun `kan ikke gjenoppta automatisk behandlet meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClock)
        val saksbehandler = ObjectMother.saksbehandler()
        val meldekortbehandling = ObjectMother.meldekortBehandletAutomatisk().copy(
            ventestatus = ventestatusSattPåVent(
                endretAv = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET.toString(),
                clock = clock,
            ),
        )

        val exception = shouldThrow<IllegalStateException> {
            meldekortbehandling.gjenoppta(
                kommando = gjenopptaMeldekortbehandlingKommando(
                    sakId = meldekortbehandling.sakId,
                    meldekortId = meldekortbehandling.id,
                    saksbehandler = saksbehandler,
                ),
                clock = clock,
            )
        }

        exception.message shouldBe "Kan ikke gjenoppta meldekortbehandling som har status AUTOMATISK_BEHANDLET"
    }

    private fun settPåVentKommando(
        sakId: SakId,
        meldekortId: MeldekortId,
        begrunnelse: String = "Venter på dokumentasjon",
        frist: LocalDate? = null,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        correlationId: CorrelationId = CorrelationId.generate(),
    ) = SettMeldekortbehandlingPåVentKommando(
        sakId = sakId,
        meldekortId = meldekortId,
        begrunnelse = begrunnelse,
        frist = frist,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )

    private fun gjenopptaMeldekortbehandlingKommando(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        correlationId: CorrelationId = CorrelationId.generate(),
    ) = GjenopptaMeldekortbehandlingKommando(
        sakId = sakId,
        meldekortId = meldekortId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )

    private fun ventestatusSattPåVent(
        endretAv: String,
        status: String,
        clock: TikkendeKlokke,
    ) = Ventestatus().settPåVent(
        tidspunktSattPåVent = nå(clock),
        endretAv = endretAv,
        begrunnelse = "Venter på dokumentasjon",
        status = status,
        frist = null,
    )
}
