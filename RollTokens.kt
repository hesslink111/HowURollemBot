package io.deltawave.primaryserver.roll

import org.jparsec.Parser
import org.jparsec.Parsers.or
import org.jparsec.Parsers.sequence
import org.jparsec.Scanners
import org.jparsec.pattern.CharPredicates
import org.jparsec.pattern.Patterns

object RollTokens {
    private val lineComment: Parser<Void> = sequence(
        Scanners.string("//"),
        Patterns.many(CharPredicates.notChar('\n')).toScanner("non-newline"),
        Scanners.isChar('\n')
    )
    private val whitespace: Parser<Void> = or(
        Scanners.isChar(' '),
        Scanners.isChar('\t'),
        Scanners.isChar('\n')
    )
    val ws: Parser<Unit> = or(
        whitespace,
        lineComment
    ).many().map {}

    val slashRoll: Parser<Void> = Scanners.string("/roll").between(ws, ws)

    val plus: Parser<(Int, Int) -> Int> = Scanners.string("+").between(ws, ws)
        .map { { a, b -> a + b } }
    val minus: Parser<(Int, Int) -> Int> = Scanners.string("-").between(ws, ws)
        .map { { a, b -> a - b } }

    val times: Parser<(Int, Int) -> Int> = Scanners.string("*").between(ws, ws)
        .map { { a, b -> a * b } }
    val div: Parser<(Int, Int) -> Int> = Scanners.string("/").between(ws, ws)
        .map { { a, b -> a / b } }

    val d: Parser<Void> = Scanners.string("d").between(ws, ws)
    val kh: Parser<Void> = Scanners.string("kh").between(ws, ws)
    val kl: Parser<Void> = Scanners.string("kl").between(ws, ws)

    val int: Parser<RollEvaluator> = sequence(
        or(plus, minus).optional { _, b -> b },
        Scanners.INTEGER
    ) { op, i -> Atomic(op(0, i.toInt())) as RollEvaluator }.between(ws, ws)

    val openParen: Parser<Void> = Scanners.string("(").between(ws, ws)
    val closeParen: Parser<Void> = Scanners.string(")").between(ws, ws)
}