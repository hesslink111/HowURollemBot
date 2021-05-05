package io.deltawave.primaryserver.roll

import org.jparsec.Parser
import org.jparsec.Parsers.or
import org.jparsec.Parsers.sequence

class RollParser(private val dice: Dice) {
    fun parse(value: String): RollEvaluator {
        return command.parse(value)
    }

    private val command: Parser<RollEvaluator> by lazy {
        sequence(
            RollTokens.slashRoll,
            rollParser
        )
    }

    private val rollParser: Parser<RollEvaluator> by lazy {
        val ref = Parser.newReference<RollEvaluator>()
        val p = additive(ref.lazy())
        ref.set(p)
        return@lazy p
    }

    private fun additive(parser: Parser<RollEvaluator>): Parser<RollEvaluator> {
        val ref = Parser.newReference<RollEvaluator>()
        val p = or(
            sequence(
                multiplicative(parser),
                or(RollTokens.plus.map { "+" to it }, RollTokens.minus.map { "-" to it }),
                ref.lazy()
            ) { a, (symbol, op), b -> Operation(a, symbol, op, b) },
            multiplicative(parser)
        )
        ref.set(p)
        return p
    }


    private fun multiplicative(parser: Parser<RollEvaluator>): Parser<RollEvaluator> {
        val ref = Parser.newReference<RollEvaluator>()
        val p = or(
            sequence(
                d(parser),
                or(RollTokens.times.map { "*" to it }, RollTokens.div.map { "/" to it }),
                ref.lazy()
            ) { a, (symbol, op), b -> Operation(a, symbol, op, b) },
            d(parser)
        )
        ref.set(p)
        return p
    }

    private fun k(parser: Parser<RollEvaluator>): Parser<K> = or(
        sequence(
            sequence(
                RollTokens.kh,
                parens(parser).optional(Atomic(1))
            ) { _, a -> a },
            sequence(
                RollTokens.kl,
                parens(parser).optional(Atomic(1))
            ) { _, a -> a }.asOptional(),
        ) { a, b -> K(a, b.orElseGet { null }) },
        sequence(
            sequence(
                RollTokens.kl,
                parens(parser).optional(Atomic(1))
            ) { _, a -> a },
            sequence(
                RollTokens.kh,
                parens(parser).optional(Atomic(1))
            ) { _, a -> a }.asOptional(),
        ) { b, a -> K(a.orElseGet { null }, b) }
    )

    private data class K(val kh: RollEvaluator?, val kl: RollEvaluator?)

    private fun d(parser: Parser<RollEvaluator>): Parser<RollEvaluator> = or(
        sequence(
            parens(parser).optional(Atomic(1)),
            RollTokens.d,
            parens(parser),
            k(parser).optional(K(null, null))
        ) { a, _, b, (kh, kl) -> DiceRoll(a, b, kh, kl, dice) },
        parens(parser)
    )

    private fun parens(parser: Parser<RollEvaluator>): Parser<RollEvaluator> = or(
        sequence(
            RollTokens.openParen,
            parser,
            RollTokens.closeParen
        ) { _, p, _ -> Parens(p) },
        atomic()
    )

    private fun atomic(): Parser<RollEvaluator> = RollTokens.int
}