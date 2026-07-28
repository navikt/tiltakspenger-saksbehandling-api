package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/**
 * Hvorfor et svar fra oppdragssystemet ikke kan tolkes som en simulering.
 *
 * Feilene her betyr at svaret bryter med kontrakten vi bygger modellen på -- for eksempel at en postering strekker seg utover meldeperioden vi selv sendte utbetalingslinjer for.
 * Da nekter vi å tolke svaret i stedet for å vise tall vi ikke kan stå for, og saksbehandler får beskjed om hvorfor.
 *
 * [beskrivelse] er PII-fri og kan vises til saksbehandler; [loggkontekst] har med de tekniske detaljene.
 */
sealed interface Simuleringsfeil : Loggbar {
    val beskrivelse: String

    /**
     * Vi sender utbetalingslinjer per meldeperiodekjede, og oppdrag arver de grensene i svaret.
     * En postering utover meldeperioden betyr at oppdrag har endret periodiseringen, og da kan ingen beløp fordeles uten å gjette.
     */
    data class PosteringUtenforMeldeperiode(
        val meldeperiodeId: MeldeperiodeId,
        val meldeperiode: Periode,
        val postering: Periode,
        val klassekode: String,
        val type: Posteringstype,
    ) : Simuleringsfeil {
        override val beskrivelse =
            "Simuleringen har en postering ($postering, $klassekode) som går utover meldeperioden $meldeperiode. Undersøk om oppdragssystemet har endret periodiseringen."

        override val loggkontekst = Loggkontekst(
            "$beskrivelse Meldeperiode: $meldeperiodeId, type: $type.",
        )
    }

    /**
     * Hjemmelsvernet grupperer justeringer per kalendermåned, siden oppdrag i dag splitter på månedsskiftet.
     * Krysser en justering likevel et månedsskifte, har oppdrag lagt om periodiseringen, og da skal månedsgrupperingen fjernes fra hjemmelsvernet -- ikke lempes på her.
     */
    data class JusteringOverMånedsskifte(
        val meldeperiodeId: MeldeperiodeId,
        val postering: Periode,
        val klassekode: String,
    ) : Simuleringsfeil {
        override val beskrivelse =
            "Simuleringen har en justering ($postering) som krysser et månedsskifte. Da kan vi ikke avgjøre hvilken måned beløpet hører til."

        override val loggkontekst = Loggkontekst(
            "$beskrivelse Meldeperiode: $meldeperiodeId, klassekode: $klassekode. Har oppdrag lagt om periodiseringen, skal månedsgrupperingen fjernes fra hjemmelsvernet.",
        )
    }

    /**
     * Svaret fra oppdragssystemet gjelder en annen person enn behandlingen.
     * Feilen bærer bevisst ikke fødselsnumrene.
     */
    data class GjelderAnnenPerson(
        val sakId: SakId?,
        val saksnummer: Saksnummer?,
    ) : Simuleringsfeil {
        override val beskrivelse = "Simuleringen fra økonomisystemet gjelder en annen person enn behandlingen."

        override val loggkontekst = Loggkontekst(
            "$beskrivelse SakId: $sakId, saksnummer: ${saksnummer?.verdi}.",
        )
    }

    /**
     * Svaret fra oppdragssystemet gjelder en annen sak enn behandlingen.
     * [beskrivelse] bærer bevisst ikke saksnumrene fra simuleringen -- de tilhører en annen sak og skal ikke ut i en brukerflate.
     */
    data class GjelderAnnenSak(
        val sakId: SakId?,
        val saksnummer: Saksnummer?,
        val saksnummerISimulering: List<Saksnummer>,
    ) : Simuleringsfeil {
        override val beskrivelse = "Simuleringen fra økonomisystemet gjelder en annen sak enn behandlingen."

        override val loggkontekst = Loggkontekst(
            "$beskrivelse Saksnummer i simuleringen: ${saksnummerISimulering.joinToString { it.verdi }}, forventet: ${saksnummer?.verdi}. SakId: $sakId.",
        )
    }

    /** Svaret har posteringer, men ingen av dem treffer sakens meldeperioder. */
    data class IngenMeldeperioderTruffet(
        val sakId: SakId?,
        val saksnummer: Saksnummer?,
        val simuleringsperiode: Periode,
    ) : Simuleringsfeil {
        override val beskrivelse =
            "Ingen av posteringene i simuleringen ($simuleringsperiode) treffer sakens meldeperioder."

        override val loggkontekst = Loggkontekst(
            "$beskrivelse SakId: $sakId, saksnummer: ${saksnummer?.verdi}.",
        )
    }
}
