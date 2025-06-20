package no.nav.tiltakspenger.saksbehandling.behandling.domene

enum class ManueltBehandlesGrunn(val visningsnavn: String) {
    SOKNAD_HAR_ANDRE_YTELSER("Bruker har svart ja på spørsmål om andre ytelser i søknaden"),
    SOKNAD_HAR_LAGT_TIL_BARN_MANUELT("Bruker har lagt til barn manuelt i søknaden"),
    SOKNAD_BARN_UTENFOR_EOS("Bruker har barn som oppholder seg utenfor EØS"),
    SOKNAD_BARN_FYLLER_16_I_SOKNADSPERIODEN("Bruker har barn som fyller 16 år i løpet av søknadsperioden"),

    SAKSOPPLYSNING_FANT_IKKE_TILTAK("Fant ikke tiltaksdeltakelsen det er søkt for"),
    SAKSOPPLYSNING_OVERLAPPENDE_TILTAK("Bruker har overlappende tiltaksdeltakelser i søknadsperioden"),
    SAKSOPPLYSNING_MINDRE_ENN_14_DAGER_MELLOM_TILTAK_OG_SOKNAD("Bruker har tiltaksdeltakelse som starter eller slutter mindre enn 14 dager før eller etter søknadsperioden"),
    SAKSOPPLYSNING_ULIK_TILTAKSPERIODE("Tiltaksdeltakelsen har ikke samme periode som det er søkt for"),
    SAKSOPPLYSNING_ANDRE_YTELSER("Bruker mottar andre ytelser i søknadsperioden"),

    ANNET_APEN_BEHANDLING("Det finnes en åpen behandling for søker"),
    ANNET_BEHANDLING_FOR_SAMME_PERIODE("Det finnes en annen behandling som overlapper med søknadsperioden"),
    ANNET_HAR_SOKT_FOR_SENT("Tiltaksdeltakelsen startet mer enn tre måneder før kravdato"),
}
