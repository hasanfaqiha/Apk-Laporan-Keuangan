package com.example

import com.example.data.Bill
import com.example.data.Transaction
import com.example.viewmodel.buildBillsCsv
import com.example.viewmodel.buildTransactionsCsv
import com.example.viewmodel.escapeCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCsvExportTest {

  @Test
  fun buildTransactionsCsv_includesHeaderAndSortedRows() {
    val tx = listOf(
      Transaction(
        id = 1,
        title = "Nasi Padang",
        amount = 25000.0,
        type = "EXPENSE",
        accountType = "CASH",
        category = "Makanan & Minuman",
        dateMillis = 1700000000000L,
        note = "Makan siang"
      ),
      Transaction(
        id = 2,
        title = "Gaji",
        amount = 5000000.0,
        type = "INCOME",
        accountType = "BANK",
        category = "Gaji",
        dateMillis = 1700000001000L,
        note = ""
      )
    )

    val csv = buildTransactionsCsv(tx)

    assertTrue(csv.startsWith("Tanggal,Tipe,Judul,Kategori,Metode Bayar,Jumlah,Catatan\n"))
    // Newest first: Gaji row appears before Nasi Padang row
    val gajiIdx = csv.indexOf("Gaji")
    val nasiIdx = csv.indexOf("Nasi Padang")
    assertTrue(gajiIdx < nasiIdx)
    assertTrue(csv.contains("Pemasukan"))
    assertTrue(csv.contains("Pengeluaran"))
    assertTrue(csv.contains("5000000"))
    assertTrue(csv.contains("25000"))
  }

  @Test
  fun buildBillsCsv_includesHeaderAndStatus() {
    val bills = listOf(
      Bill(
        id = 1,
        title = "Listrik PLN",
        amount = 350000.0,
        dueDateMillis = 1700000000000L,
        isPaid = false,
        category = "Listrik",
        note = ""
      )
    )

    val csv = buildBillsCsv(bills)

    assertTrue(csv.startsWith("Nama,Kategori,Jumlah,Jatuh Tempo,Status,Catatan\n"))
    assertTrue(csv.contains("Listrik PLN"))
    assertTrue(csv.contains("350000"))
    assertTrue(csv.contains("Belum Bayar"))
  }

  @Test
  fun escapeCsv_wrapsFieldsWithSpecialCharacters() {
    assertEquals("plain", escapeCsv("plain"))
    assertEquals("\"Makan, Sore\"", escapeCsv("Makan, Sore"))
    assertEquals("\"katanya \"\"halo\"\"\"", escapeCsv("katanya \"halo\""))
  }
}
