package com.megasena.sync.fonte;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaixaConcursoResponse {

    @JsonProperty("numero")
    private Integer numero;

    @JsonProperty("dataApuracao")
    private String dataApuracao;

    @JsonProperty("listaDezenas")
    private List<String> listaDezenas;

    @JsonProperty("listaRateioPremio")
    private List<RateioPremio> listaRateioPremio;

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getDataApuracao() { return dataApuracao; }
    public void setDataApuracao(String dataApuracao) { this.dataApuracao = dataApuracao; }
    public List<String> getListaDezenas() { return listaDezenas; }
    public void setListaDezenas(List<String> listaDezenas) { this.listaDezenas = listaDezenas; }
    public List<RateioPremio> getListaRateioPremio() { return listaRateioPremio; }
    public void setListaRateioPremio(List<RateioPremio> listaRateioPremio) { this.listaRateioPremio = listaRateioPremio; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RateioPremio {

        @JsonProperty("descricaoFaixa")
        private String descricaoFaixa;

        @JsonProperty("valorPremio")
        private BigDecimal valorPremio;

        public String getDescricaoFaixa() { return descricaoFaixa; }
        public void setDescricaoFaixa(String descricaoFaixa) { this.descricaoFaixa = descricaoFaixa; }
        public BigDecimal getValorPremio() { return valorPremio; }
        public void setValorPremio(BigDecimal valorPremio) { this.valorPremio = valorPremio; }
    }
}
