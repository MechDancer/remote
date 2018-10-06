package org.mechdancer.remote.console.s4

import org.mechdancer.remote.console.parser.Token
import org.mechdancer.remote.console.parser.TokenType.Sign

class SignBuffer : CharBuffer() {
    override fun check(char: Char) =
        depends(char in signSet)

    override fun build() =
        text?.let { Token(Sign, it) }

    private companion object {
        val signSet = setOf(
            '~', '`', '!', '@',
            '#', '$', '%',
            '^', '&', '*',
            '(', ')', '-',
            '_', '=', '+',
            '[', '{', '}', ']',
            '|', '\\', '/',
            ';', ':', '<', '>',
            ',', '.', '?'
        )
    }
}
