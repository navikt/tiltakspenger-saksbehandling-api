package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdaterMeldekortdagDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * En meldekortbehandling kan bli påvirket av andre vedtak på saken mens den står åpen.
 * Derfor kjøres en kontrollsimulering både ved send til beslutter og ved iverksettelse, og avvik fra simuleringen saksbehandler så på skal blokkere.
 */
class KontrollsimuleringMeldekortbehandlingTest {

    @Test
    fun `kontrollen følger med gjennom underkjenning og ny oppdatering`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = beslutter("beslutter"),
            )!!

            underkjennMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                beslutter = beslutter("beslutter"),
            )!!

            // Kontrollen fra forrige runde skal fortsatt ligge på behandlingen etter at saksbehandler jobber videre.
            oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(meldekortbehandling.id)!!
                .utbetalingskontroll.shouldNotBeNull()
        }
    }

    @Test
    fun `kontrollsimuleringen blokkerer ikke når ingenting har endret seg`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(meldekortbehandling.id)!!
                .utbetalingskontroll.shouldNotBeNull()

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = beslutter("beslutter"),
            )!!

            iverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                beslutter = beslutter("beslutter"),
            )!!
        }
    }

    /**
     * To meldekortbehandlinger på ulike meldeperiodekjeder er åpne samtidig.
     * Sykedagene i den første kjeden reduserer satsen i den andre, så når den første iverksettes etter at den andre er sendt til beslutter, er ikke tallene beslutter så på lenger de som ville blitt utbetalt.
     */
    @Test
    fun `iverksettelse blokkeres når en annen behandling har endret beregningen`() {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler("saksbehandler")
            val beslutter = beslutter("beslutter")

            val (sak, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 30.april(2025)),
            )

            val førsteKjede = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede[0]
            val andreKjede = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede[1]

            // Sykedagene i de to kjedene krysser til sammen arbeidsgiverperioden på 16 dager, så den andre kjeden får redusert sats -- men først når den første er vedtatt.
            val (_, opprettetAndreBehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjede.kjedeId,
                saksbehandler = saksbehandler,
            )!!
            val (_, andreBehandling) = oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = opprettetAndreBehandling.id,
                saksbehandler = saksbehandler,
                meldeperioder = listOf(sykedager(opprettetAndreBehandling)),
            )!!

            val (_, opprettetFørsteBehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
                saksbehandler = saksbehandler,
            )!!
            val (_, førsteBehandling) = oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = opprettetFørsteBehandling.id,
                saksbehandler = saksbehandler,
                meldeperioder = listOf(sykedager(opprettetFørsteBehandling)),
            )!!

            // Beslutter ser på tallene for den andre kjeden før den første er iverksatt.
            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = andreBehandling.id,
                saksbehandler = saksbehandler,
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                saksbehandler = saksbehandler,
            )!!
            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                saksbehandlerEllerBeslutter = beslutter,
            )!!
            iverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                beslutter = beslutter,
            )!!

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = andreBehandling.id,
                saksbehandlerEllerBeslutter = beslutter,
            )!!

            iverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = andreBehandling.id,
                beslutter = beslutter,
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
                medJsonBody = { it harKode "simulering_endret" },
            )

            // Kontrollen lagres slik at beslutter ser hva som avviker, og behandlingen blir stående til beslutning.
            val behandlingEtter = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(andreBehandling.id)!!
            behandlingEtter.utbetalingskontroll.shouldNotBeNull()
            behandlingEtter.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
        }
    }

    /**
     * Samme scenario som over, men avviket oppdages allerede når saksbehandler sender behandlingen til beslutter.
     * Da skal behandlingen bli stående under behandling, og kontrollen lagres slik at saksbehandler ser hva som avviker.
     */
    @Test
    fun `send til beslutter blokkeres når en annen behandling har endret beregningen`() {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler("saksbehandler")
            val beslutter = beslutter("beslutter")

            val (sak, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 30.april(2025)),
            )

            val førsteKjede = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede[0]
            val andreKjede = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede[1]

            val (_, opprettetAndreBehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjede.kjedeId,
                saksbehandler = saksbehandler,
            )!!
            val (_, andreBehandling) = oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = opprettetAndreBehandling.id,
                saksbehandler = saksbehandler,
                meldeperioder = listOf(sykedager(opprettetAndreBehandling)),
            )!!

            val (_, opprettetFørsteBehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
                saksbehandler = saksbehandler,
            )!!
            val (_, førsteBehandling) = oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = opprettetFørsteBehandling.id,
                saksbehandler = saksbehandler,
                meldeperioder = listOf(sykedager(opprettetFørsteBehandling)),
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                saksbehandler = saksbehandler,
            )!!
            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                saksbehandlerEllerBeslutter = beslutter,
            )!!
            iverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                beslutter = beslutter,
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = andreBehandling.id,
                saksbehandler = saksbehandler,
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
                medJsonBody = { it harKode "simulering_endret" },
            )

            val behandlingEtter = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(andreBehandling.id)!!
            behandlingEtter.utbetalingskontroll.shouldNotBeNull()
            behandlingEtter.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
        }
    }

    /** Alle ukedagene som gir rett fylles ut som sykedager, slik at arbeidsgiverperioden telles opp. */
    private fun sykedager(behandling: MeldekortUnderBehandling): OppdatertMeldeperiodeDTO {
        val meldeperiodebehandling = behandling.meldeperioder.single()
        return OppdatertMeldeperiodeDTO(
            kjedeId = meldeperiodebehandling.kjedeId.verdi,
            dager = meldeperiodebehandling.dager.map { dag ->
                OppdaterMeldekortdagDTO(
                    dato = dag.dato,
                    status = when {
                        dag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                        dag.dato.erHelg() -> MeldekortDagStatusDTO.IKKE_TILTAKSDAG
                        else -> MeldekortDagStatusDTO.FRAVÆR_SYK
                    },
                )
            },
        )
    }
}
