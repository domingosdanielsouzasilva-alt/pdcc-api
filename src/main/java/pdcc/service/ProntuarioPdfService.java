package pdcc.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import pdcc.model.Cliente;
import pdcc.model.Servico;
import pdcc.model.Usuario;
import pdcc.model.Veiculo;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ProntuarioPdfService {

    private static final PDRectangle PAGINA = PDRectangle.A4;
    private static final float MARGEM = 36f;
    private static final float MARGEM_INFERIOR = 54f;
    private static final float LARGURA =
            PAGINA.getWidth() - (MARGEM * 2);

    private static final Color AZUL = new Color(23, 58, 77);
    private static final Color AZUL_SUAVE = new Color(235, 242, 245);
    private static final Color AMARELO = new Color(242, 183, 5);
    private static final Color GRAFITE = new Color(51, 56, 60);
    private static final Color CINZA = new Color(108, 117, 122);
    private static final Color CINZA_CLARO = new Color(246, 248, 249);
    private static final Color BORDA = new Color(211, 218, 222);
    private static final Color VERDE = new Color(46, 125, 91);
    private static final Color VERMELHO = new Color(183, 71, 58);
    private static final Color BRANCO = Color.WHITE;

    private final PDFont normal =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private final PDFont negrito =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] gerar(
            Veiculo veiculo,
            List<Servico> servicos
    ) throws IOException {
        try (
                PDDocument documento = new PDDocument();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()
        ) {
            PaginaAtual pagina =
                    novaPagina(documento, veiculo);

            pagina = desenharIdentificacao(
                    documento,
                    pagina,
                    veiculo
            );

            pagina = desenharTituloHistorico(
                    documento,
                    pagina,
                    veiculo
            );

            List<Servico> historico =
                    new ArrayList<>(servicos);

            historico.sort(
                    Comparator.comparing(
                            Servico::getDataServico,
                            Comparator.nullsLast(
                                    Comparator.reverseOrder()
                            )
                    )
            );

            if (historico.isEmpty()) {
                pagina = desenharSemServicos(
                        documento,
                        pagina,
                        veiculo
                );
            } else {
                for (int i = 0; i < historico.size(); i++) {
                    pagina = desenharServico(
                            documento,
                            pagina,
                            historico.get(i),
                            i + 1,
                            veiculo
                    );
                }
            }

            pagina.conteudo.close();

            adicionarRodapes(documento);

            documento.save(saida);

            return saida.toByteArray();
        }
    }

    private PaginaAtual novaPagina(
            PDDocument documento,
            Veiculo veiculo
    ) throws IOException {
        PDPage paginaPdf =
                new PDPage(PAGINA);

        documento.addPage(paginaPdf);

        PDPageContentStream conteudo =
                new PDPageContentStream(
                        documento,
                        paginaPdf
                );

        PaginaAtual pagina =
                new PaginaAtual();

        pagina.pagina = paginaPdf;
        pagina.conteudo = conteudo;
        pagina.y = PAGINA.getHeight() - MARGEM;

        desenharCabecalho(
                documento,
                pagina,
                veiculo
        );

        return pagina;
    }

    private void desenharCabecalho(
            PDDocument documento,
            PaginaAtual pagina,
            Veiculo veiculo
    ) throws IOException {
        float altura = 92f;
        float base = pagina.y - altura;

        desenharRetanguloArredondado(
                pagina.conteudo,
                MARGEM,
                base,
                LARGURA,
                altura,
                10,
                BRANCO,
                BORDA
        );

        PDImageXObject logo =
                carregarLogo(documento);

        if (logo != null) {
            float maxLargura = 270f;
            float maxAltura = 64f;

            float proporcao =
                    (float) logo.getWidth()
                            / logo.getHeight();

            float larguraLogo =
                    maxLargura;

            float alturaLogo =
                    larguraLogo / proporcao;

            if (alturaLogo > maxAltura) {
                alturaLogo = maxAltura;
                larguraLogo =
                        alturaLogo * proporcao;
            }

            pagina.conteudo.drawImage(
                    logo,
                    MARGEM + 14,
                    base
                            + ((altura
                            - alturaLogo) / 2),
                    larguraLogo,
                    alturaLogo
            );
        } else {
            escrever(
                    pagina.conteudo,
                    negrito,
                    20,
                    AZUL,
                    MARGEM + 18,
                    pagina.y - 40,
                    "PDCC"
            );
        }

        float xDireita =
                MARGEM + LARGURA - 18;

        escreverAlinhadoDireita(
                pagina.conteudo,
                negrito,
                12,
                AZUL,
                xDireita,
                pagina.y - 29,
                "PRONTUÁRIO DO VEÍCULO"
        );

        escreverAlinhadoDireita(
                pagina.conteudo,
                normal,
                8.5f,
                CINZA,
                xDireita,
                pagina.y - 48,
                "Documento nº "
                        + formatarNumeroDocumento(
                        veiculo.getId()
                )
        );

        escreverAlinhadoDireita(
                pagina.conteudo,
                normal,
                8.5f,
                CINZA,
                xDireita,
                pagina.y - 63,
                "Emitido em "
                        + formatarDataHoraAtual()
        );

        pagina.conteudo.setNonStrokingColor(
                AMARELO
        );

        pagina.conteudo.addRect(
                MARGEM + 14,
                base + 8,
                LARGURA - 28,
                3
        );

        pagina.conteudo.fill();

        pagina.y = base - 18;
    }

    private PDImageXObject carregarLogo(
            PDDocument documento
    ) throws IOException {
        try (
                InputStream entrada =
                        getClass()
                                .getResourceAsStream(
                                        "/images/logo-pdcc.png"
                                )
        ) {
            if (entrada == null) {
                return null;
            }

            return PDImageXObject.createFromByteArray(
                    documento,
                    entrada.readAllBytes(),
                    "logo-pdcc"
            );
        }
    }

    private PaginaAtual desenharIdentificacao(
            PDDocument documento,
            PaginaAtual pagina,
            Veiculo veiculo
    ) throws IOException {
        pagina = garantirEspaco(
                documento,
                pagina,
                132,
                veiculo
        );

        float altura = 112f;
        float topo = pagina.y;
        float base = topo - altura;

        desenharRetanguloArredondado(
                pagina.conteudo,
                MARGEM,
                base,
                LARGURA,
                altura,
                10,
                CINZA_CLARO,
                BORDA
        );

        escrever(
                pagina.conteudo,
                negrito,
                8.5f,
                AZUL,
                MARGEM + 18,
                topo - 18,
                "IDENTIFICAÇÃO"
        );

        pagina.conteudo.setNonStrokingColor(
                AMARELO
        );

        pagina.conteudo.addRect(
                MARGEM + 18,
                topo - 25,
                48,
                3
        );

        pagina.conteudo.fill();

        float colunaCliente =
                MARGEM + 18;

        float divisor =
                MARGEM + (LARGURA * 0.46f);

        float colunaVeiculo =
                divisor + 20;

        pagina.conteudo.setStrokingColor(
                BORDA
        );

        pagina.conteudo.setLineWidth(
                0.7f
        );

        pagina.conteudo.moveTo(
                divisor,
                base + 18
        );

        pagina.conteudo.lineTo(
                divisor,
                topo - 35
        );

        pagina.conteudo.stroke();

        Cliente cliente =
                veiculo.getCliente();

        escrever(
                pagina.conteudo,
                negrito,
                8,
                CINZA,
                colunaCliente,
                topo - 45,
                "CLIENTE"
        );

        escrever(
                pagina.conteudo,
                negrito,
                11,
                GRAFITE,
                colunaCliente,
                topo - 63,
                cliente != null
                        ? valor(
                        cliente.getNome()
                )
                        : "Não informado"
        );

        escrever(
                pagina.conteudo,
                normal,
                8.5f,
                CINZA,
                colunaCliente,
                topo - 82,
                "Telefone: "
                        + (
                        cliente != null
                                ? formatarTelefone(
                                cliente.getTelefone()
                        )
                                : "Não informado"
                )
        );

        escrever(
                pagina.conteudo,
                negrito,
                8,
                CINZA,
                colunaVeiculo,
                topo - 45,
                "VEÍCULO"
        );

        float placaY =
                topo - 82;

        desenharEtiquetaPlaca(
                pagina.conteudo,
                colunaVeiculo,
                placaY,
                valor(
                        veiculo.getPlaca()
                )
        );

        float xDadosVeiculo =
                colunaVeiculo + 96;

        escrever(
                pagina.conteudo,
                negrito,
                10,
                GRAFITE,
                xDadosVeiculo,
                topo - 62,
                valor(
                        veiculo.getModelo()
                )
        );

        escrever(
                pagina.conteudo,
                normal,
                8.5f,
                CINZA,
                xDadosVeiculo,
                topo - 80,
                "Ano: "
                        + valor(
                        veiculo.getAno()
                )
                        + "   •   Cor: "
                        + valor(
                        veiculo.getCor()
                )
        );

        pagina.y =
                base - 20;

        return pagina;
    }

    private void desenharEtiquetaPlaca(
            PDPageContentStream conteudo,
            float x,
            float y,
            String placa
    ) throws IOException {
        float largura = 78;
        float altura = 30;

        desenharRetanguloArredondado(
                conteudo,
                x,
                y,
                largura,
                altura,
                6,
                AZUL,
                AZUL
        );

        conteudo.setNonStrokingColor(
                AMARELO
        );

        conteudo.addRect(
                x,
                y + altura - 4,
                largura,
                4
        );

        conteudo.fill();

        escreverCentralizado(
                conteudo,
                negrito,
                11,
                BRANCO,
                x,
                y + 9,
                largura,
                placa
        );
    }

    private PaginaAtual desenharTituloHistorico(
            PDDocument documento,
            PaginaAtual pagina,
            Veiculo veiculo
    ) throws IOException {
        pagina = garantirEspaco(
                documento,
                pagina,
                48,
                veiculo
        );

        escrever(
                pagina.conteudo,
                negrito,
                14,
                AZUL,
                MARGEM,
                pagina.y,
                "HISTÓRICO DE SERVIÇOS"
        );

        pagina.y -= 10;

        pagina.conteudo.setNonStrokingColor(
                AMARELO
        );

        pagina.conteudo.addRect(
                MARGEM,
                pagina.y - 3,
                76,
                3
        );

        pagina.conteudo.fill();

        pagina.conteudo.setStrokingColor(
                BORDA
        );

        pagina.conteudo.setLineWidth(
                0.7f
        );

        pagina.conteudo.moveTo(
                MARGEM + 84,
                pagina.y - 1.5f
        );

        pagina.conteudo.lineTo(
                MARGEM + LARGURA,
                pagina.y - 1.5f
        );

        pagina.conteudo.stroke();

        pagina.y -= 24;

        return pagina;
    }

    private PaginaAtual desenharServico(
            PDDocument documento,
            PaginaAtual pagina,
            Servico servico,
            int numero,
            Veiculo veiculo
    ) throws IOException {
        float larguraTexto =
                LARGURA - 42;

        List<String> diagnostico =
                quebrarTexto(
                        valor(
                                servico.getDiagnostico()
                        ),
                        normal,
                        9.5f,
                        larguraTexto
                );

        List<String> executado =
                quebrarTexto(
                        valor(
                                servico.getServicoExecutado()
                        ),
                        normal,
                        9.5f,
                        larguraTexto
                );

        List<String> pecas =
                quebrarTexto(
                        valor(
                                servico.getPecasTrocadas()
                        ),
                        normal,
                        9,
                        225
                );

        float alturaCabecalho = 48;
        float alturaResponsavel = 42;

        float alturaDiagnostico =
                31
                        + (diagnostico.size() * 12);

        float alturaExecutado =
                31
                        + (executado.size() * 12);

        float alturaInferior =
                Math.max(
                        100,
                        48
                                + (pecas.size() * 12)
                );

        float alturaTotal =
                alturaCabecalho
                        + alturaResponsavel
                        + alturaDiagnostico
                        + alturaExecutado
                        + alturaInferior
                        + 8;

        pagina = garantirEspaco(
                documento,
                pagina,
                alturaTotal + 18,
                veiculo
        );

        float topo =
                pagina.y;

        float base =
                topo - alturaTotal;

        desenharRetanguloArredondado(
                pagina.conteudo,
                MARGEM,
                base,
                LARGURA,
                alturaTotal,
                11,
                BRANCO,
                BORDA
        );

        pagina.conteudo.setNonStrokingColor(
                AMARELO
        );

        pagina.conteudo.addRect(
                MARGEM,
                base + 11,
                5,
                alturaTotal - 22
        );

        pagina.conteudo.fill();

        float yCabecalho =
                topo - alturaCabecalho;

        desenharFaixaSuperiorServico(
                pagina.conteudo,
                topo,
                alturaCabecalho,
                numero,
                servico
        );

        Usuario responsavel =
                servico.getResponsavel();

        String nomeResponsavel =
                responsavel != null
                        ? valor(
                        responsavel.getNome()
                )
                        : "Não informado";

        String perfil =
                responsavel != null
                        ? formatarPerfil(
                        responsavel.getRole()
                )
                        : "Não informado";

        pagina.conteudo.setNonStrokingColor(
                CINZA_CLARO
        );

        pagina.conteudo.addRect(
                MARGEM + 12,
                yCabecalho
                        - alturaResponsavel,
                LARGURA - 24,
                alturaResponsavel
        );

        pagina.conteudo.fill();

        escrever(
                pagina.conteudo,
                negrito,
                7.5f,
                CINZA,
                MARGEM + 22,
                yCabecalho - 15,
                "RESPONSÁVEL PELO SERVIÇO"
        );

        escrever(
                pagina.conteudo,
                negrito,
                10,
                GRAFITE,
                MARGEM + 22,
                yCabecalho - 31,
                nomeResponsavel
                        + "  •  "
                        + perfil
        );

        float yAtual =
                yCabecalho
                        - alturaResponsavel;

        yAtual = desenharSecaoTexto(
                pagina.conteudo,
                yAtual,
                "DIAGNÓSTICO",
                diagnostico,
                alturaDiagnostico
        );

        yAtual = desenharSecaoTexto(
                pagina.conteudo,
                yAtual,
                "SERVIÇO EXECUTADO",
                executado,
                alturaExecutado
        );

        desenharResumoServico(
                pagina.conteudo,
                yAtual,
                alturaInferior,
                servico,
                pecas
        );

        pagina.y =
                base - 18;

        return pagina;
    }

    private void desenharFaixaSuperiorServico(
            PDPageContentStream conteudo,
            float topo,
            float altura,
            int numero,
            Servico servico
    ) throws IOException {
        float x =
                MARGEM + 12;

        float largura =
                LARGURA - 24;

        desenharRetanguloArredondado(
                conteudo,
                x,
                topo - altura,
                largura,
                altura,
                8,
                AZUL,
                AZUL
        );

        escrever(
                conteudo,
                negrito,
                18,
                AMARELO,
                x + 14,
                topo - 30,
                String.format(
                        "%02d",
                        numero
                )
        );

        paginaLinhaVertical(
                conteudo,
                x + 56,
                topo - 36,
                topo - 11
        );

        escrever(
                conteudo,
                negrito,
                10,
                BRANCO,
                x + 72,
                topo - 20,
                "SERVIÇO"
        );

        escrever(
                conteudo,
                normal,
                8.5f,
                BRANCO,
                x + 72,
                topo - 35,
                formatarData(
                        servico.getDataServico()
                )
        );
    }

    private float desenharSecaoTexto(
            PDPageContentStream conteudo,
            float topo,
            String titulo,
            List<String> linhas,
            float altura
    ) throws IOException {
        conteudo.setStrokingColor(
                BORDA
        );

        conteudo.setLineWidth(
                0.5f
        );

        conteudo.moveTo(
                MARGEM + 22,
                topo
        );

        conteudo.lineTo(
                MARGEM + LARGURA - 18,
                topo
        );

        conteudo.stroke();

        escrever(
                conteudo,
                negrito,
                8,
                AZUL,
                MARGEM + 22,
                topo - 18,
                titulo
        );

        float y =
                topo - 35;

        for (String linha : linhas) {
            escrever(
                    conteudo,
                    normal,
                    9.5f,
                    GRAFITE,
                    MARGEM + 22,
                    y,
                    linha
            );

            y -= 12;
        }

        return topo - altura;
    }

    private void desenharResumoServico(
            PDPageContentStream conteudo,
            float topo,
            float altura,
            Servico servico,
            List<String> pecas
    ) throws IOException {
        float x =
                MARGEM + 22;

        float larguraTotal =
                LARGURA - 44;

        float espaco =
                12;

        float larguraPecas =
                larguraTotal * 0.53f;

        float larguraFinanceiro =
                larguraTotal
                        - larguraPecas
                        - espaco;

        float base =
                topo - altura;

        desenharRetanguloArredondado(
                conteudo,
                x,
                base + 10,
                larguraPecas,
                altura - 20,
                7,
                CINZA_CLARO,
                BORDA
        );

        float xFinanceiro =
                x
                        + larguraPecas
                        + espaco;

        desenharRetanguloArredondado(
                conteudo,
                xFinanceiro,
                base + 10,
                larguraFinanceiro,
                altura - 20,
                7,
                AZUL_SUAVE,
                BORDA
        );

        escrever(
                conteudo,
                negrito,
                7.8f,
                AZUL,
                x + 10,
                topo - 22,
                "PEÇAS UTILIZADAS / TROCADAS"
        );

        float yPecas =
                topo - 41;

        for (String linha : pecas) {
            escrever(
                    conteudo,
                    normal,
                    9,
                    GRAFITE,
                    x + 10,
                    yPecas,
                    linha
            );

            yPecas -= 12;
        }

        escrever(
                conteudo,
                negrito,
                7.8f,
                AZUL,
                xFinanceiro + 10,
                topo - 22,
                "VALOR E GARANTIA"
        );

        escrever(
                conteudo,
                negrito,
                12,
                GRAFITE,
                xFinanceiro + 10,
                topo - 45,
                formatarValor(
                        servico.getValorServico()
                )
        );

        float divisorGarantia =
                xFinanceiro
                        + (larguraFinanceiro
                        * 0.55f);

        paginaLinhaVertical(
                conteudo,
                divisorGarantia,
                base + 22,
                topo - 48
        );

        desenharStatusGarantia(
                conteudo,
                xFinanceiro + 10,
                base + 22,
                divisorGarantia
                        - xFinanceiro
                        - 20,
                servico
        );

        escrever(
                conteudo,
                normal,
                8,
                CINZA,
                divisorGarantia + 12,
                base + 49,
                "Garantia até:"
        );

        escrever(
                conteudo,
                negrito,
                9.5f,
                GRAFITE,
                divisorGarantia + 12,
                base + 32,
                formatarData(
                        servico.getGarantiaFim()
                )
        );
    }

    private void desenharStatusGarantia(
            PDPageContentStream conteudo,
            float x,
            float y,
            float larguraDisponivel,
            Servico servico
    ) throws IOException {
        String status =
                obterStatusGarantia(
                        servico
                );

        String texto =
                formatarStatus(
                        status
                );

        Color cor =
                corStatus(
                        status
                );

        float tamanho =
                7.5f;

        float larguraTexto =
                negrito
                        .getStringWidth(texto)
                        / 1000f
                        * tamanho;

        float largura =
                Math.min(
                        larguraDisponivel,
                        larguraTexto + 20
                );

        desenharRetanguloArredondado(
                conteudo,
                x,
                y,
                largura,
                20,
                6,
                cor,
                cor
        );

        escreverCentralizado(
                conteudo,
                negrito,
                tamanho,
                BRANCO,
                x,
                y + 6.5f,
                largura,
                texto
        );
    }

    private PaginaAtual desenharSemServicos(
            PDDocument documento,
            PaginaAtual pagina,
            Veiculo veiculo
    ) throws IOException {
        pagina = garantirEspaco(
                documento,
                pagina,
                90,
                veiculo
        );

        float altura =
                70;

        desenharRetanguloArredondado(
                pagina.conteudo,
                MARGEM,
                pagina.y - altura,
                LARGURA,
                altura,
                10,
                CINZA_CLARO,
                BORDA
        );

        escreverCentralizado(
                pagina.conteudo,
                negrito,
                10,
                AZUL,
                MARGEM,
                pagina.y - 30,
                LARGURA,
                "NENHUM SERVIÇO REGISTRADO"
        );

        escreverCentralizado(
                pagina.conteudo,
                normal,
                9,
                CINZA,
                MARGEM,
                pagina.y - 48,
                LARGURA,
                "Este veículo ainda não possui histórico de serviços."
        );

        pagina.y -=
                altura + 14;

        return pagina;
    }

    private PaginaAtual garantirEspaco(
            PDDocument documento,
            PaginaAtual pagina,
            float altura,
            Veiculo veiculo
    ) throws IOException {
        if (
                pagina.y - altura
                        >= MARGEM_INFERIOR
        ) {
            return pagina;
        }

        pagina.conteudo.close();

        PaginaAtual nova =
                novaPagina(
                        documento,
                        veiculo
                );

        return desenharTituloHistorico(
                documento,
                nova,
                veiculo
        );
    }

    private void desenharRetanguloArredondado(
            PDPageContentStream conteudo,
            float x,
            float y,
            float largura,
            float altura,
            float raio,
            Color preenchimento,
            Color borda
    ) throws IOException {
        caminhoArredondado(
                conteudo,
                x,
                y,
                largura,
                altura,
                raio
        );

        conteudo.setNonStrokingColor(
                preenchimento
        );

        conteudo.fill();

        caminhoArredondado(
                conteudo,
                x,
                y,
                largura,
                altura,
                raio
        );

        conteudo.setStrokingColor(
                borda
        );

        conteudo.setLineWidth(
                0.7f
        );

        conteudo.stroke();
    }

    private void caminhoArredondado(
            PDPageContentStream conteudo,
            float x,
            float y,
            float largura,
            float altura,
            float raio
    ) throws IOException {
        float k =
                0.55228475f;

        float c =
                raio * k;

        conteudo.moveTo(
                x + raio,
                y
        );

        conteudo.lineTo(
                x + largura - raio,
                y
        );

        conteudo.curveTo(
                x + largura - raio + c,
                y,
                x + largura,
                y + raio - c,
                x + largura,
                y + raio
        );

        conteudo.lineTo(
                x + largura,
                y + altura - raio
        );

        conteudo.curveTo(
                x + largura,
                y + altura - raio + c,
                x + largura - raio + c,
                y + altura,
                x + largura - raio,
                y + altura
        );

        conteudo.lineTo(
                x + raio,
                y + altura
        );

        conteudo.curveTo(
                x + raio - c,
                y + altura,
                x,
                y + altura - raio + c,
                x,
                y + altura - raio
        );

        conteudo.lineTo(
                x,
                y + raio
        );

        conteudo.curveTo(
                x,
                y + raio - c,
                x + raio - c,
                y,
                x + raio,
                y
        );

        conteudo.closePath();
    }

    private void paginaLinhaVertical(
            PDPageContentStream conteudo,
            float x,
            float yInicio,
            float yFim
    ) throws IOException {
        conteudo.setStrokingColor(
                BORDA
        );

        conteudo.setLineWidth(
                0.7f
        );

        conteudo.moveTo(
                x,
                yInicio
        );

        conteudo.lineTo(
                x,
                yFim
        );

        conteudo.stroke();
    }

    private void adicionarRodapes(
            PDDocument documento
    ) throws IOException {
        int total =
                documento.getNumberOfPages();

        String emissao =
                formatarDataHoraAtual();

        for (int i = 0; i < total; i++) {
            PDPage pagina =
                    documento.getPage(i);

            try (
                    PDPageContentStream conteudo =
                            new PDPageContentStream(
                                    documento,
                                    pagina,
                                    PDPageContentStream.AppendMode.APPEND,
                                    true,
                                    true
                            )
            ) {
                conteudo.setNonStrokingColor(
                        AZUL
                );

                conteudo.addRect(
                        MARGEM,
                        39,
                        LARGURA,
                        2
                );

                conteudo.fill();

                escrever(
                        conteudo,
                        normal,
                        7.3f,
                        CINZA,
                        MARGEM,
                        24,
                        "PDCC  •  Prontuário Digital Curto-Circuito  •  "
                                + emissao
                );

                escreverAlinhadoDireita(
                        conteudo,
                        negrito,
                        7.3f,
                        AZUL,
                        PAGINA.getWidth()
                                - MARGEM,
                        24,
                        "Página "
                                + (i + 1)
                                + " de "
                                + total
                );
            }
        }
    }

    private List<String> quebrarTexto(
            String texto,
            PDFont fonte,
            float tamanho,
            float larguraMaxima
    ) throws IOException {
        List<String> linhas =
                new ArrayList<>();

        if (
                texto == null
                        || texto.isBlank()
        ) {
            linhas.add(
                    "Não informado"
            );

            return linhas;
        }

        String[] paragrafos =
                texto
                        .replace(
                                "\r",
                                ""
                        )
                        .split("\n");

        for (String paragrafo : paragrafos) {
            if (paragrafo.isBlank()) {
                linhas.add("");
                continue;
            }

            String[] palavras =
                    paragrafo
                            .trim()
                            .split("\\s+");

            StringBuilder atual =
                    new StringBuilder();

            for (String palavra : palavras) {
                String tentativa =
                        atual.isEmpty()
                                ? palavra
                                : atual
                                + " "
                                + palavra;

                float largura =
                        fonte
                                .getStringWidth(
                                        tentativa
                                )
                                / 1000f
                                * tamanho;

                if (
                        largura
                                <= larguraMaxima
                ) {
                    atual =
                            new StringBuilder(
                                    tentativa
                            );
                } else {
                    if (!atual.isEmpty()) {
                        linhas.add(
                                atual.toString()
                        );
                    }

                    atual =
                            new StringBuilder(
                                    palavra
                            );
                }
            }

            if (!atual.isEmpty()) {
                linhas.add(
                        atual.toString()
                );
            }
        }

        return linhas;
    }

    private void escrever(
            PDPageContentStream conteudo,
            PDFont fonte,
            float tamanho,
            Color cor,
            float x,
            float y,
            String texto
    ) throws IOException {
        conteudo.beginText();

        conteudo.setFont(
                fonte,
                tamanho
        );

        conteudo.setNonStrokingColor(
                cor
        );

        conteudo.newLineAtOffset(
                x,
                y
        );

        conteudo.showText(
                valor(texto)
        );

        conteudo.endText();
    }

    private void escreverCentralizado(
            PDPageContentStream conteudo,
            PDFont fonte,
            float tamanho,
            Color cor,
            float x,
            float y,
            float largura,
            String texto
    ) throws IOException {
        String valor =
                valor(texto);

        float larguraTexto =
                fonte
                        .getStringWidth(
                                valor
                        )
                        / 1000f
                        * tamanho;

        escrever(
                conteudo,
                fonte,
                tamanho,
                cor,
                x
                        + (
                        (
                                largura
                                        - larguraTexto
                        ) / 2
                ),
                y,
                valor
        );
    }

    private void escreverAlinhadoDireita(
            PDPageContentStream conteudo,
            PDFont fonte,
            float tamanho,
            Color cor,
            float xDireita,
            float y,
            String texto
    ) throws IOException {
        String valor =
                valor(texto);

        float larguraTexto =
                fonte
                        .getStringWidth(
                                valor
                        )
                        / 1000f
                        * tamanho;

        escrever(
                conteudo,
                fonte,
                tamanho,
                cor,
                xDireita - larguraTexto,
                y,
                valor
        );
    }

    private String formatarNumeroDocumento(
            Long id
    ) {
        if (id == null) {
            return "000000";
        }

        return String.format(
                "%06d",
                id
        );
    }

    private String formatarDataHoraAtual() {
        return LocalDateTime.now(
                ZoneId.of(
                        "America/Fortaleza"
                )
        ).format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm"
                )
        );
    }

    private String formatarData(
            LocalDate data
    ) {
        if (data == null) {
            return "Não informado";
        }

        return data.format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                )
        );
    }

    private String formatarTelefone(
            String telefone
    ) {
        if (
                telefone == null
                        || telefone.isBlank()
        ) {
            return "Não informado";
        }

        String numeros =
                telefone.replaceAll(
                        "\\D",
                        ""
                );

        if (numeros.length() == 11) {
            return String.format(
                    "(%s) %s-%s",
                    numeros.substring(
                            0,
                            2
                    ),
                    numeros.substring(
                            2,
                            7
                    ),
                    numeros.substring(7)
            );
        }

        if (numeros.length() == 10) {
            return String.format(
                    "(%s) %s-%s",
                    numeros.substring(
                            0,
                            2
                    ),
                    numeros.substring(
                            2,
                            6
                    ),
                    numeros.substring(6)
            );
        }

        return telefone;
    }

    private String formatarValor(
            BigDecimal valor
    ) {
        if (valor == null) {
            return "Não informado";
        }

        return NumberFormat
                .getCurrencyInstance(
                        Locale.forLanguageTag(
                                "pt-BR"
                        )
                )
                .format(valor);
    }

    private String obterStatusGarantia(
            Servico servico
    ) {
        if (
                servico.getGarantiaFim()
                        == null
        ) {
            return "SEM_GARANTIA";
        }

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of(
                                "America/Fortaleza"
                        )
                );

        if (
                servico
                        .getGarantiaFim()
                        .isBefore(hoje)
        ) {
            return "EXPIRADA";
        }

        return "EM_GARANTIA";
    }

    private Color corStatus(
            String status
    ) {
        return switch (status) {
            case "EM_GARANTIA" ->
                    VERDE;

            case "EXPIRADA" ->
                    VERMELHO;

            default ->
                    CINZA;
        };
    }

    private String formatarStatus(
            String status
    ) {
        return switch (status) {
            case "EM_GARANTIA" ->
                    "EM GARANTIA";

            case "EXPIRADA" ->
                    "GARANTIA EXPIRADA";

            default ->
                    "SEM GARANTIA";
        };
    }

    private String formatarPerfil(
            String role
    ) {
        if (
                role == null
                        || role.isBlank()
        ) {
            return "Não informado";
        }

        return switch (role) {
            case "ADMIN" ->
                    "Administrador";

            case "FUNCIONARIO" ->
                    "Funcionário";

            default ->
                    role;
        };
    }

    private String valor(
            String texto
    ) {
        if (
                texto == null
                        || texto.isBlank()
        ) {
            return "Não informado";
        }
        return texto;
    }

    private static class PaginaAtual {
        private PDPage pagina;
        private PDPageContentStream conteudo;
        private float y;
    }
}
