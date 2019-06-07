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
            filial.add(i,new Filial());
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
        // out.println(i); //teste
        i=0;
        files = lerAllLines(PRODUTOS);
        for(String s : files){
            ctprods.adiciona(s);
            i++;
        }
        //out.println(i); //teste
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
        catch (InputMismatchException exc){
            if(codPro.equals("JU1146"))
                out.println("Venda Inválida");
            return null;}

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

        Map<Integer, Set<String>> fili = new HashMap<>(this.filial.get(fil).totalVendasEClientesMes(mes));
        for(Map.Entry<Integer, Set<String>> entry : fili.entrySet()){
            clientes.addAll(entry.getValue());
            total[0] += entry.getKey();
        }
        total[1] = clientes.size();

        return total;
    }

    public List<Integer> Querie3TotalComprasCliente(String codCliente){
        List<Integer> compras = new ArrayList<>(12);
        int total;
        for(int mes=0; mes<12; mes++){
            total = 0;
            for(InterfFilial fil:filial){
                total += fil.totalCompras(codCliente, mes);
            }
            compras.add(mes,total);
        }
        return compras;
    }

    public List<Integer> Querie3TotalProds(String codCliente){
        List<Integer> prods = new ArrayList<>(12);
        Set<String> aux = new TreeSet<>();
        for(int mes = 0; mes < 12; mes++){
            aux.clear();
            for(InterfFilial fil: filial){
                aux.addAll(fil.getProdutos(codCliente, mes));
            }
            prods.add(mes,aux.size());
        }
        return prods;
    }

    public List<Double> Querie3TotalGasto(String codCliente){
        List<Double> gasto = new ArrayList<>(12);
        List<Map<String,int[]>> prodsQuant = new ArrayList<>(12);
        for(int i=0; i<12; i++){
            prodsQuant.add(i,new HashMap<>());
        }
        for(int i=0; i<12; i++) {
            for (InterfFilial fil: filial){
                if(prodsQuant.get(i) == null) {
                    prodsQuant.add(i,fil.prodsQuant(codCliente, i));
                }else{
                    Map<String,int[]> aux = fil.prodsQuant(codCliente, i);
                    for(Map.Entry<String, int[]> entry: aux.entrySet()){
                        if(prodsQuant.get(i).get(entry.getKey()) != null) {
                            entry.getValue()[0] += prodsQuant.get(i).get(entry.getKey())[0];
                            entry.getValue()[1] += prodsQuant.get(i).get(entry.getKey())[1];
                        }
                    }
                    prodsQuant.set(i,aux);
                }
            }
        }
        gasto = this.fact.totalfaturado(prodsQuant);
        return gasto;
    }

    public List<Integer> querie4getQuantidade(String prod) {
        List<Integer> meses = new ArrayList<>(12);
        int total;
        for(int mes = 0; mes < 12; mes++) {
            total = 0;
            for(InterfFilial fil : filial){
                total += fil.getQuantidadeTotalProduto(prod, mes);
            }
            meses.add(mes,total);
        }
        return meses;
    }

    public List<Integer> querie4getClientes(String prod) { //acho que funciona
        List<Integer> meses = new ArrayList<>(12);
        int cliNum = 0;
        for(int mes = 0; mes < 12; mes++){
            cliNum = 0;
            for (InterfFilial f : filial) {
                cliNum += f.getClientes(prod, mes).size();
            }
            meses.add(mes, cliNum);
        }

        return meses;
    }

    public List<Double> querie4getTotalFaturado(String prod) {
        List<Double> meses = new ArrayList<>(12);
        double total = 0;
        for(int mes = 0; mes < 12; mes++){
            total = 0;
            for(InterfFilial fil : this.filial){
                int[] quantidades = fil.getQuantidadePorTipoProduto(prod, mes);
                total += fact.getTotalFaturado(prod, quantidades,mes);
            }
            meses.add(mes, total);
        }
        return meses;
    }

    public Map<Integer, Set<String>> querie5(String cli){
        Map<Integer, Set<String>> prods = new TreeMap<>(Comparator.reverseOrder());
        Map<Integer, Set<String>> aux = new TreeMap<>();

        for(InterfFilial f : this.filial){
            aux.putAll(f.getProdutosEQuantidades(prods, cli));
            prods.clear();
            prods.putAll(aux);
            aux.clear();
        }
        return prods;
    }

    public int getI(){
        int i=0;
        for(InterfFilial fil: filial){
            i += fil.getI();
        }
        return i;
    }

    public boolean existeCodCliente(String codCli) {
        return catcli.contains(codCli);
    }

    public boolean existeCodProd(String codProd) {
        return ctprods.contains(codProd);
    }

    public int getFILIAIS() {
        return this.FILIAIS;
    }

    public InterfFaturacao getFact(){
        return this.fact;
    }

    @Override
    public List<InterfFilial> getFil() {
        return this.filial;
    }

    public boolean isEmpty(){
        boolean flag = false;

        for(InterfFilial f : filial){
            if(f.isEmpty())
                flag = true;
        }

        return this.catcli.isEmpty() && this.ctprods.isEmpty()
                && this.fact.isEmpty() && flag;
    }

    @Override
    public Set<String> querie6PodsMaisComprados(int x) {
        TreeMap<Integer,Set<String>> prods = new TreeMap<>(Collections.reverseOrder());
        Set<String> setProds = new HashSet<>();

        for(InterfFilial fil: this.filial) {
            prods = fil.getProdMaisComprado(prods);
        }

        for(int i=0; i<x; i++){
            setProds.addAll(prods.pollFirstEntry().getValue());
        }

        return setProds;
    }

    @Override
    public Map<String, Integer> querie6Clientes(Set<String> prods) {
        Map<String, Integer> prodsEClientes = new TreeMap<>();

        for(String codProd: prods){
            Set<String> clientes = new TreeSet<>();
            for(InterfFilial fil: this.filial){
                for(int mes=0; mes<12; mes++){
                    clientes.addAll(fil.getClientes(codProd,mes));
                }
            }
            prodsEClientes.put(codProd,clientes.size());
        }
        return prodsEClientes;
    }

    @Override
    public Map<Integer, Set<String>> querie8() {
        Map<Integer,Set<String>> clientes = new TreeMap<>(Collections.reverseOrder());
        Map<String,Set<String>> cliProds = new HashMap<>();

        for(InterfFilial fil : filial){
            cliProds = fil.clientesMaisProds(cliProds);
        }
        for(Map.Entry<String, Set<String>> entry : cliProds.entrySet()){
            if(clientes.containsKey(entry.getValue().size())){
                Set<String> set = clientes.get(entry.getValue().size());
                set.add(entry.getKey());
                clientes.put(entry.getValue().size(),set);
            }else{
                Set<String> set = new TreeSet<>();
                set.add(entry.getKey());
                clientes.put(entry.getValue().size(),set);
            }
        }
        return clientes;
    }

    @Override
    public Map<Integer, Map<String, Double>> querie9(String codProd) {
        double[] precoN = new double[12];
        double[] precoP = new double[12];
        precoN = this.fact.getPrecoNormalProd(codProd);
        precoP = this.fact.getPrecoPromoProd(codProd);


        Map<Integer,Map<String,Double>> aux = new TreeMap<>();
        Map<Integer,Map<String,Double>> clis = new TreeMap<>(Collections.reverseOrder());

        if(precoN == null && precoP == null){
            return clis;
        }

        for(InterfFilial fil: filial){
            aux.putAll(fil.clisProdQ9(codProd,clis,precoN,precoP));
            clis.clear();
            clis.putAll(aux);
            aux.clear();
        }
        return clis;
    }
}