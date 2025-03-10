package no.nav.tiltakspenger.saksbehandling.felles

import java.util.Base64

class PdfA(private val content: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PdfA

        return content.contentEquals(other.content)
    }

    /**
     * Tar kun med de 20 første tegnene i toString for å unngå å logge unødvendig mye binærdata.
     */
    override fun toString(): String {
        val tjueFørsteTegn = content.copyOfRange(0, if (content.size > 20) 20 else content.size)
        return "$tjueFørsteTegn - size ${content.size}"
    }

    override fun hashCode(): Int = content.contentHashCode()
    fun getContent(): ByteArray = content.clone()

    fun toBase64(): String = Base64.getEncoder().encodeToString(getContent())
}
