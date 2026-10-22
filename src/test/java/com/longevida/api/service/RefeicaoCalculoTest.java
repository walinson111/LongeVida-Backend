package com.longevida.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.longevida.api.entity.Alimento;
import com.longevida.api.entity.RefeicaoItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Cálculo nutricional proporcional à porção de referência. */
class RefeicaoCalculoTest {

    private static Alimento alimento(String nome, String porcao, String unidade, String kcal, String p, String c, String g) {
        Alimento a = new Alimento();
        a.setNome(nome);
        a.setPorcaoQuantidade(new BigDecimal(porcao));
        a.setPorcaoUnidade(unidade);
        a.setCalorias(new BigDecimal(kcal));
        a.setProteinas(new BigDecimal(p));
        a.setCarboidratos(new BigDecimal(c));
        a.setGorduras(new BigDecimal(g));
        return a;
    }

    @Test
    void arrozPor150gevalePorcaoEMeia() {
        Alimento arroz = alimento("Arroz Branco", "100", "g", "130", "2.7", "28.0", "0.3");

        RefeicaoItem item = RefeicaoService.calcularItem(arroz, new BigDecimal("150"));

        assertThat(item.getCalorias()).isEqualTo(195);
        assertThat(item.getProteinas()).isEqualByComparingTo("4.1");      // 4,05 arredondado
        assertThat(item.getCarboidratos()).isEqualByComparingTo("42.0");
        assertThat(item.getGorduras()).isEqualByComparingTo("0.5");       // 0,45 arredondado
        assertThat(item.getUnidade()).isEqualTo("g");
    }

    @Test
    void ovoContaPorUnidade() {
        Alimento ovo = alimento("Ovo Cozido", "1", "unidade", "78", "6.3", "0.6", "5.3");

        RefeicaoItem item = RefeicaoService.calcularItem(ovo, new BigDecimal("2"));

        assertThat(item.getCalorias()).isEqualTo(156);
        assertThat(item.getProteinas()).isEqualByComparingTo("12.6");
    }

    @Test
    void azeiteDe10mlUsaAPorcaoCorreta() {
        // No protótipo (frontend) este caso era calculado dividindo por 200 por engano.
        Alimento azeite = alimento("Azeite", "10", "ml", "88", "0.0", "0.0", "10.0");

        RefeicaoItem item = RefeicaoService.calcularItem(azeite, new BigDecimal("20"));

        assertThat(item.getCalorias()).isEqualTo(176);
        assertThat(item.getGorduras()).isEqualByComparingTo("20.0");
    }

    @Test
    void leiteDe200mlComQuantidadePequena() {
        Alimento leite = alimento("Leite Integral", "200", "ml", "122", "6.4", "9.0", "6.6");

        RefeicaoItem item = RefeicaoService.calcularItem(leite, new BigDecimal("100"));

        assertThat(item.getCalorias()).isEqualTo(61);
        assertThat(item.getProteinas()).isEqualByComparingTo("3.2");
    }
}
