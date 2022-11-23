package no.nav.tiltakspenger.vedtak

interface IInnsendingHendelse : KontekstLogable, IAktivitetslogg {
    val aktivitetslogg: Aktivitetslogg
    fun journalpostId(): String
    fun toLogString(): String
}

interface ISøkerHendelse : KontekstLogable, IAktivitetslogg {
    val aktivitetslogg: Aktivitetslogg
    fun ident(): String
    fun toLogString(): String
}

abstract class InnsendingHendelse protected constructor(
    override val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : IAktivitetslogg by aktivitetslogg, IInnsendingHendelse {

    init {
        aktivitetslogg.addKontekst(this)
    }

    override fun opprettKontekst(): Kontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            Kontekst(it, mapOf("journalpostId" to journalpostId()))
        }
    }

    override fun toLogString() = aktivitetslogg.toString()
}

abstract class SøkerHendelse protected constructor(
    override val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : IAktivitetslogg by aktivitetslogg, ISøkerHendelse {

    init {
        aktivitetslogg.addKontekst(this)
    }

    override fun opprettKontekst(): Kontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            Kontekst(it, mapOf("ident" to ident()))
        }
    }

    override fun toLogString() = aktivitetslogg.toString()
}
