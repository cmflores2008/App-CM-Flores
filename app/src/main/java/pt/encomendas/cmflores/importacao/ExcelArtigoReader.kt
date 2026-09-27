package pt.encomendas.cmflores.importacao

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

data class ArtigoExcel(
    val codigo: String,
    val descricao: String,
    val precoSemIva: Double,
    val precoComIva: Double,
    val categoria: String,
    val foto: String,
    val unidade: String
)

object ExcelArtigoReader {

    fun ler(inputStream: InputStream): List<ArtigoExcel> {

        val resultado = mutableListOf<ArtigoExcel>()

        XSSFWorkbook(inputStream).use { workbook ->

            val folha = workbook.getSheet("FLORES")
                ?: workbook.getSheetAt(0)

            val primeiraLinha = folha.getRow(0)
                ?: return emptyList()

            val colunas = mutableMapOf<String, Int>()

            for (celula in primeiraLinha) {

                val nome = celula
                    .stringCellValue
                    .trim()

                colunas[nome.lowercase()] = celula.columnIndex
            }

            val indiceCodigo = colunas["codigo"]
            val indiceDescricao = colunas["descrição"]
                ?: colunas["descricao"]

            val indiceSemIva = colunas["s/ iva"]
            val indiceComIva = colunas["c/ iva"]
            val indiceCategoria = colunas["categoria"]
            val indiceFoto = colunas["foto"]
            val indiceUnidade = colunas["unidade"]

            if (indiceCodigo == null || indiceDescricao == null) {
                return emptyList()
            }

            for (numeroLinha in 1..folha.lastRowNum) {

                val linha = folha.getRow(numeroLinha)
                    ?: continue

                val codigo = obterTexto(
                    linha,
                    indiceCodigo
                )

                val descricao = obterTexto(
                    linha,
                    indiceDescricao
                )

                if (codigo.isBlank()) {
                    continue
                }

                val artigo = ArtigoExcel(

                    codigo = codigo,

                    descricao = descricao,

                    precoSemIva = obterNumero(
                        linha,
                        indiceSemIva
                    ),

                    precoComIva = obterNumero(
                        linha,
                        indiceComIva
                    ),

                    categoria = obterTexto(
                        linha,
                        indiceCategoria
                    ),

                    foto = obterTexto(
                        linha,
                        indiceFoto
                    ),

                    unidade = obterTexto(
                        linha,
                        indiceUnidade
                    )
                )

                resultado.add(artigo)
            }
        }

        return resultado
    }

    private fun obterTexto(
        linha: org.apache.poi.ss.usermodel.Row,
        indice: Int?
    ): String {

        if (indice == null) {
            return ""
        }

        val celula = linha.getCell(indice)
            ?: return ""

        return when (celula.cellType) {

            org.apache.poi.ss.usermodel.CellType.STRING ->
                celula.stringCellValue.trim()

            org.apache.poi.ss.usermodel.CellType.NUMERIC ->
                celula.numericCellValue.toString()

            org.apache.poi.ss.usermodel.CellType.BOOLEAN ->
                celula.booleanCellValue.toString()

            else ->
                ""
        }
    }

    private fun obterNumero(
        linha: org.apache.poi.ss.usermodel.Row,
        indice: Int?
    ): Double {

        if (indice == null) {
            return 0.0
        }

        val celula = linha.getCell(indice)
            ?: return 0.0

        return when (celula.cellType) {

            org.apache.poi.ss.usermodel.CellType.NUMERIC ->
                celula.numericCellValue

            org.apache.poi.ss.usermodel.CellType.STRING ->
                celula.stringCellValue
                    .replace(",", ".")
                    .trim()
                    .toDoubleOrNull()
                    ?: 0.0

            else ->
                0.0
        }
    }
}
