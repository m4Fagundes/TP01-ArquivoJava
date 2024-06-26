import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import models.*;
import services.*;


public class App {
    public static void main(String[] args) throws Exception {

        Arquivo<Livro> fileTeste = new Arquivo<>(Livro.class.getConstructor());

        Livro livro = new Livro();

        livro.nome = "Clean Code";
        livro.autor = "Robert C. Martins";

        Livro livro2 = new Livro();

        livro2.nome = "Scrum a Arte de Fazer o Dobro em Metade do Tempo";
        livro2.autor = "Jeff Sutherland";

        Livro livro3 = new Livro();

        livro3.nome = "Clean Architecture";
        livro3.autor = "Robert C. Martin";

        Livro livro4 = new Livro();

        livro4.nome = "O Universo em uma Casca de Nós";
        livro4.autor = "Stephen Hawking";

        fileTeste.Create(livro);
        fileTeste.Create(livro2);
        fileTeste.delete(2);
        fileTeste.Create(livro3);
        fileTeste.Create(livro4);

        livro3.nome = "Como Programar Java";
        livro3.autor = "Paul Deitel";

        Livro livroRead = new Livro();
        //fileTeste.Update(livro3);

        livroRead = fileTeste.read(3);

        System.out.println("O nome do livro é : " + livroRead.nome +
        livroRead.autor);
        fileTeste.pesquisaPorNome("Clean Code");

        fileTeste.printHashMapProtected();
        fileTeste.printatNameHashMapProtected();
        fileTeste.pesquisaPorPalavra("o universo em");

        String pastaEntrada = "TP1 - Arquivo/TP01 Arquivo/src/DataBase";
        String pastaDeSaida = "TP1 - Arquivo/TP01 Arquivo/src/Backup";;

        // Cria um formato de data e hora para os backps
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String dataAtual = dateFormat.format(new Date());
        String pastaSaida = pastaDeSaida + "/" + dataAtual;

        LZW.compactarPastaLZW(pastaEntrada, pastaSaida);


        Scanner scanner = new Scanner(System.in);
        String pastaBackup = "TP1 - Arquivo/TP01 Arquivo/src/Backup";

        // Lista as pastas de backup disponíveis
        File diretorioBackup = new File(pastaBackup);
        File[] pastasBackup = diretorioBackup.listFiles(File::isDirectory);

        if (pastasBackup == null || pastasBackup.length == 0) {
            System.out.println("Nenhuma pasta de backup encontrada em " + pastaBackup);
        }

        System.out.println("Pastas de backup disponíveis:");

        for (int i = 0; i < pastasBackup.length; i++) {
            System.out.println(i + ": " + pastasBackup[i].getName());
        }

        System.out.print("Escolha o número da pasta de backup que deseja descompactar: ");
        int escolha = scanner.nextInt();

        if (escolha < 0 || escolha >= pastasBackup.length) {
            System.out.println("Escolha inválida.");
        }

        String pastaEscolhida = pastasBackup[escolha].getAbsolutePath();

        String pastaDescompactadaSaida = "TP1 - Arquivo/TP01 Arquivo/src/BackupDescompactado";

        LZW.descompactarPastaLZW(pastaEscolhida, pastaDescompactadaSaida);

        System.out.println("Pasta de backup descompactada em: " + pastaDescompactadaSaida);

        scanner.close();
    }
}