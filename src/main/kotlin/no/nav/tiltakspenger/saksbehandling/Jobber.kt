package no.nav.tiltakspenger.saksbehandling

import no.nav.tiltakspenger.libs.jobber.TaskGruppe
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.behandling.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.klage.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.statistikk.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.søknad.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.tasks
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.jobb.taskGruppe
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.jobb.tasks

/**
 * Samler jobbene.
 * Hver jobb definerer selv sine tasks med navn og intervall.
 * Jobbene står i krøllparenteser, så de og contextene deres bygges først når en task kjører, ikke ved oppstart.
 */
fun ApplicationContext.jobber(isNais: Boolean): List<Task> {
    val alltid = listOf(
        { søknadContext.delautomatiskSøknadsbehandlingJobb }.tasks(),
        { utbetalingContext.journalførMeldekortvedtakJobb }.tasks(),
        { utbetalingContext.distribuerMeldekortvedtaksbrevJobb }.tasks(),
        { utbetalingContext.sendUtbetalingerJobb }.tasks(),
        { utbetalingContext.oppdaterUtbetalingsstatusJobb }.tasks(),
        { utbetalingContext.behandleUtbetalingskvitteringerJobb }.tasks(),
        { utbetalingContext.opphevUtbetalingssperrerJobb }.tasks(),
        { behandlingContext.journalførRammevedtaksbrevJobb }.tasks(),
        { behandlingContext.distribuerRammevedtaksbrevJobb }.tasks(),
        { journalnotatContext.journalførJournalnotatJobb }.tasks(),
        { klagebehandlingContext.journalførKlagebrevJobb }.tasks(),
        { klagebehandlingContext.distribuerKlagebrevJobb }.tasks(),
        { klagebehandlingContext.oversendKlageTilKlageinstansJobb }.tasks(),
        { klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb }.tasks(),
        { meldekortContext.sendTilMeldekortApiJobb }.tasks(),
        { meldekortContext.automatiskMeldekortbehandlingJobb }.tasks(),
        { behandlingContext.oppdatertTiltaksdeltakelseJobb }.tasks(),
        { behandlingContext.delautomatiskRevurderingJobb }.tasks(),
        { statistikkContext.synkroniserTiltakstypekodeverkJobb }.tasks(),
    )
    val bareINais = listOf(
        { datadelingContext.sendTilDatadelingJobb }.tasks(),
        { søknadContext.sendSøknadsutfallTilSøknadApiJobb }.tasks(),
        { personhendelseContext.personhendelseJobb }.tasks(),
        { identhendelseContext.identhendelseJobb }.tasks(),
        { tilbakekrevingContext.behandleTilbakekrevingHendelserJobb }.tasks(),
    )
    return (alltid + if (isNais) bareINais else emptyList()).flatten()
}

fun ApplicationContext.taskGrupper(): List<TaskGruppe> {
    return listOf(
        { utbetalingContext.oppdaterUtbetalingsoversiktJobb }.taskGruppe(),
    )
}
