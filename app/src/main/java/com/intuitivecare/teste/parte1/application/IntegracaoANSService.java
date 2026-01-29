package com.intuitivecare.teste.parte1.application;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte1.domain.DespesaOperadora;
import com.intuitivecare.teste.parte1.domain.DespesasAgregadas;
import com.intuitivecare.teste.parte1.domain.FlagValorSuspeito;
import com.intuitivecare.teste.parte1.domain.OperadoraCadastro;
import com.intuitivecare.teste.parte1.domain.StatusConsistencia;
import com.intuitivecare.teste.parte1.domain.TrimestreANS;
import com.intuitivecare.teste.parte1.infrastructure.ANSHttpClient;
import com.intuitivecare.teste.parte1.infrastructure.CSVDespesasParser;
import com.intuitivecare.teste.parte1.infrastructure.OperadoraCadastroParser;
import com.intuitivecare.teste.parte1.infrastructure.ZipFileHandler;

/**
 * Caso de uso: Integração com API da ANS - VERSÃO CORRIGIDA.
 * Agrega despesas por operadora e faz join com dados cadastrais.
 */
public class IntegracaoANSService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegracaoANSService.class);
    private static final String URL_CADASTRO = "https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/Relatorio_cadop.csv";
    
    private final ANSHttpClient httpClient;
    private final ZipFileHandler zipHandler;
    private final CSVDespesasParser csvParser;
    private final OperadoraCadastroParser cadastroParser;
    private final Path diretorioTrabalho;
    
    public IntegracaoANSService(Path diretorioTrabalho) {
        this.httpClient = new ANSHttpClient();
        this.zipHandler = new ZipFileHandler();
        this.csvParser = new CSVDespesasParser();
        this.cadastroParser = new OperadoraCadastroParser();
        this.diretorioTrabalho = diretorioTrabalho;
    }
    
    public void executarIntegracao(List<TrimestreANS> trimestres) throws IOException, InterruptedException {
        logger.info("Iniciando integração ANS para {} trimestres", trimestres.size());
        
        // 1. Baixar dados cadastrais
        Map<String, OperadoraCadastro> cadastro = baixarDadosCadastrais();
        
        // 2. Processar trimestres e agregar despesas
        Map<String, DespesasAgregadas> despesasPorOperadora = new HashMap<>();
        
        for (TrimestreANS trimestre : trimestres) {
            logger.info("Processando trimestre: {}", trimestre);
            try {
                List<DespesaOperadora> despesas = processarTrimestre(trimestre);
                agregarDespesas(despesas, despesasPorOperadora);
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao processar trimestre {}: {}", trimestre, e.getMessage(), e);
            }
        }
        
        // 3. Fazer join com cadastro e gerar CSV consolidado
        List<DespesaConsolidada> despesasConsolidadas = consolidarComCadastro(despesasPorOperadora, cadastro);
        
        // 4. Exportar
        Path csvConsolidado = diretorioTrabalho.resolve("consolidado_despesas.csv");
        exportarConsolidado(despesasConsolidadas, csvConsolidado);
        
        Path zipConsolidado = diretorioTrabalho.resolve("consolidado_despesas.zip");
        compactarArquivo(csvConsolidado, zipConsolidado);
        
        logger.info("Integração concluída! {} operadoras consolidadas", despesasConsolidadas.size());
        logger.info("Arquivo gerado: {}", zipConsolidado);
    }
    
    private Map<String, OperadoraCadastro> baixarDadosCadastrais() throws IOException, InterruptedException {
        logger.info("Baixando dados cadastrais de operadoras...");
        
        Path dirCadastro = diretorioTrabalho.resolve("cadastro");
        Files.createDirectories(dirCadastro);
        
        Path arquivoCadastro = httpClient.baixarArquivo(URL_CADASTRO, dirCadastro);
        return cadastroParser.carregarCadastro(arquivoCadastro);
    }
    
    private List<DespesaOperadora> processarTrimestre(TrimestreANS trimestre) 
            throws IOException, InterruptedException {
        
        List<DespesaOperadora> despesas = new ArrayList<>();
        
        // Lista arquivos ZIP disponíveis
        List<String> urlsZip = httpClient.listarArquivosZip(trimestre.ano(), trimestre.trimestre());
        
        if (urlsZip.isEmpty()) {
            logger.warn("Nenhum arquivo ZIP encontrado para trimestre: {}", trimestre);
            return despesas;
        }
        
        Path dirDownload = diretorioTrabalho.resolve("downloads").resolve(trimestre.toDisplayFormat());
        Files.createDirectories(dirDownload);
        
        for (String urlZip : urlsZip) {
            try {
                Path arquivoZip = httpClient.baixarArquivo(urlZip, dirDownload);
                Path dirExtracao = diretorioTrabalho.resolve("extraidos").resolve(trimestre.toDisplayFormat());
                List<Path> arquivosExtraidos = zipHandler.extrairZip(arquivoZip, dirExtracao);
                List<Path> arquivosDespesas = zipHandler.filtrarArquivosDespesas(arquivosExtraidos);
                
                for (Path arquivoCsv : arquivosDespesas) {
                    if (isArquivoProcessavel(arquivoCsv)) {
                        List<DespesaOperadora> despesasArquivo = csvParser.processarArquivo(
                            arquivoCsv, 
                            trimestre.ano(), 
                            trimestre.trimestre()
                        );
                        despesas.addAll(despesasArquivo);
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao processar arquivo ZIP {}: {}", urlZip, e.getMessage());
            }
        }
        
        return despesas;
    }
    
    private void agregarDespesas(
        List<DespesaOperadora> despesas, 
        Map<String, DespesasAgregadas> agregadas
    ) {
        for (DespesaOperadora despesa : despesas) {
            String chave = despesa.cnpj() + "_" + despesa.trimestre() + "_" + despesa.ano();
            
            DespesasAgregadas agregada = agregadas.computeIfAbsent(chave, k -> 
                new DespesasAgregadas(despesa.cnpj(), despesa.trimestre(), despesa.ano())
            );
            
            agregada.adicionarValor(despesa.valorDespesas());
        }
    }
    
    private List<DespesaConsolidada> consolidarComCadastro(
        Map<String, DespesasAgregadas> despesas,
        Map<String, OperadoraCadastro> cadastro
    ) {
        logger.info("Consolidando {} registros agregados com cadastro...", despesas.size());
        
        List<DespesaConsolidada> consolidadas = new ArrayList<>();
        int semCadastro = 0;
        int cnpjDuplicado = 0;
        
        for (DespesasAgregadas desp : despesas.values()) {
            OperadoraCadastro cad = cadastro.get(desp.getRegistroANS());
            
            String cnpj;
            String razaoSocial;
            StatusConsistencia status;
            
            if (cad == null || cad.cnpj() == null) {
                // Não encontrou no cadastro - usa REG_ANS como CNPJ temporário
                cnpj = desp.getRegistroANS();
                razaoSocial = "OPERADORA REG_ANS " + desp.getRegistroANS();
                status = StatusConsistencia.CONSISTENTE; // Será marcado depois se tiver duplicata
                semCadastro++;
            } else {
                cnpj = cad.cnpj();
                razaoSocial = cad.razaoSocial();
                status = StatusConsistencia.CONSISTENTE;
            }
            
            consolidadas.add(new DespesaConsolidada(
                cnpj,
                razaoSocial,
                desp.getTrimestre(),
                desp.getAno(),
                desp.getValorTotal(),
                status,
                desp.determinarFlag()
            ));
        }
        
        // Detecta CNPJs com razões sociais diferentes e operadoras sem movimentação
        Map<String, List<DespesaConsolidada>> porCnpj = consolidadas.stream()
            .collect(Collectors.groupingBy(DespesaConsolidada::cnpj));
        
        List<DespesaConsolidada> resultado = new ArrayList<>();
        int semMovimentacao = 0;
        
        for (Map.Entry<String, List<DespesaConsolidada>> entry : porCnpj.entrySet()) {
            List<DespesaConsolidada> despesasCnpj = entry.getValue();
            Set<String> razoesDistintas = despesasCnpj.stream()
                .map(DespesaConsolidada::razaoSocial)
                .collect(Collectors.toSet());
            
            // Verifica se TODOS os trimestres têm valor zero
            boolean todosTrimestresZero = despesasCnpj.stream()
                .allMatch(d -> d.valorDespesas().compareTo(BigDecimal.ZERO) == 0);
            
            if (razoesDistintas.size() > 1) {
                logger.warn("CNPJ {} possui {} razões sociais diferentes: {}", 
                    entry.getKey(), razoesDistintas.size(), razoesDistintas);
                cnpjDuplicado++;
                
                for (DespesaConsolidada d : despesasCnpj) {
                    resultado.add(d.marcarComoCnpjDuplicado());
                }
            } else if (todosTrimestresZero) {
                logger.warn("Operadora {} sem movimentação em todos os trimestres", entry.getKey());
                semMovimentacao++;
                
                for (DespesaConsolidada d : despesasCnpj) {
                    resultado.add(d.marcarComoSemMovimentacao());
                }
            } else {
                resultado.addAll(despesasCnpj);
            }
        }
        
        logger.info("Consolidação: {} sem cadastro, {} CNPJs duplicados, {} sem movimentação", 
            semCadastro, cnpjDuplicado, semMovimentacao);
        return resultado;
    }
    
    private void exportarConsolidado(List<DespesaConsolidada> despesas, Path destino) throws IOException {
        logger.info("Exportando {} despesas consolidadas para: {}", despesas.size(), destino);
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("CNPJ", "RazaoSocial", "Trimestre", "Ano", "ValorDespesas", 
                      "StatusConsistencia", "FlagValorSuspeito")
            .build();
        
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (DespesaConsolidada despesa : despesas) {
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
    
    private boolean isArquivoProcessavel(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.endsWith(".csv") || nome.endsWith(".txt");
    }
    
    private void compactarArquivo(Path arquivo, Path destino) throws IOException {
        logger.info("Compactando arquivo: {} -> {}", arquivo, destino);
        
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(destino))) {
            ZipEntry entry = new ZipEntry(arquivo.getFileName().toString());
            zos.putNextEntry(entry);
            Files.copy(arquivo, zos);
            zos.closeEntry();
        }
        
        logger.info("Arquivo compactado com sucesso: {}", destino);
    }
    
    /**
     * Record para despesa consolidada (após join com cadastro).
     */
    private record DespesaConsolidada(
        String cnpj,
        String razaoSocial,
        Integer trimestre,
        Integer ano,
        BigDecimal valorDespesas,
        StatusConsistencia statusConsistencia,
        FlagValorSuspeito flagValorSuspeito
    ) {
        public DespesaConsolidada marcarComoCnpjDuplicado() {
            return new DespesaConsolidada(
                cnpj, razaoSocial, trimestre, ano, valorDespesas,
                StatusConsistencia.CNPJ_DUPLICADO_RAZAO_DIVERGENTE,
                flagValorSuspeito
            );
        }
        
        public DespesaConsolidada marcarComoSemMovimentacao() {
            return new DespesaConsolidada(
                cnpj, razaoSocial, trimestre, ano, valorDespesas,
                StatusConsistencia.OPERADORA_SEM_MOVIMENTACAO,
                flagValorSuspeito
            );
        }
    }
}
