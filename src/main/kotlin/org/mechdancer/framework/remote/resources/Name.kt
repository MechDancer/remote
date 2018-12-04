package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.AbstractComponent

class Name private constructor(val field: String) :
    AbstractComponent<Name>(Name::class) {

    companion object {
        operator fun invoke(value: String) = Name(value.trim())
    }
}
