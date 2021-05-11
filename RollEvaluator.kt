package io.deltawave.primaryserver.roll

import com.pengrad.telegrambot.model.MessageEntity
import java.lang.StringBuilder

interface RollEvaluator {
    fun specialCircumstances(specials: MutableSet<String>)
    fun printParseTree(builder: StringBuilder, command: Boolean)
    fun print(builder: TStringBuilder)
    fun eval(): Int
}

class Operation(
    private val a: RollEvaluator,
    private val opSymbol: String,
    private val opCommand: String,
    private val operation: (Int, Int) -> Int,
    private val b: RollEvaluator
): RollEvaluator {
    override fun specialCircumstances(specials: MutableSet<String>) {
        // None.
    }

    override fun printParseTree(builder: StringBuilder, command: Boolean) {
        val sp = if(command) "" else " "
        val op = if(command) opCommand else opSymbol

        a.printParseTree(builder, command)
        builder.append("$sp$op$sp")
        b.printParseTree(builder, command)
    }

    override fun print(builder: TStringBuilder) {
        a.print(builder)
        builder.append(" $opSymbol ")
        b.print(builder)
    }

    override fun eval(): Int {
        return operation(a.eval(), b.eval())
    }
}

class DiceRoll(
    private val x: RollEvaluator,
    private val y: RollEvaluator,
    private val kh: RollEvaluator?,
    private val kl: RollEvaluator?,
    dice: Dice
): RollEvaluator {
    init {
        if(x.eval() > 1000) {
            throw IllegalArgumentException("Number of dice too high: ${x.eval()}")
        }
    }
    private val values = (0 until x.eval()).mapIndexed { i, _ -> i to dice.roll(y.eval()) }
    private val keepHigh = (kh ?: x.takeIf { kl == null })?.eval() ?: 0
    private val keepLow = (kl ?: x.takeIf { kh == null })?.eval() ?: 0
    private val high = values.sortedByDescending { (_, v) -> v }
        .take(keepHigh)
    private val low = values.sortedByDescending { (_, v) -> v }
        .takeLast(keepLow)
    private val kept = (high.map { (i, _) -> i } + low.map { (i, _) -> i }).toSet()

    override fun specialCircumstances(specials: MutableSet<String>) {
        if(y.eval() == 20 && values.any { (i, v) -> i in kept && v == 20 }) {
            specials.add("#Natural20")
        }
    }

    override fun printParseTree(builder: StringBuilder, command: Boolean) {
        x.printParseTree(builder, command)
        builder.append("d")
        y.printParseTree(builder, command)
        if(kh != null) {
            builder.append("kh")
            kh.printParseTree(builder, command)
        }
        if(kl != null) {
            builder.append("kl")
            kl.printParseTree(builder, command)
        }
    }

    override fun print(builder: TStringBuilder) {
        builder.append("( ")

        for(i in 0 until x.eval()) {
            if(i > 0) {
                builder.append(" + ")
            }

            builder.append(values[i].second.toString(), listOfNotNull(
                if(values[i].second == y.eval()) MessageEntity.Type.underline else null,
                if(values[i].first !in kept) MessageEntity.Type.strikethrough else null
            ))
        }

        if(x.eval() == 0) {
            builder.append("0")
        }

        builder.append(" )")
    }

    override fun eval(): Int {
        return values
            .filter { (i, _) -> i in kept }
            .sumBy { (_, v) -> v }
    }
}

class Parens(private val a: RollEvaluator): RollEvaluator {
    override fun specialCircumstances(specials: MutableSet<String>) {
        // None.
    }

    override fun printParseTree(builder: StringBuilder, command: Boolean) {
        val sp = if(command) "" else " "
        val openParen = if(command) "open" else "("
        val closeParen = if(command) "close" else ")"
        builder.append("$openParen$sp")
        a.printParseTree(builder, command)
        builder.append("$sp$closeParen")
    }

    override fun print(builder: TStringBuilder) {
        builder.append("( ")
        a.print(builder)
        builder.append(" )")
    }

    override fun eval(): Int {
        return a.eval()
    }
}

class Atomic(private val i: Int): RollEvaluator {
    override fun specialCircumstances(specials: MutableSet<String>) {
        // None.
    }

    override fun printParseTree(builder: StringBuilder, command: Boolean) {
        builder.append(i.toString())
    }

    override fun print(builder: TStringBuilder) {
        builder.append(i.toString())
    }

    override fun eval(): Int {
        return i
    }
}