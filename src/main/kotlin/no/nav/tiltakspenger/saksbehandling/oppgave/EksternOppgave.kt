package no.nav.tiltakspenger.saksbehandling.oppgave

import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDateTime

/**
 * En oppgave i det eksterne oppgavesystemet med et uforanderlig øyeblikksbilde av grunnlaget.
 * Foreløpig lagres bare oppgaver for endret tiltaksdeltakelse, ikke personhendelser, søknader eller meldekort.
 * Lagring for personbeskyttelse avventer juridisk avklaring og må ikke gjøre det mulig å identifisere brukeren.
 * [opprettet] er lokalt registreringstidspunkt, ikke oppgavens eksterne opprettelsestidspunkt.
 * Ved duplikattreff hos oppgavetjenesten kan oppgaven ha blitt opprettet eksternt tidligere.
 * [tilleggstekst] er teksten sendt til det eksterne oppgavesystemet, uavhengig av grunnlagstype.
 */
data class EksternOppgave(
    val oppgaveId: OppgaveId,
    val sakId: SakId,
    val opprettet: LocalDateTime,
    val grunnlag: Oppgavegrunnlag,
    val tilleggstekst: String?,
)

/**
 * En lagret [EksternOppgave], der [grunnlag] er json-en slik den ligger i databasen.
 * Grunnlaget deserialiseres ikke, siden det kun er for sporbarhet og feilsøking.
 * Eldre rader kan derfor ha et annet format enn det som skrives i dag.
 */
data class LagretEksternOppgave(
    val oppgaveId: OppgaveId,
    val sakId: SakId,
    val opprettet: LocalDateTime,
    val grunnlag: String,
    val tilleggstekst: String?,
)
