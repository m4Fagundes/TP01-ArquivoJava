package services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import models.*;

/**
 * Classe genérica para manipulação de registros de arquivo que implementam a
 * interface Registro.
 * Utiliza serialização e deserialização de objetos do tipo T em um array de
 * bytes para armazenamento em arquivo.
 * 
 * @param <T> Tipo do registro que estende a interface Registro.
 */
public class Arquivo<T extends Registro> {

  protected RandomAccessFile arquivo; // Objeto para leitura e escrita no arquivo.
  protected Constructor<T> construtor; // Construtor do tipo T, usado para criar instâncias de T.
  final protected int TAM_CABECALHO = 4; // Tamanho fixo do cabeçalho do arquivo.
  HashMap<Integer, Long> index = new HashMap<>(); // Tabela hash de indexação para pesquisa


  /**
   * Salva o índice em um arquivo usando serialização.
   */
  public void salvarIndice() {
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("indice.db"))) {
      out.writeObject(index);
    } catch (IOException e) {
      System.out.println("Erro ao salvar índice: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Carrega o índice de um arquivo usando serialização.
   */
  @SuppressWarnings("unchecked")
  public void carregarIndice() {
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("indice.db"))) {
      index = (HashMap<Integer, Long>) in.readObject();
    } catch (FileNotFoundException e) {
      System.out.println("Arquivo de índice não encontrado. Um novo índice será criado.");
      index = new HashMap<>();
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Erro ao carregar índice: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // Sobrescrever o construtor para carregar o índice ao inicializar
  public Arquivo(Constructor<T> c) throws Exception {
    this.construtor = c;
    arquivo = new RandomAccessFile("pessoas.db", "rw");
    carregarIndice(); // Carrega o índice na inicialização
    if (arquivo.length() < TAM_CABECALHO) {
      arquivo.seek(0);
      arquivo.writeInt(0); // Escreve 0 no cabeçalho se o arquivo está vazio.
    }
  }

  // Sobrescrever o método close para garantir que o índice seja salvo
  public void close() {
    try {
      if (arquivo != null) {
        arquivo.close();
      }
      salvarIndice(); // Salva o índice antes de fechar
    } catch (IOException e) {
      System.out.println("Erro ao fechar o arquivo: " + e.getMessage());
    }
  }
  

  /**
   * Cria um novo registro no arquivo, utilizando índices para otimizar a busca.
   * Salva o novo ID no objeto e no arquivo, e escreve o objeto serializado,
   * reutilizando espaço de registros deletados se possível.
   * 
   * @param obj Instância de T para ser armazenada no arquivo.
   */
  public void Create(T obj) throws Exception {
    arquivo.seek(0);
    int ultimoID = arquivo.readInt();
    ultimoID++;
    arquivo.seek(0);
    arquivo.writeInt(ultimoID);
    obj.setID(ultimoID);

    byte[] registroOBJ = obj.toByteArray();
    short tamanhoRegistroNovo = (short) registroOBJ.length;
    boolean espaçoEncontrado = false;
    Long offset = null;

    arquivo.seek(TAM_CABECALHO);

    while (arquivo.getFilePointer() < arquivo.length()) {
      long posiçãoLapide = arquivo.getFilePointer();
      byte valorLapide = arquivo.readByte();
      short tamanhoRegistroAtual = arquivo.readShort();

      if (valorLapide == '*' && tamanhoRegistroAtual >= tamanhoRegistroNovo) {
        arquivo.seek(posiçãoLapide);
        arquivo.writeByte(' ');
        arquivo.writeShort(tamanhoRegistroNovo);
        arquivo.write(registroOBJ);
        espaçoEncontrado = true;
        offset = posiçãoLapide;
        break;
      } else {
        arquivo.skipBytes(tamanhoRegistroAtual);
      }
    }

    if (!espaçoEncontrado) {
      arquivo.seek(arquivo.length());
      arquivo.writeByte(' ');
      arquivo.writeShort(tamanhoRegistroNovo);
      arquivo.write(registroOBJ);
      offset = arquivo.getFilePointer() - (1 + 2 + tamanhoRegistroNovo);
    }

    index.put(ultimoID, offset);
  }

  /**
   * Apaga um registro pelo ID, marcando seu espaço como deletado (lapide '*')
   * e remove o ID do índice.
   *
   * @param id ID do registro a ser deletado.
   */
  public void delete(int id) {
    try {
      Long offset = index.get(id);
      if (offset == null) {
        System.out.println("Registro com ID " + id + " não encontrado.");
        return;
      }
      arquivo.seek(offset);
      arquivo.writeByte('*');
      index.remove(id);
    } catch (IOException e) {
      System.out.println("Erro de I/O ao deletar registro: " + e.getMessage());
    }
  }

  /**
   * Atualiza um registro no arquivo substituindo o antigo por um novo
   * se o espaço for suficiente, ou movendo para o final do arquivo se necessário.
   * Atualiza o índice para refletir a mudança.
   *
   * @param obj Registro a ser atualizado.
   */
  public void Update(T obj) throws Exception {
    Long enderecoOBJ = index.get(obj.getID());
    if (enderecoOBJ == null) {
      System.out.println("Objeto não encontrado para atualização.");
      return;
    }

    arquivo.seek(enderecoOBJ);
    byte lapide = arquivo.readByte();
    short tamanhoAtual = arquivo.readShort();
    byte[] novoRegistro = obj.toByteArray();
    short novoTamanho = (short) novoRegistro.length;

    if (tamanhoAtual >= novoTamanho) {
      arquivo.seek(enderecoOBJ);
      arquivo.writeByte(' ');
      arquivo.writeShort(novoTamanho);
      arquivo.write(novoRegistro);
    } else {
      arquivo.seek(enderecoOBJ);
      arquivo.writeByte('*');
      index.remove(obj.getID());
      arquivo.seek(arquivo.length());
      long novoEndereco = arquivo.getFilePointer();
      arquivo.writeByte(' ');
      arquivo.writeShort(novoTamanho);
      arquivo.write(novoRegistro);
      index.put(obj.getID(), novoEndereco);
    }
  }

  /**
   * Lê e deserializa um registro pelo ID usando índices.
   * Retorna nulo se o registro não for encontrado ou estiver marcado como
   * deletado.
   * 
   * @param id ID do registro a ser lido.
   * @return Uma instância de T se encontrado, nulo caso contrário.
   */
  public T read(int id) throws Exception {
    Long enderecoLeitura = index.get(id);
    if (enderecoLeitura == null) {
      System.out.println("Objeto não encontrado para leitura.");
      return null;
    }

    arquivo.seek(enderecoLeitura);
    byte lapide = arquivo.readByte();
    if (lapide == ' ') {
      short tamanhoRegistro = arquivo.readShort();
      byte[] registro = new byte[tamanhoRegistro];
      arquivo.readFully(registro);
      T obj = construtor.newInstance();
      obj.fromByteArray(registro);
      return obj;
    } else {
      System.out.println("O registro não está presente no acervo.");
      return null;
    }
  }
}