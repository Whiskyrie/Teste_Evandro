package com.intuitivecare.teste.parte1.infrastructure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte1.domain.DespesaOperadora;

/**
 * Parser de arquivos CSV com suporte a múltiplos formatos.
 * Implementa processamento streaming conforme decisão técnica.
 */
public class CSVDespesasParser {
    
    private static final Logger logger = LoggerFactory.getLogger(CSVDespesasParser.class);
    
    /**
     * Processa um arquivo CSV e retorna lista de despesas.
     * Usa streaming para eficiência de memória.
     */
    public List<DespesaOperadora> processarArquivo(Path arquivo, int ano, int trimestre) throws IOException {
        logger.info("Processando arquivo CSV: {}", arquivo);
        
        List<DespesaOperadora> despesas = new ArrayList<>();
        CSVFormat format = detectarFormato(arquivo);
        
        try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            Map<String, Integer> headers = parser.getHeaderMap();
            logger.debug("Headers detectados: {}", headers.keySet());
            
            for (CSVRecord record : parser) {
                try {
                    DespesaOperadora despesa = parsearRegistro(record, headers, ano, trimestre);
                    if (despesa != null) {
                        despesas.add(despesa);
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao processar linha {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }
        }
        
        logger.info("Processadas {} despesas do arquivo: {}", despesas.size(), arquivo.getFileName());
        return despesas;
    }
    
    /**
     * Exporta lista de despesas para CSV consolidado.
     */
    public void exportarParaCSV(List<DespesaOperadora> despesas, Path destino) throws IOException {
        logger.info("Exportando {} despesas para: {}", despesas.size(), destino);
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("CNPJ", "RazaoSocial", "Trimestre", "Ano", "ValorDespesas", 
                      "StatusConsistencia", "FlagValorSuspeito")
            .build();
        
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (DespesaOperadora despesa : despesas) {
                printer.printRecord(
                    despesa.cnpj(),
                    despesa.razaoSocial(),
                    despesa.trimestre(),
                    despesa.ano(),
                    despesa.valorDespesas(),
                    despesa.statusConsistencia().name(),
                    despesa.flagValorSuspeito().name()
                );
            }
        }
        
        logger.info("Exportação concluída: {}", destino);
    }
    
    private CSVFormat detectarFormato(Path arquivo) throws IOException {
        // Lê primeira linha para detectar delimitador
        try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            String primeiraLinha = reader.readLine();
            
            if (primeiraLinha == null) {
                throw new IOException("Arquivo vazio: " + arquivo);
            }
            
            // Detecta delimitador mais comum
            char delimitador = ';';
            if (primeiraLinha.contains(",") && !primeiraLinha.contains(";")) {
                delimitador = ',';
            } else if (primeiraLinha.contains("\t")) {
                delimitador = '\t';
            }
            
            return CSVFormat.DEFAULT.builder()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        }
    }
    
    private DespesaOperadora parsearRegistro(
        CSVRecord record, 
        Map<String, Integer> headers, 
        int ano, 
        int trimestre
    ) {
        // Tenta encontrar as colunas com nomes variados
        // Para demonstrações contábeis da ANS: REG_ANS, DESCRICAO, VL_SALDO_FINAL
        String regANS = buscarValorColuna(record, headers, "reg_ans", "registro_ans", "cnpj", "cd_cnpj");
        String descricao = buscarValorColuna(record, headers, "descricao", "razao_social", "nm_razao_social");
        String valorStr = buscarValorColuna(record, headers, "vl_saldo_final", "vl_saldo_inicial", "valor", "vl_despesa", "despesa");
        
        if (regANS == null || descricao == null || valorStr == null) {
            return null;
        }
        
        // Filtra apenas contas relacionadas a despesas com eventos/sinistros
        // Contas contábeis que começam com 411 ou 412 são de eventos/sinistros
        String contaContabil = buscarValorColuna(record, headers, "cd_conta_contabil", "conta");
        if (contaContabil != null && !contaContabil.startsWith("411") && !contaContabil.startsWith("412")) {
            return null; // Ignora contas que não são de eventos
        }
        
        // Usa REG_ANS como CNPJ temporário (teremos que buscar o CNPJ real depois)
        String cnpjTemp = regANS.replaceAll("[^0-9]", "");
        
        // Parseia valor
        BigDecimal valor;
        try {
            // Remove separadores de milhar e troca vírgula por ponto
            valorStr = valorStr.replaceAll("\\.", "").replace(",", ".");
            valor = new BigDecimal(valorStr);
        } catch (NumberFormatException e) {
            return null;
        }
        
        return DespesaOperadora.criar(cnpjTemp, descricao.trim(), trimestre, ano, valor);
    }
    
    private String buscarValorColuna(CSVRecord record, Map<String, Integer> headers, String... nomespossiveis) {
        for (String nome : nomespossiveis) {
            // Busca case-insensitive
            for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().contains(nome.toLowerCase())) {
                    String valor = record.get(entry.getValue());
                    if (valor != null && !valor.trim().isEmpty()) {
                        return valor;
                    }
                }
            }
        }
        return null;
    }
}
