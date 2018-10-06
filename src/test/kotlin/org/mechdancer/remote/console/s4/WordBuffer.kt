package org.mechdancer.remote.console.s4

import org.mechdancer.remote.console.parser.Token
import org.mechdancer.remote.console.parser.TokenType.Word

class WordBuffer : CharBuffer() {
    override fun check(char: Char) =
        depends(char.isJavaIdentifierStart() || buffer.isNotEmpty() && char.isJavaIdentifierPart())

    override fun build() =
        text?.let { Token(Word, it) }
}
