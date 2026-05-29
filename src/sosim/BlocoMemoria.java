package sosim;

// representa um bloco contiguo na memoria principal
// usado tanto para regiao livre quanto para regiao ocupada
public class BlocoMemoria {

    private int inicio;       // endereco inicial (em MB)
    private int tamanho;      // quantidade em MB
    private Integer idProcessoDono; // null = bloco livre

    public BlocoMemoria(int inicio, int tamanho, Integer idDono) {
        this.inicio = inicio;
        this.tamanho = tamanho;
        this.idProcessoDono = idDono;
    }

    public int getInicio() { return inicio; }
    public int getTamanho() { return tamanho; }
    public int getFim() { return inicio + tamanho - 1; }
    public Integer getIdProcessoDono() { return idProcessoDono; }

    public boolean estaLivre() { return idProcessoDono == null; }

    public void setInicio(int i) { this.inicio = i; }
    public void setTamanho(int t) { this.tamanho = t; }
    public void setIdProcessoDono(Integer id) { this.idProcessoDono = id; }

    @Override
    public String toString() {
        return "[" + inicio + ".." + getFim() + " "
                + (estaLivre() ? "livre" : ("P" + idProcessoDono))
                + " " + tamanho + "MB]";
    }
}
