package io.deltawave.primaryserver.roll

import org.jparsec.Parser
import org.jparsec.Parsers.or
import org.jparsec.Parsers.sequence
import org.jparsec.Scanners
import org.jparsec.Scanners.string
import org.jparsec.Scanners.stringCaseInsensitive
import org.jparsec.pattern.CharPredicates
import org.jparsec.pattern.Patterns

object RollTokens {
    private val lineComment: Parser<Void> = sequence(
        string("//"),
        Patterns.many(CharPredicates.notChar('\n')).toScanner("non-newline"),
        Scanners.isChar('\n')
    )
    private val whitespace: Parser<Void> = or(
        Scanners.isChar(' '),
        Scanners.isChar('\t'),
        Scanners.isChar('\n'),
        Scanners.isChar('_')
    )
    private val ws: Parser<Unit> = or(
        whitespace,
        lineComment,
    ).many().map {}

    val slashRoll: Parser<Void> = stringCaseInsensitive("/roll").between(ws, ws)

    val plus: Parser<(Int, Int) -> Int> = or(
        string("+"),
        stringCaseInsensitive("plus")
    ).between(ws, ws)
        .map { { a, b -> a + b } }
    val minus: Parser<(Int, Int) -> Int> = or(
        string("-"),
        stringCaseInsensitive("minus")
    ).between(ws, ws)
        .map { { a, b -> a - b } }

    val times: Parser<(Int, Int) -> Int> = or(
        string("*"),
        stringCaseInsensitive("times")
    ).between(ws, ws)
        .map { { a, b -> a * b } }
    val div: Parser<(Int, Int) -> Int> = or(
        string("/"),
        stringCaseInsensitive("div")
    ).between(ws, ws)
        .map { { a, b -> a / b } }

    val d: Parser<Void> = stringCaseInsensitive("d").between(ws, ws)
    val kh: Parser<Void> = stringCaseInsensitive("kh").between(ws, ws)
    val kl: Parser<Void> = stringCaseInsensitive("kl").between(ws, ws)

    val int: Parser<RollEvaluator> = Scanners.INTEGER
        .map { i -> Atomic(i.toInt()) as RollEvaluator }
        .between(ws, ws)

    val openParen: Parser<Void> = or(
        string("("),
        stringCaseInsensitive("open")
    ).between(ws, ws)
    val closeParen: Parser<Void> = or(
        string(")"),
        stringCaseInsensitive("close")
    ).between(ws, ws)
}