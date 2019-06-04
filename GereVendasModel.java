import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.out;

public class GereVendasModel implements InterfGereVendasModel {
    public static String CLIENTES;
    public static String PRODUTOS;
    public static String VENDAS;
    public static int FILIAIS;

    private InterfCatProds ctprods;
    private InterfCatClientes catcli;
    private InterfFaturacao fact;
    private List<InterfFilial> filial;

    public GereVendasModel(){
        List<String> files = lerAllLines("configs.txt");
        setCLIENTES(files.get(0));
        setPRODUTOS(files.get(1));
        setVENDAS(files.get(2));
        setFILIAIS(Integer.parseInt(files.get(3)));
        files.clear();

        ctprods = new CatProds();
        catcli = new CatClientes();
        fact = new Faturacao();
        filial = new ArrayList<>(FILIAIS);
        for (int i = 0; i < FILIAIS; i++)
            filial.add(0,new Filial());
    }

    public void createData() {
        List<String> files;
        int i = 0;
        /* TESTE
           out.println(CLIENTES);
           out.println(PRODUTOS);
           out.println(VENDAS);
           out.println(FILIAIS);
        */
        files = lerAllLines(CLIENTES);
        for(String s : files){
            catcli.adiciona(s);
            i++;
        }
        out.println(i); //teste
        i=0;
        files = lerAllLines(PRODUTOS);
        for(String s : files){
            ctprods.adiciona(s);
            i++;
        }
        out.println(i); //teste
        i=0;
        files = lerAllLines(VENDAS);
        for(String s : files) {
            InterfVenda venda = divideVenda(s);
            if(verificaVenda(venda)) {
                this.fact.adiciona(venda);
                InterfFilial f = filial.get(venda.getFilial()-1);
                f.adiciona(venda);
                fact.adiciona(venda);
                i++;
            }
         //   out.println(i);
        }
        out.println(i); //teste
    }

    private boolean verificaVenda(InterfVenda venda) {
        return venda != null &&
                catcli.contains(venda.getCodCli()) &&
                ctprods.contains(venda.getCodPro()) &&
                (venda.getTipo().equals("P") || venda.getTipo().equals("N"));
    }

    private static InterfVenda divideVenda(String linha) {
        String codPro, codCli, tipo;
        int mes = 0, filial = 0, quant = 0;
        double preco = 0;
        String[] campos = linha.split(" ");
        codPro = campos[0];
        tipo = campos[3];
        codCli = campos[4];
        try{
            preco = Double.parseDouble(campos[1]);
            quant = Integer.parseInt(campos[2]);
            mes = Integer.parseInt(campos[5]);
            filial = Integer.parseInt(campos[6]);
        }
        catch (InputMismatchException exc){return null;}

        if(mes < 1 || mes > 12) return null;
        if(filial < 1 || filial > FILIAIS) return null;

        return new Venda(codPro, codCli, tipo, mes, filial, quant, preco);
    }

    public static List<String> lerAllLines(String fichtxt) {
        List<String> linhas = new ArrayList<>();
        try{
            linhas = Files.readAllLines(Paths.get(fichtxt));
        }
        catch (IOException exc) {out.println(exc);}
        return linhas;
    }

    public static void setCLIENTES(String CLIENTES) {
        GereVendasModel.CLIENTES = CLIENTES;
    }

    public static void setPRODUTOS(String PRODUTOS) {
        GereVendasModel.PRODUTOS = PRODUTOS;
    }

    public static void setVENDAS(String VENDAS) {
        GereVendasModel.VENDAS = VENDAS;
    }

    public static void setFILIAIS(int FILIAIS) {
        GereVendasModel.FILIAIS = FILIAIS;
    }

    public Set<String> querie1(){
        return this.fact.getListaOrdenadaProdutosNuncaComprados(ctprods);
    }

    public int[] querie2(int mes) {
        Set<String> clientes = new TreeSet<>();
        int[] total = new int[2];

        for(int i = 0; i < FILIAIS; i++){
            Map<Integer, Set<String>> fil = new HashMap<>(filial.get(i).totalVendasEClientesMes(mes));
            for(Map.Entry<Integer, Set<String>> entry : fil.entrySet()){
                clientes.addAll(entry.getValue());
                total[0] += entry.getKey();
            }
            total[1] = clientes.size();
        }
        return total;
    }

    public int[] querie2(int mes, int fil){
        Set<String> clientes = new TreeSet<>();
        int[] total = new int[2];

        Map<Integer, Set<String>> fili = new HashMap<>(filial.get(fil).totalVendasEClientesMes(mes));
        for(Map.Entry<Integer, Set<String>> entry : fili.entrySet()){
            clientes.addAll(entry.getValue());
            total[0] += entry.getKey();
        }
        total[1] = clientes.size();

        return total;
    }

    public List<Integer> getTotalComprasCliente(String codCliente){
        List<Integer> compras = new ArrayList<>(12);
        int total = 0;
        for(int i=0; i<12; i++){
             for(int fil=0; fil<FILIAIS; fil++){
                 total += this.filial.get(fil).totalCompras(codCliente)[i];
             }
            compras.add(i,total);
        }
        return compras;
    }

    public List<Integer> getTotalProds(String codCliente){
        List<Integer> prods = new ArrayList<>(12);
        int total = 0;
        for(int i=0; i<12; i++){
            for(int fil=0; fil<FILIAIS; fil++){
                total += this.filial.get(fil).totalProds(codCliente)[i];
            }
            prods.add(i,total);
        }
        return prods;
    }

    public List<Double> getTotalGasto(String codCliente){
        List<Double> gasto = new ArrayList<>(12);
        Map<String, int[]> prodsQuantNormal = new HashMap<>();
        for(int fil=0; fil<FILIAIS; fil++){
            Map<String, int[]> aux = this.filial.get(fil).prodsQuantNormal(codCliente);
            if(fil>0) {
                for (Map.Entry<String, int[]> entry : aux.entrySet()) {
                    if (prodsQuantNormal.containsKey(entry.getKey())) {
                        for (int i = 0; i < 12; i++) {
                            prodsQuantNormal.get(entry.getKey())[i] += entry.getValue()[i];
                        }
                    }else{
                        prodsQuantNormal.put(entry.getKey(),entry.getValue());
                    }
                }
            }else{
                prodsQuantNormal.putAll(aux);
            }
        }
        Map<String, int[]> prodsQuantPromo = new HashMap<>();
        for(int fil=0; fil<FILIAIS; fil++){
            Map<String, int[]> aux = this.filial.get(fil).prodsQuantPromo(codCliente);
            if(fil>0) {
                for (Map.Entry<String, int[]> entry : aux.entrySet()) {
                    if (prodsQuantPromo.containsKey(entry.getKey())) {
                        for (int i = 0; i < 12; i++) {
                            prodsQuantPromo.get(entry.getKey())[i] += entry.getValue()[i];
                        }
                    }else{
                        prodsQuantPromo.put(entry.getKey(),entry.getValue());
                    }
                }
            }else{
                prodsQuantPromo.putAll(aux);
            }
        }
        gasto = this.fact.totalfaturado(prodsQuantNormal, prodsQuantPromo);
        return gasto;
    }

    @Override
    public boolean existeCodCliente(String codCli) {
        return catcli.contains(codCli);
    }

    @Override
    public boolean existeCodProd(String codProd) {
        return ctprods.contains(codProd);
    }

    @Override
    public int getFILIAIS() {
        return FILIAIS;
    }
}