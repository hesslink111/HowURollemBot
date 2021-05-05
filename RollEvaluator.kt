package io.deltawave.primaryserver.roll

import java.lang.StringBuilder

interface RollEvaluator {
    fun specialCircumstances(specials: MutableSet<String>)
    fun printParseTree(builder: StringBuilder)
    fun print(builder: StringBuilder)
    fun eval(): Int
}

class Operation(
    private val a: RollEvaluator,
    private val opSymbol: String,
    private val op: (Int, Int) -> Int,
    private val b: RollEvaluator
): RollEvaluator {
    override fun specialCircumstances(specials: MutableSet<String>) {
        // None.
    }

    override fun printParseTree(builder: StringBuilder) {
        a.printParseTree(builder)
        builder.append(" $opSymbol ")
        b.printParseTree(builder)
    }

    override fun print(builder: StringBuilder) {
        a.print(builder)
        builder.append(" $opSymbol ")
        b.print(builder)
    }

    override fun eval(): Int {
        return op(a.eval(), b.eval())
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
        if(x.eval() > 10000) {
            throw IllegalArgumentException("Number of dice too high: ${x.eval()}")
        }
    }
    private val values = (0 until x.eval()).mapIndexed { i, _ -> i to dice.roll(y.eval()) }
    private val keepHigh = (kh ?: x.takeIf { kl == null } ?: Atomic(0)).eval()
    private val keepLow = (kl ?: x.takeIf { kh == null } ?: Atomic(0)).eval()
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

    override fun printParseTree(builder: StringBuilder) {
        x.printParseTree(builder)
        builder.append("d")
        y.printParseTree(builder)
        if(kh != null) {
            builder.append("kh")
            kh.printParseTree(builder)
        }
        if(kl != null) {
            builder.append("kl")
            kl.printParseTree(builder)
        }
    }

    override fun print(builder: StringBuilder) {
        for(i in 0 until x.eval()) {
            if(i > 0) {
                builder.append(" + ")
            }

            val highestPrefix = if(values[i].second == y.eval()) "<u>" else ""
            val highestPostfix = if(values[i].second == y.eval()) "</u>" else ""
            val keptPrefix = if(values[i].first !in kept) "<s>" else ""
            val keptPostfix = if(values[i].first !in kept) "</s>" else ""

            builder.append("$keptPrefix$highestPrefix${values[i].second}$highestPostfix$keptPostfix")
        }
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

    override fun printParseTree(builder: StringBuilder) {
        builder.append("( ")
        a.printParseTree(builder)
        builder.append(" )")
    }

    override fun print(builder: StringBuilder) {
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

    override fun printParseTree(builder: StringBuilder) {
        builder.append(i)
    }

    override fun print(builder: StringBuilder) {
        builder.append(i)
    }

    override fun eval(): Int {
        return i
    }
}