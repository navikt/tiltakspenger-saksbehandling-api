@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.fakes.repos.SakFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.sammenlign
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeHenteUtbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingGateway
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

class UtbetalingFakeGateway(
    private val sakFakeRepo: SakFakeRepo,
) : UtbetalingGateway {
    private val utbetalinger = Atomic(mutableMapOf<VedtakId, Utbetaling>())

    override suspend fun iverksett(
        vedtak: Utbetalingsvedtak,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        val response = SendtUtbetaling("request - ${vedtak.id}", "response - ${vedtak.id}", responseStatus = 202)
        val utbetaling = Utbetaling(vedtak, correlationId, response)
        utbetalinger.get()[vedtak.id] = utbetaling
        return response.right()
    }

    override suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<KunneIkkeHenteUtbetalingsstatus, Utbetalingsstatus> {
        return Utbetalingsstatus.Ok.right()
    }

    override suspend fun simuler(
        behandling: MeldekortBehandling,
        brukersNavkontor: Navkontor,
        forrigeUtbetalingJson: String?,
        forrigeVedtakId: VedtakId?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        val sak = sakFakeRepo.hentForSakId(behandling.sakId)!!
        val simuleringForMeldeperioder = behandling.beregning!!.beregninger.map { beregningEtter ->
            val beregningFør = sak.meldeperiodeBeregninger.sisteBeregningFør(
                beregningEtter.beregningMeldekortId,
                beregningEtter.kjedeId,
            )
            val sammenligning = sammenlign(
                forrigeBeregning = beregningFør,
                gjeldendeBeregning = beregningEtter,
            )
            SimuleringForMeldeperiode(
                // TODO jah: [MeldeperiodeBeregning] bør ha en meldeperiodeId. Blir mer riktig å bruke den enn kjedeId.
                meldeperiode = sak.hentSisteMeldeperiodeForKjede(beregningEtter.kjedeId),
                simuleringsdager = sammenligning.dager.mapNotNull {
                    if (it.erEndret) {
                        val erFeilutbetaling = it.totalbeløpEndring < 0
                        Simuleringsdag(
                            dato = it.dato,
                            tidligereUtbetalt = it.forrigeTotalbeløp,
                            nyUtbetaling = max(it.totalbeløpEndring, 0),
                            totalEtterbetaling = max(it.totalbeløpEndring, 0),
                            totalFeilutbetaling = if (erFeilutbetaling) abs(it.totalbeløpEndring) else 0,
                            posteringsdag = PosteringerForDag(
                                dato = it.dato,
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = it.dato,
                                        fagområde = "TILTAKSPENGER",
                                        beløp = it.nyttTotalbeløp,
                                        // TODO jah: Legger inn denne enkel/feil. Utvid med https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md hvis du trenger.
                                        type = if (erFeilutbetaling) Posteringstype.FEILUTBETALING else Posteringstype.YTELSE,
                                        // TODO jah: Denne forblir enkelt. Utvid hvis du trenger
                                        klassekode = "test_klassekode",
                                    ),
                                ),
                            ),
                        )
                    } else {
                        null
                    }
                }.toNonEmptyListOrNull()!!,
            )
        }
        return SimuleringMedMetadata(
            simulering = if (simuleringForMeldeperioder.isEmpty()) {
                Simulering.IngenEndring
            } else {
                Simulering.Endring(
                    simuleringPerMeldeperiode = simuleringForMeldeperioder,
                    datoBeregnet = LocalDate.now(),
                    // TODO jah: Litt usikker på hva denne kommer som fra OS.
                    totalBeløp = simuleringForMeldeperioder.sumOf { it.nyUtbetaling },
                )
            },
            originalJson = "{}",
        ).right()
    }
    data class Utbetaling(
        val vedtak: Utbetalingsvedtak,
        val correlationId: CorrelationId,
        val sendtUtbetaling: SendtUtbetaling,
    )
}
