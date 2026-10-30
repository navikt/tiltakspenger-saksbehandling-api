package no.nav.tiltakspenger.saksbehandling.klage.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.Avbryt
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.Gjenoppta
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.LeggTilbake
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.OppdaterSettPåVent
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.Overta
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.Saksbehandle
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.SettPåVent
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando.Tildel
import org.junit.jupiter.api.Test

class KlagebehandlingGyldigeKommandoerExTest {

    private val saksbehandler = ObjectMother.saksbehandler()
    private val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999")

    private fun klagebehandling(
        status: Klagebehandlingsstatus = Klagebehandlingsstatus.UNDER_BEHANDLING,
        saksbehandler: String? = this.saksbehandler.navIdent,
        påVent: Boolean = false,
    ): Klagebehandling = ObjectMother.opprettKlagebehandling().copy(
        status = status,
        saksbehandler = saksbehandler,
        ventestatus = if (påVent) {
            Ventestatus().settPåVent(
                tidspunktSattPåVent = nå(fixedClock),
                endretAv = this.saksbehandler.navIdent,
                begrunnelse = "venter på noe",
                status = status.name,
                frist = null,
            )
        } else {
            Ventestatus()
        },
    )

    @Test
    fun `bruker uten saksbehandlerrolle får ingen gyldige kommandoer`() {
        val klagebehandling = klagebehandling()

        listOf(
            ObjectMother.beslutter(),
            ObjectMother.saksbehandlerUtenTilgang(),
            ObjectMother.veileder(),
            ObjectMother.utvikler(),
        ).forEach { bruker ->
            klagebehandling.finnGyldigeKommandoer(bruker, null) shouldBe emptyList()
        }
    }

    @Test
    fun `saksbehandler på klagebehandlingen under behandling kan behandle, legge tilbake, sette på vent og avbryte`() {
        klagebehandling().finnGyldigeKommandoer(saksbehandler, null) shouldBe
            listOf(Saksbehandle, LeggTilbake, SettPåVent, Avbryt)
    }

    @Test
    fun `annen saksbehandler kan kun overta en klagebehandling under behandling`() {
        klagebehandling().finnGyldigeKommandoer(annenSaksbehandler, null) shouldBe listOf(Overta)
    }

    @Test
    fun `klagebehandling klar til behandling kan tildeles`() {
        klagebehandling(status = Klagebehandlingsstatus.KLAR_TIL_BEHANDLING, saksbehandler = null)
            .finnGyldigeKommandoer(saksbehandler, null) shouldBe listOf(Tildel)
    }

    @Test
    fun `klagebehandling på vent kan tildeles, oppdateres og gjenopptas av en hvilken som helst saksbehandler`() {
        val klagebehandling = klagebehandling(
            status = Klagebehandlingsstatus.KLAR_TIL_BEHANDLING,
            saksbehandler = null,
            påVent = true,
        )

        listOf(saksbehandler, annenSaksbehandler).forEach { bruker ->
            klagebehandling.finnGyldigeKommandoer(bruker, null) shouldBe listOf(Tildel, OppdaterSettPåVent, Gjenoppta)
        }
    }

    @Test
    fun `klagebehandling mottatt fra klageinstansen kan ikke legges tilbake`() {
        val opprettholdt = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
        val mottattFraKlageinstans = opprettholdt.copy(
            status = Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS,
            resultat = (opprettholdt.resultat as Klagebehandlingsresultat.Opprettholdt).copy(
                oversendtKlageinstansenTidspunkt = opprettholdt.sistEndret,
            ),
        )

        mottattFraKlageinstans.finnGyldigeKommandoer(saksbehandler, null) shouldBe listOf(Saksbehandle, SettPåVent, Avbryt)
    }

    @Test
    fun `avbrutt klagebehandling har ingen gyldige kommandoer`() {
        klagebehandling(status = Klagebehandlingsstatus.AVBRUTT)
            .finnGyldigeKommandoer(saksbehandler, null) shouldBe emptyList()
    }

    @Test
    fun `opprettholdt klagebehandling har ingen gyldige kommandoer`() {
        ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
            .finnGyldigeKommandoer(saksbehandler, null) shouldBe emptyList()
    }

    @Test
    fun `klagebehandling med aktiv tilknyttet behandling under behandling får kommandoene fra den tilknyttede behandlingen`() {
        val tilknyttet = AktivTilknyttetBehandling.Meldekort(
            ObjectMother.meldekortUnderBehandling(
                saksbehandler = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
            ),
        )

        klagebehandling().finnGyldigeKommandoer(saksbehandler, tilknyttet) shouldBe
            listOf(Saksbehandle, LeggTilbake, SettPåVent)
        klagebehandling().finnGyldigeKommandoer(annenSaksbehandler, tilknyttet) shouldBe listOf(Overta)
    }

    @Test
    fun `klagebehandling med aktiv tilknyttet behandling hos beslutter har ingen gyldige kommandoer`() {
        val tilknyttet = AktivTilknyttetBehandling.Ramme(
            ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler),
        )

        listOf(saksbehandler, annenSaksbehandler, ObjectMother.saksbehandlerOgBeslutter()).forEach { bruker ->
            klagebehandling().finnGyldigeKommandoer(bruker, tilknyttet) shouldBe emptyList()
        }
    }
}
