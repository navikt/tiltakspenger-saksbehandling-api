package no.nav.tiltakspenger.saksbehandling.felles

/**
 * Domenefeil som bærer sin egen loggkontekst, slik at route-laget kan logge én linje uten å utlede konteksten selv.
 * Den som bygger feilen legger inn det den unikt vet der og da.
 *
 * Brukes sammen med `ApplicationCall.loggOgSvarFeil` i route-laget.
 */
// TODO jah: Flytt til tiltakspenger-libs (ktor-common) sammen med ApplicationCall.loggOgSvarFeil når mønsteret har satt seg i flere routes.
interface Loggbar {
    /**
     * Kontekst til vanlig logg.
     * Skal være PII-fri uten unntak - sensitiv kontekst hører hjemme i [sikkerloggkontekst].
     * Det gjelder også [Loggkontekst.underliggendeFeil]: en feil med sensitivt innhold i meldingen (f.eks. rå responskropp) skal ikke legges her.
     */
    val loggkontekst: Loggkontekst

    /**
     * Sensitiv detaljkontekst (fnr, rå responser o.l.) som kun skal til sikkerlogg.
     * Når satt logger `loggOgSvarFeil` en parallell linje i sikkerlogg på samme nivå, og henviser til den fra vanlig logg med lenke.
     * Skal ikke brukes til å flytte feilsøkbar kontekst ut av vanlig logg - kun til det som faktisk er sensitivt.
     */
    val sikkerloggkontekst: Loggkontekst? get() = null
}

/**
 * Melding og eventuell underliggende feil for én logglinje.
 * [melding] er påkrevd ved konstruksjon, slik at en [underliggendeFeil] aldri kan logges uten en forklarende melding.
 */
data class Loggkontekst(
    val melding: String,
    val underliggendeFeil: Throwable? = null,
)
