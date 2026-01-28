package com.intuitivecare.teste.parte2.application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte1.domain.OperadoraCadastro;
import com.intuitivecare.teste.parte1.infrastructure.OperadoraCadastroParser;
import com.intuitivecare.teste.parte2.domain.CNPJValidator;
import com.intuitivecare.teste.parte2.domain.DespesaEnriquecida;
import com.intuitivecare.teste.parte2.domain.DespesasAgregadasPorOperadora;
import com.intuitivecare.teste.parte2.domain.StatusValidacao;

/**
 * Serviço para transformação e validação de dados (Parte 2).
 */
public class TransformacaoValidacaoService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformacaoValidacaoService.class);
    
    private final Path diretorioTrabalho;
    private final OperadoraCadastroParser cadastroParser;
    
    public TransformacaoValidacaoService(Path diretorioTrabalho) {
        this.diretorioTrabalho = diretorioTrabalho;
        this.cadastroParser = new OperadoraCadastroParser();
    }
    
    /**
     * Executa processo completo de transformação e validação.
     */
    public void executarTransformacao(Path csvConsolidado, Path csvCadastro) throws IOException {
        logger.info("=== Iniciando Parte 2: Transformação e Validação ===");
        
        // Criar diretório de trabalho
        Files.createDirectories(diretorioTrabalho);
        
        // 1. Carregar cadastro
        Map<String, OperadoraCadastro> cadastro = cadastroParser.carregarCadastro(csvCadastro);
        logger.info("Cadastro carregado: {} operadoras", cadastro.size());
        
        // 2. Processar e validar dados consolidados
        List<DespesaEnriquecida> despesasEnriquecidas = processarEValidar(csvConsolidado, cadastro);
        logger.info("Processados {} registros", despesasEnriquecidas.size());
        
        // 3. Exportar despesas enriquecidas
        Path csvEnriquecido = diretorioTrabalho.resolve("despesas_enriquecidas.csv");
        exportarDespesasEnriquecidas(despesasEnriquecidas, csvEnriquecido);
        
        // 4. Agregar por operadora/UF
        List<DespesasAgregadasPorOperadora> agregadas = agregarPorOperadoraUF(despesasEnriquecidas);
        logger.info("Agregadas {} operadoras/UF", agregadas.size());
        
        // 5. Exportar agregações
        Path csvAgregado = diretorioTrabalho.resolve("despesas_agregadas.csv");
        exportarDespesasAgregadas(agregadas, csvAgregado);
        
        logger.info("=== Parte 2 Concluída ===");
    }
    
    private List<DespesaEnriquecida> processarEValidar(
        Path csvConsolidado,
        Map<String, OperadoraCadastro> cadastro
    ) throws IOException {
        
        logger.info("Processando e validando: {}", csvConsolidado);
        
        List<DespesaEnriquecida> resultado = new ArrayList<>();
        int validos = 0;
        int cnpjInvalido = 0;
        int semCadastro = 0;
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
        
        try (BufferedReader reader = Files.newBufferedReader(csvConsolidado, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            for (CSVRecord record : parser) {
                try {
                    String cnpj = record.get("CNPJ");
                    String razaoSocial = record.get("RazaoSocial");
                    Integer trimestre = Integer.valueOf(record.get("Trimestre"));
                    Integer ano = Integer.valueOf(record.get("Ano"));
                    BigDecimal valor = new BigDecimal(record.get("ValorDespesas"));
                    
                    // Buscar dados cadastrais
                    OperadoraCadastro cad = buscarCadastroPorCNPJ(cnpj, cadastro);
                    
                    String registroANS = null;
                    String modalidade = null;
                    String uf = null;
                    
                    if (cad != null) {
                        registroANS = cad.registroANS();
                        modalidade = cad.modalidade();
                        uf = cad.uf();
                    } else {
                        semCadastro++;
                    }
                    
                    DespesaEnriquecida despesa = DespesaEnriquecida.criar(
                        cnpj, razaoSocial, trimestre, ano, valor,
                        registroANS, modalidade, uf
                    );
                    
                    resultado.add(despesa);
                    
                    if (despesa.isValido()) {
                        validos++;
                    } else if (despesa.statusValidacao() == StatusValidacao.CNPJ_INVALIDO) {
                        cnpjInvalido++;
                    }
                    
                } catch (NumberFormatException e) {
                    logger.warn("Erro ao processar linha {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }
        }
        
        logger.info("Validação: {} válidos, {} CNPJ inválido, {} sem cadastro", 
            validos, cnpjInvalido, semCadastro);
        
        return resultado;
    }
    
    private OperadoraCadastro buscarCadastroPorCNPJ(String cnpj, Map<String, OperadoraCadastro> cadastro) {
        // Normaliza CNPJ
        String cnpjNormalizado = CNPJValidator.normalizar(cnpj);
        
        // Busca diretamente por CNPJ
        return cadastro.values().stream()
            .filter(c -> cnpjNormalizado.equals(CNPJValidator.normalizar(c.cnpj())))
            .findFirst()
            .orElse(null);
    }
    
    private List<DespesasAgregadasPorOperadora> agregarPorOperadoraUF(List<DespesaEnriquecida> despesas) {
        logger.info("Agregando despesas por Razão Social e UF...");
        
        // Filtra apenas despesas válidas para agregação
        Map<String, DespesasAgregadasPorOperadora> mapa = new HashMap<>();
        
        for (DespesaEnriquecida despesa : despesas) {
            // Só agrega se tiver UF (dados enriquecidos)
            if (despesa.uf() == null || despesa.uf().isBlank()) {
                continue;
            }
            
            String chave = despesa.razaoSocial() + "|" + despesa.uf();
            
            DespesasAgregadasPorOperadora agregada = mapa.computeIfAbsent(
                chave,
                k -> new DespesasAgregadasPorOperadora(despesa.razaoSocial(), despesa.uf())
            );
            
            agregada.adicionarValor(despesa.valorDespesas());
        }
        
        // Ordena por total (maior para menor)
        return mapa.values().stream()
            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
            .collect(Collectors.toList());
    }
    
    private void exportarDespesasEnriquecidas(List<DespesaEnriquecida> despesas, Path destino) throws IOException {
        logger.info("Exportando {} despesas enriquecidas para: {}", despesas.size(), destino);
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("CNPJ", "RazaoSocial", "Trimestre", "Ano", "ValorDespesas",
                      "RegistroANS", "Modalidade", "UF", "StatusValidacao", "MensagemValidacao")
            .build();
        
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (DespesaEnriquecida d : despesas) {
                printer.printRecord(
                    d.cnpj(),
                    d.razaoSocial(),
                    d.trimestre(),
                    d.ano(),
                    d.valorDespesas(),
                    d.registroANS(),
                    d.modalidade(),
                    d.uf(),
                    d.statusValidacao().name(),
                    d.mensagemValidacao()
                );
            }
        }
        
        logger.info("Exportação concluída: {}", destino);
    }
    
    private void exportarDespesasAgregadas(List<DespesasAgregadasPorOperadora> agregadas, Path destino) throws IOException {
        logger.info("Exportando {} agregações para: {}", agregadas.size(), destino);
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("RazaoSocial", "UF", "TotalDespesas", "MediaPorTrimestre", 
                      "DesvioPadrao", "QuantidadeTrimestres")
            .build();
        
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (DespesasAgregadasPorOperadora a : agregadas) {
                printer.printRecord(
                    a.getRazaoSocial(),
                    a.getUf(),
                    a.getTotal(),
                    a.getMedia(),
                    a.getDesvioPadrao(),
                    a.getQuantidadeTrimestres()
                );
            }
        }
        
        logger.info("Exportação concluída: {}", destino);
    }
}
