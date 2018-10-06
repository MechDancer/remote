package org.mechdancer.remote.console.s4

import org.mechdancer.remote.console.parser.Token

abstract class CharBuffer : TokenBuffer<Char, Token<*>>() {
    protected val text get() = buffer.joinToString("").takeIf { it.isNotBlank() }
}
