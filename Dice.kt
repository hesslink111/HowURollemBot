package io.deltawave.primaryserver.roll

import java.security.SecureRandom

open class Dice {
    private val random = SecureRandom()

    open fun roll(sides: Int): Int {
        return random.nextInt(sides) + 1
    }
}