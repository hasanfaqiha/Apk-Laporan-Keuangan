package com.example

import com.example.data.Budget
import com.example.data.RecurringRule
import com.example.data.Transaction
import com.example.data.computeBudgetUsages
import com.example.data.computeRecurringOccurrences
import com.example.data.monthExpenseTotalsByCategory
import com.example.data.nextRecurringRunMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringAndBudgetTest {

  private fun millis(year: Int, month: Int, day: Int, hour: Int = 10, minute: Int = 0): Long =
    Calendar.getInstance().apply {
      clear()
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

  private fun dayOfMonth(dateMillis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = dateMillis }.get(Calendar.DAY_OF_MONTH)

  private fun dailyRule(start: Long) =
    RecurringRule(
      title = "Internet",
      amount = 350_000.0,
      type = "EXPENSE",
      accountType = "BANK",
      category = "Sewa & Tagihan",
      frequency = "DAILY",
      startDateMillis = start,
      nextRunMillis = start
    )

  @Test
  fun recurring_daily_generatesEveryDueDayUntilNow() {
    val start = millis(2024, Calendar.JANUARY, 1)
    val now = millis(2024, Calendar.JANUARY, 4, hour = 23)
    val occurrences = computeRecurringOccurrences(listOf(dailyRule(start)), now)

    assertEquals(4, occurrences.size)
    // Strictly increasing schedule, all on or before `now`.
    occurrences.zipWithNext().forEach { (a, b) -> assertTrue(a.dateMillis < b.dateMillis) }
    assertTrue(occurrences.all { it.dateMillis <= now })
  }

  @Test
  fun recurring_monthly_anchorsOnStartDayAndClampsShortMonths() {
    // Started on the 31st: leap-year Feb gets the 29th, never drifts later.
    val start = millis(2024, Calendar.JANUARY, 31)
    val now = millis(2024, Calendar.MAY, 1)
    val rule = RecurringRule(
      title = "Cicilan",
      amount = 1_000_000.0,
      type = "EXPENSE",
      accountType = "BANK",
      category = "Lain-lain",
      frequency = "MONTHLY",
      startDateMillis = start,
      nextRunMillis = start
    )
    val occurrences = computeRecurringOccurrences(listOf(rule), now)

    assertEquals(listOf(31, 29, 31, 30), occurrences.map { dayOfMonth(it.dateMillis) })
  }

  @Test
  fun recurring_nextRunAdvancesMonthAndClampsToMonthEnd() {
    val start = millis(2025, Calendar.JANUARY, 31)
    val febRun = millis(2025, Calendar.FEBRUARY, 28)
    val rule = RecurringRule(
      title = "Cicilan",
      amount = 1_000_000.0,
      type = "EXPENSE",
      accountType = "BANK",
      category = "Lain-lain",
      frequency = "MONTHLY",
      startDateMillis = start,
      nextRunMillis = start
    )

    // Feb 28th -> March 31st (back to the anchored day, not March 28th).
    val next = nextRecurringRunMillis(rule, febRun)
    assertEquals(millis(2025, Calendar.MARCH, 31), next)
  }

  @Test
  fun recurring_respectsMaxPerRuleAndSkipsInactiveRules() {
    val start = millis(2023, Calendar.JANUARY, 1)
    val now = millis(2025, Calendar.JANUARY, 1) // far in the future
    val rule = dailyRule(start)
    val disabled = rule.copy(isActive = false)

    val capped = computeRecurringOccurrences(listOf(rule), now, maxPerRule = 6)
    assertEquals(6, capped.size)

    val withInactive = computeRecurringOccurrences(listOf(rule, disabled), now, maxPerRule = 10)
    assertEquals(10, withInactive.size) // inactive rule contributed nothing
  }

  @Test
  fun recurring_skipsNonPositiveAmounts() {
    val start = millis(2024, Calendar.JANUARY, 1)
    val now = millis(2024, Calendar.FEBRUARY, 1)
    val zeroAmount = dailyRule(start).copy(amount = 0.0)
    assertEquals(0, computeRecurringOccurrences(listOf(zeroAmount), now).size)
  }

  private fun tx(id: Int, category: String, amount: Double, dateMillis: Long, type: String = "EXPENSE") =
    Transaction(
      id = id,
      title = "Tx $id",
      amount = amount,
      type = type,
      accountType = "CASH",
      category = category,
      dateMillis = dateMillis
    )

  @Test
  fun budgets_flagsOnlyCurrentMonthExpenseTotals() {
    val febExpense1 = tx(1, "Makanan", 45_000.0, millis(2024, Calendar.FEBRUARY, 5))
    val febExpense2 = tx(2, "Makanan", 10_000.0, millis(2024, Calendar.FEBRUARY, 20))
    val janExpense = tx(3, "Makanan", 1_000_000.0, millis(2024, Calendar.JANUARY, 31))
    val marExpense = tx(4, "Makanan", 999_999.0, millis(2024, Calendar.MARCH, 1))
    val febIncome = tx(5, "Gaji", 5_000_000.0, millis(2024, Calendar.FEBRUARY, 10), type = "INCOME")

    val budgets = listOf(
      Budget(id = 1, category = "Makanan", monthlyLimit = 50_000.0),   // exceeded: 55k
      Budget(id = 2, category = "Transportasi", monthlyLimit = 9_999.0) // untouched: ok
    )

    val usages = computeBudgetUsages(
      transactions = listOf(febExpense1, febExpense2, janExpense, marExpense, febIncome),
      budgets = budgets,
      year = 2024,
      month = Calendar.FEBRUARY
    )

    assertEquals(2, usages.size)
    val makanan = usages.first { it.budget.category == "Makanan" }
    val transport = usages.first { it.budget.category == "Transportasi" }
    assertEquals(55_000.0, makanan.spent, 0.001)
    assertTrue(makanan.exceeded)
    assertEquals(0.0, transport.spent, 0.001)
    assertFalse(transport.exceeded)
  }

  @Test
  fun monthTotals_groupOnlyByCategoryAndIgnoreOtherMonths() {
    val totals = monthExpenseTotalsByCategory(
      transactions = listOf(
        tx(1, "Makanan", 20_000.0, millis(2024, Calendar.FEBRUARY, 2)),
        tx(2, "Makanan", 5_000.0, millis(2024, Calendar.FEBRUARY, 28)),
        tx(3, "Belanja", 100_000.0, millis(2024, Calendar.FEBRUARY, 15)),
        tx(4, "Makanan", 50_000.0, millis(2024, Calendar.MARCH, 1)), // outside month
        tx(5, "Gaji", 9_999.0, millis(2024, Calendar.FEBRUARY, 1), type = "INCOME") // income
      ),
      year = 2024,
      month = Calendar.FEBRUARY
    )

    assertEquals(25_000.0, totals["Makanan"] ?: 0.0, 0.001)
    assertEquals(100_000.0, totals["Belanja"] ?: 0.0, 0.001)
    assertEquals(2, totals.size)
  }
}
