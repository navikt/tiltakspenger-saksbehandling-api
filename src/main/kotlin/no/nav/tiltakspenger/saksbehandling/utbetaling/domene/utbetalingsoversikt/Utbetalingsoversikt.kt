package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.periode.Periode
import ulid.ULID
import java.time.LocalDate
import java.time.LocalDateTime

data class UtbetalingsoversiktId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    companion object {
        private const val PREFIX = "utbetalingsoversikt"

        fun random() = UtbetalingsoversiktId(UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): UtbetalingsoversiktId {
            require(stringValue.startsWith(PREFIX)) { "Ugyldig UtbetalingsoversiktId: må starte med $PREFIX ($stringValue)" }
            return UtbetalingsoversiktId(UlidBase(stringValue))
        }
    }
}

/** Ett oppslag mot økonomisystemet for én sak, slik det gikk. */
sealed interface Utbetalingsoversikt {
    val id: UtbetalingsoversiktId
    val sakId: SakId
    val hentet: LocalDateTime
    val oppslag: Oppslag
    val plan: Oppslagsplan

    data class Vellykket(
        override val id: UtbetalingsoversiktId,
        override val sakId: SakId,
        override val hentet: LocalDateTime,
        override val oppslag: Oppslag,
        override val plan: Oppslagsplan,
        val utbetalinger: List<RegistrertUtbetaling>,
    ) : Utbetalingsoversikt {
        init {
            require(plan.antallFeilPåRad == 0) { "Et vellykket oppslag kan ikke ha feil på rad" }
            require(plan.nesteOppslag >= hentet) { "Neste oppslag kan ikke være før oversikten ble hentet" }
        }
    }

    data class Feilet(
        override val id: UtbetalingsoversiktId,
        override val sakId: SakId,
        override val hentet: LocalDateTime,
        override val oppslag: Oppslag,
        override val plan: Oppslagsplan,
        val feiltype: Oppslagsfeiltype,
    ) : Utbetalingsoversikt {
        init {
            require(plan.antallFeilPåRad > 0) { "Et feilet oppslag må telle minst én feil" }
            require(plan.nesteOppslag >= hentet) { "Neste oppslag kan ikke være før oversikten ble hentet" }
        }
    }
}

/** Perioden og periodetypen vi spurte med. */
data class Oppslag(
    val periode: Periode,
    val periodetype: Oppslagsperiodetype,
) {
    companion object {
        /**
         * Månedsytelser utbetales før utgangen av måneden de gjelder, og kan derfor være postert før en periode som starter senere i samme måned.
         * Se https://lovdata.no/lov/1997-02-28-19/§22-10 og https://lovdata.no/lov/2005-04-29-21/§11.
         * Oppslaget starter likevel dette antallet måneder før grunnlagets første periode, regnet fra første dag i måneden, som margin for ytelser med ukjent utbetalingsrytme.
         */
        const val MÅNEDER_FØR_FØRSTE_PERIODE = 1L

        /**
         * Gjør oppslag fra første dag i måneden som ligger [MÅNEDER_FØR_FØRSTE_PERIODE] måneder før grunnlagets første periode, til og med [iDag].
         * Sluttdatoen er alltid [iDag] fordi økonomisystemet ikke godtar en sluttdato etter dagens dato, og starten settes til [iDag] bare når den ellers ville kommet etter sluttdatoen.
         * Måneder som ikke er postert ennå, finnes ikke i økonomisystemet og kommer med i et senere oppslag.
         * Svaret avgrenses uansett til grunnlagets perioder før lagring.
         */
        fun forGrunnlag(grunnlag: Utbetalingsoversiktgrunnlag, iDag: LocalDate): Oppslag {
            val førsteFraOgMed = grunnlag.perioder.minOf { it.fraOgMed }
            return Oppslag(
                periode = Periode(
                    fraOgMed = minOf(førsteFraOgMed.withDayOfMonth(1).minusMonths(MÅNEDER_FØR_FØRSTE_PERIODE), iDag),
                    tilOgMed = iDag,
                ),
                periodetype = Oppslagsperiodetype.UTBETALINGSPERIODE,
            )
        }
    }
}

sealed interface Utbetalingsoversiktstatus {
    val antallFeilPåRad: Int

    data object IkkeHentet : Utbetalingsoversiktstatus {
        override val antallFeilPåRad = 0
    }

    data class SisteOppslagVellykket(
        val oversikt: Utbetalingsoversikt.Vellykket,
    ) : Utbetalingsoversiktstatus {
        override val antallFeilPåRad = 0
    }

    data class SisteOppslagFeilet(
        val oversikt: Utbetalingsoversikt.Feilet,
        val sisteVellykkede: Utbetalingsoversikt.Vellykket?,
    ) : Utbetalingsoversiktstatus {
        override val antallFeilPåRad = oversikt.plan.antallFeilPåRad

        init {
            require(sisteVellykkede == null || sisteVellykkede.sakId == oversikt.sakId) { "Oversiktene må gjelde samme sak" }
            require(sisteVellykkede == null || compareValuesBy(sisteVellykkede, oversikt, { it.hentet }, { it.id.toString() }) < 0) {
                "Siste vellykkede må komme før siste oppslag, målt på hentet og deretter id"
            }
        }
    }
}
